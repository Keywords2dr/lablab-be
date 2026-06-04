package com.keywords2dr.lablab.service.impl;

import com.keywords2dr.lablab.dto.ticket.*;
import com.keywords2dr.lablab.entity.*;
import com.keywords2dr.lablab.entity.enums.*;
import com.keywords2dr.lablab.exception.BadRequestException;
import com.keywords2dr.lablab.exception.ResourceNotFoundException;
import com.keywords2dr.lablab.mapper.RentTicketMapper;
import com.keywords2dr.lablab.repository.*;
import com.keywords2dr.lablab.service.AuditLogService;
import com.keywords2dr.lablab.service.StockAlertService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  RentTicketServiceImplTest  —  53 test cases / 8 nhóm          │
 * │                                                                 │
 * │  Nhóm 1 · createTicket        · 11 cases                       │
 * │  Nhóm 2 · cancelTicket        ·  7 cases                       │
 * │  Nhóm 3 · getTicketById       ·  2 cases                       │
 * │  Nhóm 4 · teacherApprove      ·  8 cases                       │
 * │  Nhóm 5 · activateTicket      ·  3 cases                       │
 * │  Nhóm 6 · requestReturn       ·  9 cases                       │
 * │  Nhóm 7 · teacherConfirmReturn·  6 cases                       │
 * │  Nhóm 8 · adminApprove        ·  7 cases                       │
 * │                                                                 │
 * │  Chạy: mvn test -Dtest=RentTicketServiceImplTest               │
 * └─────────────────────────────────────────────────────────────────┘
 */
@ExtendWith(MockitoExtension.class)
class RentTicketServiceImplTest {

    // ── Mocks — khớp chính xác @RequiredArgsConstructor của service ───────────
    @Mock RentTicketRepository       rentTicketRepository;
    @Mock RentTicketDetailRepository rentTicketDetailRepository;
    @Mock RoomRepository             roomRepository;
    @Mock UserRepository             userRepository;
    @Mock ItemRepository             itemRepository;
    @Mock RoomInventoryRepository    roomInventoryRepository;
    @Mock RentTicketMapper           rentTicketMapper;
    @Mock AuditLogService            auditLogService;
    @Mock ApplicationEventPublisher  eventPublisher;
    @Mock StockAlertService          stockAlertService;

    RentTicketServiceImpl service;

    // ── Fixtures ───────────────────────────────────────────────────────────────
    UUID userId, ticketId, roomId, teacherId, adminId, itemId;
    User requester, teacher, admin;
    Room room;
    RentTicket ticket;
    RentTicketResponse        ticketResp;
    RentTicketSummaryResponse summaryResp;

    @BeforeEach
    void setUp() {
        userId    = UUID.randomUUID();
        ticketId  = UUID.randomUUID();
        roomId    = UUID.randomUUID();
        teacherId = UUID.randomUUID();
        adminId   = UUID.randomUUID();
        itemId    = UUID.randomUUID();

        requester = mockUser(userId,    "student01");
        teacher   = mockUser(teacherId, "teacher01");
        admin     = mockUser(adminId,   "admin01");

        RoomStaffAssignment sa = new RoomStaffAssignment();
        sa.setUser(teacher);

        room = new Room();
        room.setRoomId(roomId);
        room.setRoomName("Lab A");
        room.setIsActive(true);
        room.setStaffAssignments(new ArrayList<>(List.of(sa)));

        ticket = RentTicket.builder()
                .ticketId(ticketId)
                .requester(requester)
                .fromRoom(room)
                .ticketType(TicketType.ROOM_ONLY)
                .status(TicketStatus.PENDING_OWNER)
                .borrowDate(LocalDateTime.now().plusHours(1))
                .expectedReturnDate(LocalDateTime.now().plusHours(3))
                .createdAt(LocalDateTime.now())
                .ticketDetails(new HashSet<>())
                .build();

        ticketResp  = new RentTicketResponse();
        ticketResp.setTicketId(ticketId);
        summaryResp = new RentTicketSummaryResponse();
        summaryResp.setTicketId(ticketId);

        service = new RentTicketServiceImpl(
                rentTicketRepository,
                rentTicketDetailRepository,
                roomRepository,
                userRepository,
                itemRepository,
                roomInventoryRepository,
                rentTicketMapper,
                auditLogService,
                eventPublisher,
                stockAlertService
        );
    }

    // =========================================================================
    // Nhóm 1 · createTicket — 11 cases
    // =========================================================================
    @Nested
    @DisplayName("Nhóm 1 · createTicket()")
    class CreateTicket {

        @Test
        @DisplayName("TC01 · ROOM_ONLY — thành công, không lưu detail, check conflict phòng")
        void tc01_roomOnly_success() {
            stubUserAndRoom();
            when(rentTicketRepository.existsConflictingBooking(eq(roomId), any(), any())).thenReturn(false);
            when(rentTicketRepository.save(any())).thenReturn(ticket);
            when(rentTicketMapper.toResponse(any())).thenReturn(ticketResp);

            RentTicketResponse result = service.createTicket(userId, createReq(TicketType.ROOM_ONLY, null));

            assertThat(result.getTicketId()).isEqualTo(ticketId);
            verify(rentTicketRepository).save(any(RentTicket.class));
            verify(rentTicketDetailRepository, never()).saveAll(any());
            verify(rentTicketRepository).existsConflictingBooking(eq(roomId), any(), any());
        }

        @Test
        @DisplayName("TC02 · CHEMICAL_ONLY — thành công, lưu detail, KHÔNG check conflict phòng")
        void tc02_chemicalOnly_success() {
            stubUserAndRoom();
            when(rentTicketRepository.save(any())).thenReturn(ticket);
            when(itemRepository.findAllById(any())).thenReturn(List.of(activeItem()));
            when(roomInventoryRepository.findAllByRoom_RoomIdAndItem_ItemIdIn(any(), any()))
                    .thenReturn(List.of(inventory()));
            when(rentTicketMapper.toResponse(any())).thenReturn(ticketResp);

            service.createTicket(userId, createReq(TicketType.CHEMICAL_ONLY, List.of(detailReq())));

            verify(rentTicketRepository, never()).existsConflictingBooking(any(), any(), any());
            verify(rentTicketDetailRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("TC03 · Phòng không có staff — notify ADMIN thay vì notify staff")
        void tc03_noStaff_notifyAdmins() {
            room.setStaffAssignments(Collections.emptyList());
            stubUserAndRoom();
            when(rentTicketRepository.existsConflictingBooking(any(), any(), any())).thenReturn(false);
            when(rentTicketRepository.save(any())).thenReturn(ticket);
            when(rentTicketMapper.toResponse(any())).thenReturn(ticketResp);
            when(userRepository.findAllByRole("ADMIN")).thenReturn(List.of(admin));

            service.createTicket(userId, createReq(TicketType.ROOM_ONLY, null));

            verify(userRepository).findAllByRole("ADMIN");
        }

        @Test
        @DisplayName("TC04 · Người dùng không tồn tại — ResourceNotFoundException")
        void tc04_userNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createTicket(userId, createReq(TicketType.ROOM_ONLY, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("TC05 · Phòng không tồn tại — ResourceNotFoundException")
        void tc05_roomNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
            when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createTicket(userId, createReq(TicketType.ROOM_ONLY, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("TC06 · Phòng bị khóa (isActive=false) — BadRequestException chứa 'khóa'")
        void tc06_roomInactive() {
            room.setIsActive(false);
            when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

            assertThatThrownBy(() -> service.createTicket(userId, createReq(TicketType.ROOM_ONLY, null)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("khóa");
        }

        @Test
        @DisplayName("TC07 · borrowDate >= expectedReturnDate — BadRequestException chứa 'thời gian'")
        void tc07_invalidDateRange() {
            RentTicketCreateRequest req = createReq(TicketType.ROOM_ONLY, null);
            req.setBorrowDate(LocalDateTime.now().plusHours(5));
            req.setExpectedReturnDate(LocalDateTime.now().plusHours(2));
            when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

            assertThatThrownBy(() -> service.createTicket(userId, req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("thời gian");
        }

        @Test
        @DisplayName("TC08 · ticketType không hợp lệ — BadRequestException chứa 'không hợp lệ'")
        void tc08_invalidTicketType() {
            RentTicketCreateRequest req = createReq(TicketType.ROOM_ONLY, null);
            req.setTicketType("GARBAGE_TYPE");
            when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

            assertThatThrownBy(() -> service.createTicket(userId, req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("không hợp lệ");
        }

        @Test
        @DisplayName("TC09 · CHEMICAL_ONLY items rỗng — BadRequestException chứa 'hóa chất'")
        void tc09_chemicalOnly_emptyItems() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

            assertThatThrownBy(() -> service.createTicket(userId,
                    createReq(TicketType.CHEMICAL_ONLY, Collections.emptyList())))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("hóa chất");
        }

        @Test
        @DisplayName("TC10 · ROOM_ONLY trùng lịch phòng — BadRequestException, không gọi save")
        void tc10_conflictingBooking() {
            stubUserAndRoom();
            when(rentTicketRepository.existsConflictingBooking(eq(roomId), any(), any())).thenReturn(true);

            assertThatThrownBy(() -> service.createTicket(userId, createReq(TicketType.ROOM_ONLY, null)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("lịch mượn");
            verify(rentTicketRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC11 · Item đã bị xóa — BadRequestException chứa 'đã bị xóa'")
        void tc11_itemDeleted() {
            stubUserAndRoom();
            when(rentTicketRepository.save(any())).thenReturn(ticket);
            Item deleted = activeItem();
            deleted.setDeleted(true);
            when(itemRepository.findAllById(any())).thenReturn(List.of(deleted));

            assertThatThrownBy(() -> service.createTicket(userId,
                    createReq(TicketType.CHEMICAL_ONLY, List.of(detailReq()))))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("đã bị xóa");
        }
    }

    // =========================================================================
    // Nhóm 2 · cancelTicket — 7 cases
    // =========================================================================
    @Nested
    @DisplayName("Nhóm 2 · cancelTicket()")
    class CancelTicket {

        @Test
        @DisplayName("TC12 · PENDING_OWNER — hủy thành công → CANCELLED")
        void tc12_pendingOwner_success() {
            stubFindTicket();
            when(rentTicketMapper.toResponse(any())).thenReturn(ticketResp);

            service.cancelTicket(ticketId, userId);

            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
            verify(rentTicketRepository).save(ticket);
        }

        @Test
        @DisplayName("TC13 · PENDING_ADMIN — hủy thành công → CANCELLED")
        void tc13_pendingAdmin_success() {
            ticket.setStatus(TicketStatus.PENDING_ADMIN);
            stubFindTicket();
            when(rentTicketMapper.toResponse(any())).thenReturn(ticketResp);

            service.cancelTicket(ticketId, userId);

            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        }

        @Test
        @DisplayName("TC14 · Phiếu không tồn tại — ResourceNotFoundException")
        void tc14_ticketNotFound() {
            when(rentTicketRepository.findById(ticketId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cancelTicket(ticketId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("TC15 · Người dùng khác hủy — BadRequestException chứa 'quyền'")
        void tc15_wrongUser() {
            stubFindTicket();

            assertThatThrownBy(() -> service.cancelTicket(ticketId, UUID.randomUUID()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("quyền");
        }

        @Test
        @DisplayName("TC16 · Phiếu đã APPROVED — BadRequestException chứa 'chờ duyệt'")
        void tc16_approvedStatus() {
            ticket.setStatus(TicketStatus.APPROVED);
            stubFindTicket();

            assertThatThrownBy(() -> service.cancelTicket(ticketId, userId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("chờ duyệt");
        }

        @Test
        @DisplayName("TC17 · Phiếu BORROWED — BadRequestException (không thể hủy khi đang mượn)")
        void tc17_borrowedStatus() {
            ticket.setStatus(TicketStatus.BORROWED);
            stubFindTicket();

            assertThatThrownBy(() -> service.cancelTicket(ticketId, userId))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("TC18 · Phiếu RETURNED — BadRequestException (không thể hủy sau khi đã trả)")
        void tc18_returnedStatus() {
            ticket.setStatus(TicketStatus.RETURNED);
            stubFindTicket();

            assertThatThrownBy(() -> service.cancelTicket(ticketId, userId))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    // =========================================================================
    // Nhóm 3 · getTicketById — 2 cases
    // =========================================================================
    @Nested
    @DisplayName("Nhóm 3 · getTicketById()")
    class GetTicketById {

        @Test
        @DisplayName("TC19 · Tìm thấy — trả về response đúng")
        void tc19_found() {
            when(rentTicketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(rentTicketMapper.toResponse(ticket)).thenReturn(ticketResp);

            assertThat(service.getTicketById(ticketId).getTicketId()).isEqualTo(ticketId);
        }

        @Test
        @DisplayName("TC20 · Không tìm thấy — ResourceNotFoundException")
        void tc20_notFound() {
            when(rentTicketRepository.findById(ticketId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getTicketById(ticketId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // Nhóm 4 · teacherApprove — 8 cases
    // =========================================================================
    @Nested
    @DisplayName("Nhóm 4 · teacherApprove()")
    class TeacherApprove {

        @Test
        @DisplayName("TC21 · ROOM_ONLY — duyệt → APPROVED trực tiếp, ghi ownerApprovedBy + At")
        void tc21_roomOnly_approved() {
            stubFindTicket();
            when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
            when(rentTicketMapper.toResponse(any())).thenReturn(ticketResp);

            service.teacherApprove(ticketId, teacherId, approveReq(true, null));

            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.APPROVED);
            assertThat(ticket.getOwnerApprovedBy()).isEqualTo(teacher);
            assertThat(ticket.getOwnerApprovedAt()).isNotNull();
        }

        @Test
        @DisplayName("TC22 · CHEMICAL_ONLY — duyệt → PENDING_ADMIN, notify admin")
        void tc22_chemicalOnly_pendingAdmin() {
            ticket.setTicketType(TicketType.CHEMICAL_ONLY);
            stubFindTicket();
            when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
            when(userRepository.findAllByRole("ADMIN")).thenReturn(List.of(admin));
            when(rentTicketMapper.toResponse(any())).thenReturn(ticketResp);

            service.teacherApprove(ticketId, teacherId, approveReq(true, null));

            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.PENDING_ADMIN);
            verify(userRepository).findAllByRole("ADMIN");
        }

        @Test
        @DisplayName("TC23 · Từ chối có lý do — REJECTED, lưu rejectedBy=teacher, reason, rejectedAt")
        void tc23_reject_withReason() {
            stubFindTicket();
            when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
            when(rentTicketMapper.toResponse(any())).thenReturn(ticketResp);

            service.teacherApprove(ticketId, teacherId, approveReq(false, "Phòng đang sửa chữa"));

            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.REJECTED);
            assertThat(ticket.getRejectedBy()).isEqualTo(teacher);
            assertThat(ticket.getRejectedReason()).isEqualTo("Phòng đang sửa chữa");
            assertThat(ticket.getRejectedAt()).isNotNull();
        }

        @Test
        @DisplayName("TC24 · Từ chối không có lý do (null) — BadRequestException chứa 'lý do'")
        void tc24_reject_nullReason() {
            stubFindTicket();

            assertThatThrownBy(() -> service.teacherApprove(ticketId, teacherId, approveReq(false, null)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("lý do");
        }

        @Test
        @DisplayName("TC25 · Từ chối lý do blank/trắng — BadRequestException")
        void tc25_reject_blankReason() {
            stubFindTicket();

            assertThatThrownBy(() -> service.teacherApprove(ticketId, teacherId, approveReq(false, "   ")))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("TC26 · Phiếu không ở PENDING_OWNER — BadRequestException chứa 'PENDING_OWNER'")
        void tc26_wrongStatus() {
            ticket.setStatus(TicketStatus.APPROVED);
            stubFindTicket();

            assertThatThrownBy(() -> service.teacherApprove(ticketId, teacherId, approveReq(true, null)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("PENDING_OWNER");
        }

        @Test
        @DisplayName("TC27 · Teacher không phụ trách phòng — BadRequestException chứa 'quyền'")
        void tc27_notRoomOwner() {
            stubFindTicket();

            assertThatThrownBy(() -> service.teacherApprove(ticketId, UUID.randomUUID(), approveReq(true, null)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("quyền");
        }

        @Test
        @DisplayName("TC28 · Phòng không có staff, teacher bất kỳ duyệt — BadRequestException")
        void tc28_roomNoStaff_anyTeacherBlocked() {
            room.setStaffAssignments(Collections.emptyList());
            stubFindTicket();

            assertThatThrownBy(() -> service.teacherApprove(ticketId, teacherId, approveReq(true, null)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("quyền");
        }
    }

    // =========================================================================
    // Nhóm 5 · activateTicket — 3 cases
    // =========================================================================
    @Nested
    @DisplayName("Nhóm 5 · activateTicket()")
    class ActivateTicket {

        @Test
        @DisplayName("TC29 · APPROVED → BORROWED thành công")
        void tc29_approved_toBorrowed() {
            ticket.setStatus(TicketStatus.APPROVED);
            stubFindTicket();
            when(rentTicketMapper.toResponse(any())).thenReturn(ticketResp);

            service.activateTicket(ticketId, teacherId);

            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.BORROWED);
        }

        @Test
        @DisplayName("TC30 · Phiếu không ở APPROVED — BadRequestException chứa 'APPROVED'")
        void tc30_wrongStatus() {
            ticket.setStatus(TicketStatus.PENDING_OWNER);
            stubFindTicket();

            assertThatThrownBy(() -> service.activateTicket(ticketId, teacherId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("APPROVED");
        }

        @Test
        @DisplayName("TC31 · Teacher không phụ trách phòng — BadRequestException chứa 'quyền'")
        void tc31_notRoomOwner() {
            ticket.setStatus(TicketStatus.APPROVED);
            stubFindTicket();

            assertThatThrownBy(() -> service.activateTicket(ticketId, UUID.randomUUID()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("quyền");
        }
    }

    // =========================================================================
    // Nhóm 6 · requestReturn — 9 cases
    // =========================================================================
    @Nested
    @DisplayName("Nhóm 6 · requestReturn()")
    class RequestReturn {

        @Test
        @DisplayName("TC32 · ROOM_ONLY BORROWED — trả thành công → PENDING_RETURN")
        void tc32_roomOnly_success() {
            ticket.setStatus(TicketStatus.BORROWED);
            stubFindTicket();
            when(rentTicketMapper.toResponse(any())).thenReturn(ticketResp);

            service.requestReturn(ticketId, userId, returnReq(null));

            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.PENDING_RETURN);
        }

        @Test
        @DisplayName("TC33 · CHEMICAL_ONLY — đủ tất cả detail, RETURNED → PENDING_RETURN")
        void tc33_chemicalOnly_allDetails_success() {
            UUID dId = UUID.randomUUID();
            RentTicketDetail detail = buildDetail(dId);
            ticket.setStatus(TicketStatus.BORROWED);
            ticket.setTicketType(TicketType.CHEMICAL_ONLY);
            ticket.setTicketDetails(new HashSet<>(Set.of(detail)));
            stubFindTicket();
            when(rentTicketDetailRepository.findAllById(any())).thenReturn(List.of(detail));
            when(rentTicketMapper.toResponse(any())).thenReturn(ticketResp);

            service.requestReturn(ticketId, userId,
                    returnReq(List.of(returnDetailReq(dId, "RETURNED", null))));

            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.PENDING_RETURN);
        }

        @Test
        @DisplayName("TC34 · Người dùng khác yêu cầu trả — BadRequestException chứa 'quyền'")
        void tc34_wrongUser() {
            ticket.setStatus(TicketStatus.BORROWED);
            stubFindTicket();

            assertThatThrownBy(() -> service.requestReturn(ticketId, UUID.randomUUID(), returnReq(null)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("quyền");
        }

        @Test
        @DisplayName("TC35 · Phiếu không BORROWED — BadRequestException chứa 'BORROWED'")
        void tc35_notBorrowed() {
            ticket.setStatus(TicketStatus.APPROVED);
            stubFindTicket();

            assertThatThrownBy(() -> service.requestReturn(ticketId, userId, returnReq(null)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("BORROWED");
        }

        @Test
        @DisplayName("TC36 · CHEMICAL_ONLY — items null — BadRequestException")
        void tc36_chemicalOnly_nullItems() {
            ticket.setStatus(TicketStatus.BORROWED);
            ticket.setTicketType(TicketType.CHEMICAL_ONLY);
            ticket.setTicketDetails(new HashSet<>(Set.of(buildDetail(UUID.randomUUID()))));
            stubFindTicket();

            assertThatThrownBy(() -> service.requestReturn(ticketId, userId, returnReq(null)))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("TC37 · CHEMICAL_ONLY — thiếu detail của 1 item — BadRequestException chứa 'Thiếu'")
        void tc37_chemicalOnly_missingDetail() {
            UUID d1 = UUID.randomUUID(), d2 = UUID.randomUUID();
            ticket.setStatus(TicketStatus.BORROWED);
            ticket.setTicketType(TicketType.CHEMICAL_ONLY);
            ticket.setTicketDetails(new HashSet<>(Set.of(buildDetail(d1), buildDetail(d2))));
            stubFindTicket();

            assertThatThrownBy(() -> service.requestReturn(ticketId, userId,
                    returnReq(List.of(returnDetailReq(d1, "RETURNED", null)))))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Thiếu");
        }

        @Test
        @DisplayName("TC38 · returnStatus DAMAGED không có note — BadRequestException chứa 'ghi chú'")
        void tc38_damaged_noNote() {
            UUID dId = UUID.randomUUID();
            RentTicketDetail detail = buildDetail(dId);
            ticket.setStatus(TicketStatus.BORROWED);
            ticket.setTicketType(TicketType.CHEMICAL_ONLY);
            ticket.setTicketDetails(new HashSet<>(Set.of(detail)));
            stubFindTicket();
            when(rentTicketDetailRepository.findAllById(any())).thenReturn(List.of(detail));

            assertThatThrownBy(() -> service.requestReturn(ticketId, userId,
                    returnReq(List.of(returnDetailReq(dId, "DAMAGED", null)))))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("ghi chú");
        }

        @Test
        @DisplayName("TC39 · returnStatus LOST — note blank — BadRequestException")
        void tc39_lost_blankNote() {
            UUID dId = UUID.randomUUID();
            RentTicketDetail detail = buildDetail(dId);
            ticket.setStatus(TicketStatus.BORROWED);
            ticket.setTicketType(TicketType.CHEMICAL_ONLY);
            ticket.setTicketDetails(new HashSet<>(Set.of(detail)));
            stubFindTicket();
            when(rentTicketDetailRepository.findAllById(any())).thenReturn(List.of(detail));

            assertThatThrownBy(() -> service.requestReturn(ticketId, userId,
                    returnReq(List.of(returnDetailReq(dId, "LOST", "   ")))))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("TC40 · returnStatus không hợp lệ — BadRequestException chứa 'không hợp lệ'")
        void tc40_invalidReturnStatus() {
            UUID dId = UUID.randomUUID();
            RentTicketDetail detail = buildDetail(dId);
            ticket.setStatus(TicketStatus.BORROWED);
            ticket.setTicketType(TicketType.CHEMICAL_ONLY);
            ticket.setTicketDetails(new HashSet<>(Set.of(detail)));
            stubFindTicket();
            when(rentTicketDetailRepository.findAllById(any())).thenReturn(List.of(detail));

            assertThatThrownBy(() -> service.requestReturn(ticketId, userId,
                    returnReq(List.of(returnDetailReq(dId, "INVALID_STATUS", null)))))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("không hợp lệ");
        }
    }

    // =========================================================================
    // Nhóm 7 · teacherConfirmReturn — 6 cases
    // =========================================================================
    @Nested
    @DisplayName("Nhóm 7 · teacherConfirmReturn()")
    class TeacherConfirmReturn {

        @Test
        @DisplayName("TC41 · ROOM_ONLY PENDING_RETURN → RETURNED, ghi actualReturnDate")
        void tc41_roomOnly_success() {
            ticket.setStatus(TicketStatus.PENDING_RETURN);
            stubFindTicket();
            when(rentTicketMapper.toResponse(any())).thenReturn(ticketResp);

            service.teacherConfirmReturn(ticketId, teacherId);

            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RETURNED);
            assertThat(ticket.getActualReturnDate()).isNotNull();
        }

        @Test
        @DisplayName("TC42 · CHEMICAL_ONLY — tất cả đã cập nhật returnStatus, không có vấn đề → RETURNED")
        void tc42_chemicalOnly_allOk() {
            ticket.setStatus(TicketStatus.PENDING_RETURN);
            ticket.setTicketType(TicketType.CHEMICAL_ONLY);
            ticket.setTicketDetails(new HashSet<>());
            stubFindTicket();
            when(rentTicketDetailRepository.existsByTicket_TicketIdAndReturnStatus(
                    ticketId, ReturnStatus.NOT_RETURNED)).thenReturn(false);
            when(rentTicketDetailRepository.findProblematicDetails(ticketId))
                    .thenReturn(Collections.emptyList());
            when(rentTicketMapper.toResponse(any())).thenReturn(ticketResp);

            service.teacherConfirmReturn(ticketId, teacherId);

            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RETURNED);
        }

        @Test
        @DisplayName("TC43 · CHEMICAL_ONLY — còn NOT_RETURNED — BadRequestException chứa 'chưa được cập nhật'")
        void tc43_chemicalOnly_hasUnreturned() {
            ticket.setStatus(TicketStatus.PENDING_RETURN);
            ticket.setTicketType(TicketType.CHEMICAL_ONLY);
            stubFindTicket();
            when(rentTicketDetailRepository.existsByTicket_TicketIdAndReturnStatus(
                    ticketId, ReturnStatus.NOT_RETURNED)).thenReturn(true);

            assertThatThrownBy(() -> service.teacherConfirmReturn(ticketId, teacherId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("chưa được cập nhật");
        }

        @Test
        @DisplayName("TC44 · Phiếu không PENDING_RETURN — BadRequestException chứa 'PENDING_RETURN'")
        void tc44_wrongStatus() {
            ticket.setStatus(TicketStatus.BORROWED);
            stubFindTicket();

            assertThatThrownBy(() -> service.teacherConfirmReturn(ticketId, teacherId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("PENDING_RETURN");
        }

        @Test
        @DisplayName("TC45 · Teacher không phụ trách phòng — BadRequestException chứa 'quyền'")
        void tc45_notRoomOwner() {
            ticket.setStatus(TicketStatus.PENDING_RETURN);
            stubFindTicket();

            assertThatThrownBy(() -> service.teacherConfirmReturn(ticketId, UUID.randomUUID()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("quyền");
        }

        @Test
        @DisplayName("TC46 · Có hóa chất DAMAGED khi trả — notify admin + notify requester")
        void tc46_problematicItems_notifyBoth() {
            ticket.setStatus(TicketStatus.PENDING_RETURN);
            ticket.setTicketType(TicketType.CHEMICAL_ONLY);
            ticket.setTicketDetails(new HashSet<>());
            stubFindTicket();
            when(rentTicketDetailRepository.existsByTicket_TicketIdAndReturnStatus(
                    ticketId, ReturnStatus.NOT_RETURNED)).thenReturn(false);
            RentTicketDetail damaged = buildDetail(UUID.randomUUID());
            damaged.setReturnStatus(ReturnStatus.DAMAGED);
            damaged.setReturnNote("Bình bị vỡ");
            damaged.setQuantityReturned(BigDecimal.ZERO);
            when(rentTicketDetailRepository.findProblematicDetails(ticketId))
                    .thenReturn(List.of(damaged));
            when(userRepository.findAllByRole("ADMIN")).thenReturn(List.of(admin));
            when(rentTicketMapper.toResponse(any())).thenReturn(ticketResp);

            assertThatCode(() -> service.teacherConfirmReturn(ticketId, teacherId))
                    .doesNotThrowAnyException();

            verify(userRepository).findAllByRole("ADMIN");
            verify(eventPublisher, atLeast(2)).publishEvent(any());
        }
    }

    // =========================================================================
    // Nhóm 8 · adminApprove — 7 cases
    // =========================================================================
    @Nested
    @DisplayName("Nhóm 8 · adminApprove()")
    class AdminApprove {

        @Test
        @DisplayName("TC47 · PENDING_ADMIN — duyệt → APPROVED, ghi adminApprovedBy + At")
        void tc47_approve_success() {
            ticket.setStatus(TicketStatus.PENDING_ADMIN);
            ticket.setTicketType(TicketType.CHEMICAL_ONLY);
            ticket.setTicketDetails(new HashSet<>());
            stubFindTicket();
            when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
            when(rentTicketMapper.toResponse(any())).thenReturn(ticketResp);

            service.adminApprove(ticketId, adminId, approveReq(true, null));

            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.APPROVED);
            assertThat(ticket.getAdminApprovedBy()).isEqualTo(admin);
            assertThat(ticket.getAdminApprovedAt()).isNotNull();
        }

        @Test
        @DisplayName("TC48 · Từ chối có lý do — REJECTED, rejectedBy=admin, không gọi lockInventory")
        void tc48_reject_withReason() {
            ticket.setStatus(TicketStatus.PENDING_ADMIN);
            stubFindTicket();
            when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
            when(rentTicketMapper.toResponse(any())).thenReturn(ticketResp);

            service.adminApprove(ticketId, adminId, approveReq(false, "Vượt quota hóa chất"));

            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.REJECTED);
            assertThat(ticket.getRejectedBy()).isEqualTo(admin);
            assertThat(ticket.getRejectedReason()).isEqualTo("Vượt quota hóa chất");
            verify(roomInventoryRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("TC49 · Từ chối không có lý do — BadRequestException chứa 'lý do'")
        void tc49_reject_noReason() {
            ticket.setStatus(TicketStatus.PENDING_ADMIN);
            stubFindTicket();

            assertThatThrownBy(() -> service.adminApprove(ticketId, adminId, approveReq(false, null)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("lý do");
        }

        @Test
        @DisplayName("TC50 · Phiếu không PENDING_ADMIN — BadRequestException chứa 'PENDING_ADMIN'")
        void tc50_wrongStatus() {
            ticket.setStatus(TicketStatus.PENDING_OWNER);
            stubFindTicket();

            assertThatThrownBy(() -> service.adminApprove(ticketId, adminId, approveReq(true, null)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("PENDING_ADMIN");
        }

        @Test
        @DisplayName("TC51 · Admin không tồn tại — ResourceNotFoundException")
        void tc51_adminNotFound() {
            ticket.setStatus(TicketStatus.PENDING_ADMIN);
            stubFindTicket();
            when(userRepository.findById(adminId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.adminApprove(ticketId, adminId, approveReq(true, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("TC52 · Duyệt nhưng tồn kho không đủ để lock — BadRequestException chứa 'không đủ số lượng'")
        void tc52_approve_insufficientInventory() {
            RentTicketDetail detail = buildDetail(UUID.randomUUID());
            detail.setQuantityBorrowed(new BigDecimal("999"));
            ticket.setStatus(TicketStatus.PENDING_ADMIN);
            ticket.setTicketType(TicketType.CHEMICAL_ONLY);
            ticket.setTicketDetails(new HashSet<>(Set.of(detail)));
            stubFindTicket();
            when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
            RoomInventory inv = inventory();
            inv.setTotalQuantity(BigDecimal.ONE);
            inv.setLockedQuantity(BigDecimal.ONE);
            when(roomInventoryRepository.findAllByRoom_RoomIdAndItem_ItemIdIn(any(), any()))
                    .thenReturn(List.of(inv));

            assertThatThrownBy(() -> service.adminApprove(ticketId, adminId, approveReq(true, null)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("không đủ số lượng");
        }

        @Test
        @DisplayName("TC53 · Duyệt thành công — gọi saveAll để cập nhật lockedQuantity")
        void tc53_approve_callsSaveAllInventory() {
            RentTicketDetail detail = buildDetail(UUID.randomUUID());
            detail.setQuantityBorrowed(new BigDecimal("5"));
            ticket.setStatus(TicketStatus.PENDING_ADMIN);
            ticket.setTicketType(TicketType.CHEMICAL_ONLY);
            ticket.setTicketDetails(new HashSet<>(Set.of(detail)));
            stubFindTicket();
            when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
            when(roomInventoryRepository.findAllByRoom_RoomIdAndItem_ItemIdIn(any(), any()))
                    .thenReturn(List.of(inventory()));
            when(rentTicketMapper.toResponse(any())).thenReturn(ticketResp);

            service.adminApprove(ticketId, adminId, approveReq(true, null));

            verify(roomInventoryRepository).saveAll(any());
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void stubFindTicket() {
        when(rentTicketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        lenient().when(rentTicketRepository.save(any())).thenReturn(ticket);
    }

    private void stubUserAndRoom() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
    }

    private User mockUser(UUID id, String username) {
        User u = new User();
        u.setUserId(id);
        u.setUsername(username);
        return u;
    }

    private RentTicketCreateRequest createReq(TicketType type, List<RentTicketDetailRequest> items) {
        RentTicketCreateRequest req = new RentTicketCreateRequest();
        req.setRoomId(roomId);
        req.setTicketType(type.name());
        req.setBorrowDate(LocalDateTime.now().plusHours(1));
        req.setExpectedReturnDate(LocalDateTime.now().plusHours(3));
        req.setItems(items);
        return req;
    }

    private RentTicketDetailRequest detailReq() {
        RentTicketDetailRequest r = new RentTicketDetailRequest();
        r.setItemId(itemId);
        r.setQuantityBorrowed(new BigDecimal("5"));
        return r;
    }

    private RentTicketApproveRequest approveReq(boolean approved, String reason) {
        RentTicketApproveRequest r = new RentTicketApproveRequest();
        r.setApproved(approved);
        r.setRejectedReason(reason);
        return r;
    }

    private ReturnTicketRequest returnReq(List<ReturnTicketDetailRequest> items) {
        ReturnTicketRequest r = new ReturnTicketRequest();
        r.setItems(items);
        return r;
    }

    private ReturnTicketDetailRequest returnDetailReq(UUID dId, String status, String note) {
        ReturnTicketDetailRequest r = new ReturnTicketDetailRequest();
        r.setDetailId(dId);
        r.setQuantityReturned(BigDecimal.ONE);
        r.setReturnStatus(status);
        r.setReturnNote(note);
        return r;
    }

    private Item activeItem() {
        Item item = new Item();
        item.setItemId(itemId);
        item.setName("Hóa chất A");
        item.setUnit("ml");
        item.setDeleted(false);
        return item;
    }

    private RoomInventory inventory() {
        RoomInventory inv = new RoomInventory();
        inv.setItem(activeItem());
        inv.setRoom(room);
        inv.setTotalQuantity(new BigDecimal("100"));
        inv.setLockedQuantity(BigDecimal.ZERO);
        return inv;
    }

    private RentTicketDetail buildDetail(UUID dId) {
        RentTicketDetail d = new RentTicketDetail();
        d.setDetailId(dId);
        d.setTicket(ticket);
        d.setItem(activeItem());
        d.setQuantityBorrowed(new BigDecimal("5"));
        d.setReturnStatus(ReturnStatus.NOT_RETURNED);
        return d;
    }
}