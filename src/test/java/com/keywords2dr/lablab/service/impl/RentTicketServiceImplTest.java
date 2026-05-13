package com.keywords2dr.lablab.service.impl;

import com.keywords2dr.lablab.dto.ticket.*;
import com.keywords2dr.lablab.entity.*;
import com.keywords2dr.lablab.entity.enums.TicketStatus;
import com.keywords2dr.lablab.entity.enums.TicketType;
import com.keywords2dr.lablab.exception.BadRequestException;
import com.keywords2dr.lablab.exception.ResourceNotFoundException;
import com.keywords2dr.lablab.mapper.RentTicketMapper;
import com.keywords2dr.lablab.repository.*;
import com.keywords2dr.lablab.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentTicketServiceImplTest {

    @Mock private RentTicketRepository rentTicketRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoomInventoryRepository roomInventoryRepository;
    @Mock private RentTicketDetailRepository rentTicketDetailRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private RentTicketMapper rentTicketMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RentTicketServiceImpl rentTicketService;

    private User mockStudent, mockTeacher, mockAdmin;
    private Room mockRoom;
    private RentTicket mockTicket;

    @BeforeEach
    void setUp() {
        mockStudent = new User(); mockStudent.setUserId(UUID.randomUUID());
        mockTeacher = new User(); mockTeacher.setUserId(UUID.randomUUID());
        mockAdmin = new User(); mockAdmin.setUserId(UUID.randomUUID());

        mockRoom = new Room();
        mockRoom.setRoomId(UUID.randomUUID());
        mockRoom.setRoomName("Lab Vật Lý");
        mockRoom.setIsActive(true);

        RoomStaffAssignment sa = new RoomStaffAssignment();
        sa.setUser(mockTeacher);
        sa.setRoom(mockRoom);
        mockRoom.setStaffAssignments(List.of(sa));

        mockTicket = new RentTicket();
        mockTicket.setTicketId(UUID.randomUUID());
        mockTicket.setFromRoom(mockRoom);
        mockTicket.setRequester(mockStudent);
        mockTicket.setTicketType(TicketType.ROOM_ONLY);
        mockTicket.setStatus(TicketStatus.PENDING_OWNER);
    }

    // ================== CREATE TICKET ==================

    @Test
    void createTicket_ThrowsException_WhenRoomNotFound() {
        RentTicketCreateRequest req = new RentTicketCreateRequest();
        req.setRoomId(UUID.randomUUID());

        when(userRepository.findById(mockStudent.getUserId())).thenReturn(Optional.of(mockStudent));
        when(roomRepository.findById(req.getRoomId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> rentTicketService.createTicket(mockStudent.getUserId(), req));
    }

    @Test
    void createTicket_ThrowsException_WhenRoomInactive() {
        mockRoom.setIsActive(false);
        RentTicketCreateRequest req = new RentTicketCreateRequest();
        req.setRoomId(mockRoom.getRoomId());

        when(userRepository.findById(mockStudent.getUserId())).thenReturn(Optional.of(mockStudent));
        when(roomRepository.findById(req.getRoomId())).thenReturn(Optional.of(mockRoom));

        assertThrows(BadRequestException.class, () -> rentTicketService.createTicket(mockStudent.getUserId(), req));
    }

    @Test
    void createTicket_ThrowsException_WhenConflictingBooking() {
        RentTicketCreateRequest req = new RentTicketCreateRequest();
        req.setRoomId(mockRoom.getRoomId());
        req.setTicketType("ROOM_ONLY");
        req.setBorrowDate(LocalDateTime.now().plusDays(1));
        req.setExpectedReturnDate(LocalDateTime.now().plusDays(1).plusHours(2));

        when(userRepository.findById(mockStudent.getUserId())).thenReturn(Optional.of(mockStudent));
        when(roomRepository.findById(req.getRoomId())).thenReturn(Optional.of(mockRoom));
        when(rentTicketRepository.existsConflictingBooking(any(), any(), any())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> rentTicketService.createTicket(mockStudent.getUserId(), req));
    }

    // ================== CANCEL TICKET ==================

    @Test
    void cancelTicket_ThrowsException_WhenNotCreator() {
        when(rentTicketRepository.findById(mockTicket.getTicketId())).thenReturn(Optional.of(mockTicket));
        // Dùng UUID ngẫu nhiên khác với mockStudent.getUserId()
        assertThrows(BadRequestException.class, () -> rentTicketService.cancelTicket(mockTicket.getTicketId(), UUID.randomUUID()));
    }

    @Test
    void cancelTicket_ThrowsException_WhenInvalidStatus() {
        mockTicket.setStatus(TicketStatus.BORROWED); // Đang mượn thì không được hủy
        when(rentTicketRepository.findById(mockTicket.getTicketId())).thenReturn(Optional.of(mockTicket));

        assertThrows(BadRequestException.class, () -> rentTicketService.cancelTicket(mockTicket.getTicketId(), mockStudent.getUserId()));
    }

    // ================== TEACHER APPROVE ==================

    @Test
    void teacherApprove_ThrowsException_WhenNotOwner() {
        mockRoom.setStaffAssignments(List.of()); // Phòng không có giáo viên quản lý
        RentTicketApproveRequest req = new RentTicketApproveRequest();

        when(rentTicketRepository.findById(mockTicket.getTicketId())).thenReturn(Optional.of(mockTicket));
        assertThrows(BadRequestException.class, () -> rentTicketService.teacherApprove(mockTicket.getTicketId(), mockTeacher.getUserId(), req));
    }

    @Test
    void teacherApprove_ThrowsException_WhenInvalidStatus() {
        mockTicket.setStatus(TicketStatus.APPROVED); // Đã duyệt rồi thì không duyệt lại
        RentTicketApproveRequest req = new RentTicketApproveRequest();

        when(rentTicketRepository.findById(mockTicket.getTicketId())).thenReturn(Optional.of(mockTicket));
        assertThrows(BadRequestException.class, () -> rentTicketService.teacherApprove(mockTicket.getTicketId(), mockTeacher.getUserId(), req));
    }

    @Test
    void teacherApprove_Reject_MovesToRejected() {
        RentTicketApproveRequest req = new RentTicketApproveRequest();
        req.setApproved(false);
        req.setRejectedReason("Phòng đang sửa chữa");

        when(rentTicketRepository.findById(mockTicket.getTicketId())).thenReturn(Optional.of(mockTicket));
        when(userRepository.findById(mockTeacher.getUserId())).thenReturn(Optional.of(mockTeacher));
        when(rentTicketMapper.toResponse(any())).thenReturn(new RentTicketResponse());

        rentTicketService.teacherApprove(mockTicket.getTicketId(), mockTeacher.getUserId(), req);

        assertEquals(TicketStatus.REJECTED, mockTicket.getStatus());
        assertEquals("Phòng đang sửa chữa", mockTicket.getRejectedReason());
    }

    // ================== REQUEST RETURN & ACTIVATE ==================

    @Test
    void requestReturn_ThrowsException_WhenInvalidStatus() {
        mockTicket.setStatus(TicketStatus.APPROVED); // Chưa nhận đồ (BORROWED) thì ko được trả
        when(rentTicketRepository.findById(mockTicket.getTicketId())).thenReturn(Optional.of(mockTicket));
        assertThrows(BadRequestException.class, () -> rentTicketService.requestReturn(mockTicket.getTicketId(), mockStudent.getUserId(), new ReturnTicketRequest()));
    }

    @Test
    void requestReturn_ThrowsException_WhenNotCreator() {
        mockTicket.setStatus(TicketStatus.BORROWED);
        when(rentTicketRepository.findById(mockTicket.getTicketId())).thenReturn(Optional.of(mockTicket));
        assertThrows(BadRequestException.class, () -> rentTicketService.requestReturn(mockTicket.getTicketId(), UUID.randomUUID(), new ReturnTicketRequest()));
    }

    @Test
    void activateTicket_ThrowsException_WhenInvalidStatus() {
        mockTicket.setStatus(TicketStatus.PENDING_OWNER); // Phải là APPROVED mới được activate
        when(rentTicketRepository.findById(mockTicket.getTicketId())).thenReturn(Optional.of(mockTicket));
        assertThrows(BadRequestException.class, () -> rentTicketService.activateTicket(mockTicket.getTicketId(), mockTeacher.getUserId()));
    }

    // ================== ADMIN APPROVE ==================

    @Test
    void adminApprove_ThrowsException_WhenInsufficientInventory() {
        mockTicket.setTicketType(TicketType.CHEMICAL_ONLY);
        mockTicket.setStatus(TicketStatus.PENDING_ADMIN);

        Item mockItem = new Item(); mockItem.setItemId(UUID.randomUUID());
        RentTicketDetail detail = new RentTicketDetail();
        detail.setItem(mockItem);
        detail.setQuantityBorrowed(new BigDecimal("15.0")); // Yêu cầu mượn 15
        mockTicket.setTicketDetails(List.of(detail));

        RoomInventory inventory = new RoomInventory();
        inventory.setItem(mockItem);
        inventory.setTotalQuantity(new BigDecimal("20"));
        inventory.setLockedQuantity(new BigDecimal("10")); // Khả dụng chỉ còn 10 (20 - 10)

        RentTicketApproveRequest req = new RentTicketApproveRequest();
        req.setApproved(true);

        when(rentTicketRepository.findById(mockTicket.getTicketId())).thenReturn(Optional.of(mockTicket));
        when(userRepository.findById(mockAdmin.getUserId())).thenReturn(Optional.of(mockAdmin));
        when(roomInventoryRepository.findAllByRoom_RoomIdAndItem_ItemIdIn(any(), any())).thenReturn(List.of(inventory));

        assertThrows(BadRequestException.class, () -> rentTicketService.adminApprove(mockTicket.getTicketId(), mockAdmin.getUserId(), req));
    }

    @Test
    void adminApprove_Reject_MovesToRejected_And_DoesNotLockInventory() {
        mockTicket.setStatus(TicketStatus.PENDING_ADMIN);
        RentTicketApproveRequest req = new RentTicketApproveRequest();
        req.setApproved(false);
        req.setRejectedReason("Hết hóa chất");

        when(rentTicketRepository.findById(mockTicket.getTicketId())).thenReturn(Optional.of(mockTicket));
        when(userRepository.findById(mockAdmin.getUserId())).thenReturn(Optional.of(mockAdmin));
        when(rentTicketMapper.toResponse(any())).thenReturn(new RentTicketResponse());

        rentTicketService.adminApprove(mockTicket.getTicketId(), mockAdmin.getUserId(), req);

        assertEquals(TicketStatus.REJECTED, mockTicket.getStatus());
        verify(roomInventoryRepository, never()).saveAll(any()); // Đảm bảo ko gọi hàm lock inventory
    }
}