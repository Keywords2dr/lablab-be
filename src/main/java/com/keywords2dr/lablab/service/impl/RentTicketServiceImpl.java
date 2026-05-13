package com.keywords2dr.lablab.service.impl;

import com.keywords2dr.lablab.dto.ticket.*;
import com.keywords2dr.lablab.entity.*;
import com.keywords2dr.lablab.entity.enums.ReturnStatus;
import com.keywords2dr.lablab.entity.enums.TicketStatus;
import com.keywords2dr.lablab.entity.enums.TicketType;
import com.keywords2dr.lablab.event.NotificationEvent;
import com.keywords2dr.lablab.mapper.RentTicketMapper;
import com.keywords2dr.lablab.repository.*;
import com.keywords2dr.lablab.service.RentTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RentTicketServiceImpl implements RentTicketService {

    private final RentTicketRepository ticketRepository;
    private final RentTicketDetailRepository detailRepository;
    private final RoomInventoryRepository inventoryRepository;
    private final RoomRepository roomRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final RentTicketMapper ticketMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public RentTicketResponse createTicket(RentTicketCreateRequest request, UUID requesterId) {
        User requester = userRepository.findById(requesterId).orElseThrow(() -> new RuntimeException("Người dùng không tồn tại!"));
        Room room = roomRepository.findById(request.getRoomId()).orElseThrow(() -> new RuntimeException("Phòng không tồn tại!"));
        TicketType type = TicketType.valueOf(request.getTicketType());

        if (type != TicketType.CHEMICAL_ONLY) {
            if (ticketRepository.existsConflictingBooking(room.getRoomId(), request.getBorrowDate(), request.getExpectedReturnDate())) {
                throw new RuntimeException("Phòng đã được mượn trong khoảng thời gian này!");
            }
        }

        RentTicket ticket = RentTicket.builder()
                .requester(requester)
                .fromRoom(room)
                .ticketType(type)
                .status(TicketStatus.PENDING_OWNER)
                .purposeType(request.getPurposeType())
                .subjectName(request.getSubjectName())
                .lessonDetail(request.getLessonDetail())
                .classCode(request.getClassCode())
                .borrowDate(request.getBorrowDate())
                .expectedReturnDate(request.getExpectedReturnDate())
                .build();

        List<RentTicketDetail> details = new ArrayList<>();
        if (type != TicketType.ROOM_ONLY && request.getItems() != null) {
            for (RentTicketDetailRequest itemReq : request.getItems()) {
                Item item = itemRepository.findById(itemReq.getItemId()).orElseThrow();
                RoomInventory inv = inventoryRepository.findByRoom_RoomIdAndItem_ItemId(room.getRoomId(), item.getItemId())
                        .orElseThrow(() -> new RuntimeException("Vật tư " + item.getName() + " không có trong phòng!"));

                BigDecimal available = inv.getTotalQuantity().subtract(inv.getLockedQuantity());
                if (itemReq.getQuantityBorrowed().compareTo(available) > 0) {
                    throw new RuntimeException("Không đủ số lượng khả dụng cho: " + item.getName());
                }
                inv.setLockedQuantity(inv.getLockedQuantity().add(itemReq.getQuantityBorrowed()));
                inventoryRepository.save(inv);

                details.add(RentTicketDetail.builder()
                        .ticket(ticket)
                        .item(item)
                        .quantityBorrowed(itemReq.getQuantityBorrowed())
                        .returnStatus(ReturnStatus.NOT_RETURNED)
                        .build());
            }
        }
        ticket.setTicketDetails(details);
        RentTicket saved = ticketRepository.save(ticket);
        notifyRoomTeachers(room, "Phiếu mượn mới", requester.getUsername() + " yêu cầu mượn tại " + room.getRoomName(), "TICKET_NEW", saved.getTicketId());
        return ticketMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RentTicketResponse teacherApprove(UUID ticketId, UUID teacherId, RentTicketApproveRequest request) {
        RentTicket ticket = ticketRepository.findById(ticketId).orElseThrow();
        if (ticket.getStatus() != TicketStatus.PENDING_OWNER) throw new RuntimeException("Trạng thái không hợp lệ!");

        User teacher = userRepository.findById(teacherId).orElseThrow();
        ticket.setOwnerApprovedBy(teacher);
        ticket.setOwnerApprovedAt(LocalDateTime.now());

        if (Boolean.FALSE.equals(request.getApproved())) {
            ticket.setStatus(TicketStatus.REJECTED);
            ticket.setRejectedReason(request.getRejectedReason());
            ticket.setRejectedAt(LocalDateTime.now());
            releaseLock(ticket);
            notifyUser(ticket.getRequester(), "Phiếu bị từ chối", "Teacher đã từ chối yêu cầu của bạn.", "TICKET_REJECTED", ticketId);
        } else {
            if (ticket.getTicketType() == TicketType.ROOM_ONLY) {
                ticket.setStatus(TicketStatus.APPROVED);
                notifyUser(ticket.getRequester(), "Phiếu đã duyệt", "Yêu cầu mượn phòng đã được duyệt.", "TICKET_APPROVED", ticketId);
            } else {
                ticket.setStatus(TicketStatus.PENDING_ADMIN);
                notifyAdmins("Duyệt hóa chất", "Phiếu mượn của " + ticket.getRequester().getUsername() + " chờ Admin duyệt bước 2.", "TICKET_ADMIN_PENDING", ticketId);
            }
        }
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    @Override
    @Transactional
    public RentTicketResponse adminApprove(UUID ticketId, UUID adminId, RentTicketApproveRequest request) {
        RentTicket ticket = ticketRepository.findById(ticketId).orElseThrow();
        if (ticket.getStatus() != TicketStatus.PENDING_ADMIN) throw new RuntimeException("Trạng thái không hợp lệ!");

        User admin = userRepository.findById(adminId).orElseThrow();
        ticket.setAdminApprovedBy(admin);
        ticket.setAdminApprovedAt(LocalDateTime.now());

        if (Boolean.FALSE.equals(request.getApproved())) {
            ticket.setStatus(TicketStatus.REJECTED);
            ticket.setRejectedReason(request.getRejectedReason());
            ticket.setRejectedAt(LocalDateTime.now());
            releaseLock(ticket);
        } else {
            ticket.setStatus(TicketStatus.APPROVED);
            notifyUser(ticket.getRequester(), "Phiếu đã duyệt", "Admin đã duyệt cấp hóa chất cho bạn.", "TICKET_APPROVED", ticketId);
        }
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    @Override
    @Transactional
    public void startBorrowing(UUID ticketId, UUID requesterId) {
        RentTicket ticket = ticketRepository.findById(ticketId).orElseThrow();
        if (ticket.getStatus() != TicketStatus.APPROVED) throw new RuntimeException("Phiếu chưa được duyệt hoàn toàn!");

        if (ticket.getTicketDetails() != null) {
            for (RentTicketDetail d : ticket.getTicketDetails()) {
                RoomInventory inv = inventoryRepository.findByRoom_RoomIdAndItem_ItemId(ticket.getFromRoom().getRoomId(), d.getItem().getItemId()).orElseThrow();
                inv.setLockedQuantity(inv.getLockedQuantity().subtract(d.getQuantityBorrowed()));
                inv.setTotalQuantity(inv.getTotalQuantity().subtract(d.getQuantityBorrowed()));
                inventoryRepository.save(inv);
            }
        }
        ticket.setStatus(TicketStatus.BORROWED);
        ticketRepository.save(ticket);
    }

    @Override
    @Transactional
    public RentTicketResponse teacherConfirmReturn(UUID ticketId, UUID teacherId, ReturnTicketRequest request) {
        RentTicket ticket = ticketRepository.findById(ticketId).orElseThrow();
        if (ticket.getStatus() != TicketStatus.PENDING_RETURN) throw new RuntimeException("Phiếu không ở trạng thái chờ trả!");

        if (ticket.getTicketType() != TicketType.ROOM_ONLY && request.getItems() != null) {
            Map<UUID, ReturnTicketDetailRequest> reqMap = request.getItems().stream().collect(Collectors.toMap(ReturnTicketDetailRequest::getDetailId, r -> r));
            for (RentTicketDetail detail : ticket.getTicketDetails()) {
                ReturnTicketDetailRequest req = reqMap.get(detail.getDetailId());
                if (req != null) {
                    BigDecimal borrowed = detail.getQuantityBorrowed();
                    BigDecimal returned = req.getQuantityReturned() != null ? req.getQuantityReturned() : BigDecimal.ZERO;
                    ReturnStatus reqStatus = ReturnStatus.valueOf(req.getReturnStatus());

                    // --- BỘ LỌC BẢO VỆ LOGIC NGHIỆP VỤ ---

                    // 1. Chặn số âm
                    if (returned.compareTo(BigDecimal.ZERO) < 0) {
                        throw new RuntimeException("Lỗi: Số lượng trả (" + returned + ") không được là số âm!");
                    }

                    // 2. Chặn trả lớn hơn mượn
                    if (returned.compareTo(borrowed) > 0) {
                        throw new RuntimeException("Lỗi: Số lượng trả (" + returned + ") không được lớn hơn số lượng đã mượn (" + borrowed + ")!");
                    }

                    // 3. Bao quát toàn bộ trường hợp của Enum ReturnStatus
                    switch (reqStatus) {
                        case RETURNED:
                            if (returned.compareTo(borrowed) != 0) {
                                throw new RuntimeException("Lỗi logic: Trạng thái là RETURNED (Đã trả đủ) nhưng số lượng trả (" + returned + ") lại khác số lượng mượn (" + borrowed + ")!");
                            }
                            break;

                        case PARTIAL:
                            if (returned.compareTo(borrowed) == 0) {
                                throw new RuntimeException("Lỗi logic: Đã trả đủ (" + borrowed + "). Trạng thái phải là RETURNED, không thể là PARTIAL!");
                            }
                            if (returned.compareTo(BigDecimal.ZERO) == 0) {
                                throw new RuntimeException("Lỗi logic: Số lượng trả bằng 0. Vui lòng dùng trạng thái LOST hoặc DAMAGED thay vì PARTIAL!");
                            }
                            break;

                        case DAMAGED:
                        case LOST:
                            if (returned.compareTo(borrowed) == 0) {
                                throw new RuntimeException("Lỗi logic: Trạng thái là " + reqStatus + " nhưng số nguyên vẹn cất lại vào kho lại bằng đúng số mượn (" + borrowed + ")!");
                            }
                            if (req.getReturnNote() == null || req.getReturnNote().isBlank()) {
                                throw new RuntimeException("Lỗi: Vui lòng nhập ghi chú (returnNote) lý do bị hỏng hoặc mất để Admin kiểm tra!");
                            }
                            break;

                        case NOT_RETURNED:
                            throw new RuntimeException("Lỗi: Phiếu đang chốt trả đồ, không thể để trạng thái là NOT_RETURNED!");
                    }

                    // --- KẾT THÚC BỘ LỌC ---

                    detail.setQuantityReturned(returned);
                    detail.setReturnStatus(reqStatus);
                    detail.setReturnNote(req.getReturnNote());

                    // CỘNG PHẦN NGUYÊN VẸN LẠI VÀO KHO
                    if (returned.compareTo(BigDecimal.ZERO) > 0) {
                        RoomInventory inv = inventoryRepository.findByRoom_RoomIdAndItem_ItemId(ticket.getFromRoom().getRoomId(), detail.getItem().getItemId()).orElseThrow();
                        inv.setTotalQuantity(inv.getTotalQuantity().add(returned));
                        inventoryRepository.save(inv);
                    }
                }
            }
        }
        ticket.setStatus(TicketStatus.RETURNED);
        ticket.setActualReturnDate(LocalDateTime.now());
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    private void releaseLock(RentTicket t) {
        if (t.getTicketDetails() != null) {
            for (RentTicketDetail d : t.getTicketDetails()) {
                RoomInventory inv = inventoryRepository.findByRoom_RoomIdAndItem_ItemId(t.getFromRoom().getRoomId(), d.getItem().getItemId()).orElseThrow();
                inv.setLockedQuantity(inv.getLockedQuantity().subtract(d.getQuantityBorrowed()));
                inventoryRepository.save(inv);
            }
        }
    }

    private void notifyRoomTeachers(Room room, String title, String msg, String type, UUID id) {
        room.getStaffAssignments().forEach(sa -> eventPublisher.publishEvent(new NotificationEvent(sa.getUser().getUserId(), title, msg, type)));
    }
    private void notifyAdmins(String title, String msg, String type, UUID id) {
        userRepository.findAllByRole("ADMIN").forEach(a -> eventPublisher.publishEvent(new NotificationEvent(a.getUserId(), title, msg, type)));
    }
    private void notifyUser(User u, String title, String msg, String type, UUID id) {
        eventPublisher.publishEvent(new NotificationEvent(u.getUserId(), title, msg, type));
    }

    @Override @Transactional(readOnly = true) public Page<RentTicketSummaryResponse> getMyTickets(UUID rid, Pageable p) { return ticketRepository.findAllByRequester_UserIdOrderByCreatedAtDesc(rid, p).map(ticketMapper::toSummaryResponse); }
    @Override @Transactional(readOnly = true) public RentTicketResponse getTicketDetail(UUID id) { return ticketMapper.toResponse(ticketRepository.findById(id).orElseThrow()); }
    @Override @Transactional(readOnly = true) public Page<RentTicketSummaryResponse> getTeacherPendingTickets(UUID tid, Pageable p) { return null; }
    @Override @Transactional(readOnly = true) public Page<RentTicketSummaryResponse> getAdminPendingTickets(Pageable p) { return null; }
    @Override @Transactional(readOnly = true) public Page<RentTicketSummaryResponse> getAllTickets(UUID rid, String s, String t, Pageable p) { return null; }
    @Override @Transactional public void cancelTicket(UUID id, UUID rid) { /* Logic cancel */ }
    @Override @Transactional public void requestReturn(UUID id, UUID rid) { RentTicket t = ticketRepository.findById(id).orElseThrow(); t.setStatus(TicketStatus.PENDING_RETURN); ticketRepository.save(t); }
}