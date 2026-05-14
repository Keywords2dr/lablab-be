package com.keywords2dr.lablab.service.impl;

import com.keywords2dr.lablab.dto.ticket.*;
import com.keywords2dr.lablab.entity.*;
import com.keywords2dr.lablab.entity.enums.ReturnStatus;
import com.keywords2dr.lablab.entity.enums.TicketStatus;
import com.keywords2dr.lablab.entity.enums.TicketType;
import com.keywords2dr.lablab.event.NotificationEvent;
import com.keywords2dr.lablab.exception.BadRequestException;
import com.keywords2dr.lablab.exception.ResourceNotFoundException;
import com.keywords2dr.lablab.mapper.RentTicketMapper;
import com.keywords2dr.lablab.repository.*;
import com.keywords2dr.lablab.repository.specification.RentTicketSpecification;
import com.keywords2dr.lablab.service.AuditLogService;
import com.keywords2dr.lablab.service.RentTicketService;
import com.keywords2dr.lablab.util.UserNameResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RentTicketServiceImpl implements RentTicketService {

    private final RentTicketRepository rentTicketRepository;
    private final RentTicketDetailRepository rentTicketDetailRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final RoomInventoryRepository roomInventoryRepository;
    private final RentTicketMapper rentTicketMapper;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public RentTicketResponse createTicket(UUID requesterId, RentTicketCreateRequest request) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng!"));

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng Lab!"));

        if (Boolean.FALSE.equals(room.getIsActive())) {
            throw new BadRequestException(
                    "Phòng [" + room.getRoomName() + "] đang bị khóa, không thể tạo phiếu mượn!");
        }

        if (!request.getBorrowDate().isBefore(request.getExpectedReturnDate())) {
            throw new BadRequestException("Thời gian trả phải sau thời gian mượn!");
        }

        TicketType ticketType;
        try {
            ticketType = TicketType.valueOf(request.getTicketType());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Loại phiếu không hợp lệ: " + request.getTicketType());
        }

        if (ticketType != TicketType.ROOM_ONLY) {
            if (request.getItems() == null || request.getItems().isEmpty()) {
                throw new BadRequestException("Phiếu mượn hóa chất phải có ít nhất 1 hóa chất!");
            }
        }

        if (ticketType != TicketType.CHEMICAL_ONLY) {
            boolean hasConflict = rentTicketRepository.existsConflictingBooking(
                    room.getRoomId(), request.getBorrowDate(), request.getExpectedReturnDate());
            if (hasConflict) {
                throw new BadRequestException(
                        "Phòng [" + room.getRoomName() + "] đã có lịch mượn trong khoảng thời gian này!");
            }
        }

        RentTicket ticket = RentTicket.builder()
                .requester(requester)
                .fromRoom(room)
                .ticketType(ticketType)
                .status(TicketStatus.PENDING_OWNER)
                .purposeType(request.getPurposeType())
                .subjectName(request.getSubjectName())
                .lessonDetail(request.getLessonDetail())
                .classCode(request.getClassCode())
                .borrowDate(request.getBorrowDate())
                .expectedReturnDate(request.getExpectedReturnDate())
                .build();

        RentTicket savedTicket = rentTicketRepository.save(ticket);

        if (ticketType != TicketType.ROOM_ONLY && request.getItems() != null) {
            List<RentTicketDetail> details = buildTicketDetails(savedTicket, request.getItems(), room);
            rentTicketDetailRepository.saveAll(details);
            savedTicket.setTicketDetails(details);
        }

        if (room.getStaffAssignments() == null || room.getStaffAssignments().isEmpty()) {
            notifyAllAdmins(
                    "Phiếu mượn mới (phòng chưa có staff)",
                    String.format("[%s] vừa tạo phiếu mượn %s tại phòng %s nhưng phòng chưa có staff phụ trách. Vui lòng xem xét.",
                            UserNameResolver.resolve(requester), ticketType.name(), room.getRoomName()),
                    "TICKET_CREATED_NO_STAFF");
        } else {
            notifyRoomStaff(room,
                    "Phiếu mượn mới",
                    String.format("[%s] vừa tạo phiếu mượn %s tại phòng %s. Vui lòng xem xét và duyệt.",
                            UserNameResolver.resolve(requester), ticketType.name(), room.getRoomName()),
                    "TICKET_CREATED");
        }

        RentTicketResponse newState = rentTicketMapper.toResponse(savedTicket);
        auditLogService.logAction("CREATE", "RENT_TICKET", savedTicket.getTicketId(),
                null, newState);

        return newState;
    }

    @Override
    @Transactional
    public void cancelTicket(UUID ticketId, UUID requesterId) {
        RentTicket ticket = findTicketById(ticketId);

        if (!ticket.getRequester().getUserId().equals(requesterId)) {
            throw new BadRequestException("Bạn không có quyền hủy phiếu này!");
        }
        if (ticket.getStatus() != TicketStatus.PENDING_OWNER
                && ticket.getStatus() != TicketStatus.PENDING_ADMIN) {
            throw new BadRequestException(
                    "Chỉ có thể hủy phiếu đang chờ duyệt! Trạng thái hiện tại: " + ticket.getStatus());
        }

        RentTicketResponse oldState = rentTicketMapper.toResponse(ticket);
        ticket.setStatus(TicketStatus.CANCELLED);
        rentTicketRepository.save(ticket);

        notifyRoomStaff(ticket.getFromRoom(),
                "Phiếu mượn đã bị hủy",
                String.format("[%s] đã hủy phiếu mượn tại phòng %s.",
                        UserNameResolver.resolve(ticket.getRequester()),
                        ticket.getFromRoom().getRoomName()),
                "TICKET_CANCELLED");

        notifyUser(ticket.getRequester().getUserId(),
                "Hủy phiếu mượn thành công",
                String.format("Phiếu mượn tại phòng %s của bạn đã được hủy thành công.",
                        ticket.getFromRoom().getRoomName()),
                "TICKET_CANCELLED_CONFIRMATION");

        RentTicketResponse newState = rentTicketMapper.toResponse(ticket);
        auditLogService.logAction("CANCEL", "RENT_TICKET", ticketId, oldState, newState);
    }

    // ── XEM CHI TIẾT ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public RentTicketResponse getTicketById(UUID ticketId) {
        return rentTicketMapper.toResponse(findTicketById(ticketId));
    }

    // ── TEACHER ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<RentTicketSummaryResponse> getPendingTicketsForTeacher(UUID teacherId) {
        return rentTicketMapper.toSummaryList(
                rentTicketRepository.findPendingTicketsByTeacher(teacherId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RentTicketSummaryResponse> getAllTicketsForTeacher(UUID teacherId, Pageable pageable) {
        return rentTicketRepository.findAllTicketsByTeacher(teacherId, pageable)
                .map(rentTicketMapper::toSummaryResponse);
    }

    @Override
    @Transactional
    public RentTicketResponse teacherApprove(UUID ticketId, UUID teacherId, RentTicketApproveRequest request) {
        RentTicket ticket = findTicketById(ticketId);

        boolean isOwner = ticket.getFromRoom().getStaffAssignments().stream()
                .anyMatch(sa -> sa.getUser().getUserId().equals(teacherId));
        if (!isOwner) {
            throw new BadRequestException("Bạn không có quyền duyệt phiếu của phòng này!");
        }

        if (ticket.getStatus() != TicketStatus.PENDING_OWNER) {
            throw new BadRequestException(
                    "Phiếu phải ở trạng thái PENDING_OWNER! Trạng thái hiện tại: " + ticket.getStatus());
        }

        if (Boolean.FALSE.equals(request.getApproved())) {
            validateRejectedReason(request.getRejectedReason());
        }

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng!"));

        RentTicketResponse oldState = rentTicketMapper.toResponse(ticket);

        if (Boolean.FALSE.equals(request.getApproved())) {
            ticket.setStatus(TicketStatus.REJECTED);
            ticket.setRejectedReason(request.getRejectedReason());
            ticket.setRejectedAt(LocalDateTime.now());
            ticket.setRejectedBy(teacher);

            notifyUser(ticket.getRequester().getUserId(),
                    "Phiếu mượn bị từ chối",
                    String.format("Phiếu mượn tại phòng %s đã bị Teacher từ chối. Lý do: %s",
                            ticket.getFromRoom().getRoomName(), request.getRejectedReason()),
                    "TICKET_REJECTED_BY_TEACHER");
        } else {
            ticket.setOwnerApprovedBy(teacher);
            ticket.setOwnerApprovedAt(LocalDateTime.now());

            if (ticket.getTicketType() == TicketType.ROOM_ONLY) {
                ticket.setStatus(TicketStatus.APPROVED);
                notifyUser(ticket.getRequester().getUserId(),
                        "Phiếu mượn được duyệt",
                        String.format("Phiếu mượn phòng %s đã được Teacher duyệt.",
                                ticket.getFromRoom().getRoomName()),
                        "TICKET_APPROVED");
            } else {
                ticket.setStatus(TicketStatus.PENDING_ADMIN);
                notifyUser(ticket.getRequester().getUserId(),
                        "Phiếu mượn đang chờ Admin duyệt",
                        String.format("Teacher đã duyệt phiếu mượn tại phòng %s. Đang chờ Admin xác nhận.",
                                ticket.getFromRoom().getRoomName()),
                        "TICKET_PENDING_ADMIN");

                notifyAllAdmins(
                        "Phiếu mượn chờ duyệt",
                        String.format("Phiếu mượn của [%s] tại phòng %s đã được Teacher duyệt. Vui lòng xem xét và phê duyệt.",
                                UserNameResolver.resolve(ticket.getRequester()),
                                ticket.getFromRoom().getRoomName()),
                        "TICKET_PENDING_ADMIN_ALERT");
            }
        }

        rentTicketRepository.save(ticket);
        RentTicketResponse newState = rentTicketMapper.toResponse(ticket);
        auditLogService.logAction("TEACHER_APPROVE", "RENT_TICKET", ticketId, oldState, newState);
        return newState;
    }

    @Override
    @Transactional
    public RentTicketResponse activateTicket(UUID ticketId, UUID teacherId) {
        RentTicket ticket = findTicketById(ticketId);

        boolean isOwner = ticket.getFromRoom().getStaffAssignments().stream()
                .anyMatch(sa -> sa.getUser().getUserId().equals(teacherId));
        if (!isOwner) {
            throw new BadRequestException("Bạn không có quyền bàn giao đồ cho phòng này!");
        }

        if (ticket.getStatus() != TicketStatus.APPROVED) {
            throw new BadRequestException(
                    "Chỉ có thể bàn giao khi phiếu ở trạng thái APPROVED! "
                            + "Trạng thái hiện tại: " + ticket.getStatus());
        }

        RentTicketResponse oldState = rentTicketMapper.toResponse(ticket);

        ticket.setStatus(TicketStatus.BORROWED);
        rentTicketRepository.save(ticket);

        notifyUser(ticket.getRequester().getUserId(),
                "Đồ đã được bàn giao",
                String.format("Teacher đã bàn giao đồ cho bạn tại phòng %s. "
                                + "Vui lòng yêu cầu trả khi hoàn tất sử dụng.",
                        ticket.getFromRoom().getRoomName()),
                "TICKET_BORROWED");

        RentTicketResponse newState = rentTicketMapper.toResponse(ticket);
        auditLogService.logAction("ACTIVATE", "RENT_TICKET", ticketId, oldState, newState);
        return newState;
    }

    @Override
    @Transactional
    public RentTicketResponse teacherConfirmReturn(UUID ticketId, UUID teacherId) {
        RentTicket ticket = findTicketById(ticketId);

        boolean isOwner = ticket.getFromRoom().getStaffAssignments().stream()
                .anyMatch(sa -> sa.getUser().getUserId().equals(teacherId));
        if (!isOwner) {
            throw new BadRequestException("Bạn không có quyền xác nhận trả cho phòng này!");
        }

        if (ticket.getStatus() != TicketStatus.PENDING_RETURN) {
            throw new BadRequestException(
                    "Phiếu phải ở trạng thái PENDING_RETURN! Trạng thái hiện tại: " + ticket.getStatus());
        }

        if (ticket.getTicketType() != TicketType.ROOM_ONLY) {
            boolean hasUnreturned = rentTicketDetailRepository
                    .existsByTicket_TicketIdAndReturnStatus(ticketId, ReturnStatus.NOT_RETURNED);
            if (hasUnreturned) {
                throw new BadRequestException(
                        "Vẫn còn hóa chất chưa được cập nhật trạng thái trả. Vui lòng kiểm tra lại!");
            }
        }

        RentTicketResponse oldState = rentTicketMapper.toResponse(ticket);

        ticket.setStatus(TicketStatus.RETURNED);
        ticket.setActualReturnDate(LocalDateTime.now());
        rentTicketRepository.save(ticket);

        if (ticket.getTicketType() != TicketType.ROOM_ONLY
                && ticket.getTicketDetails() != null
                && !ticket.getTicketDetails().isEmpty()) {
            processInventoryOnReturn(ticket);
        }

        notifyUser(ticket.getRequester().getUserId(),
                "Trả đồ thành công",
                String.format("Teacher đã xác nhận nhận lại đồ tại phòng %s.",
                        ticket.getFromRoom().getRoomName()),
                "TICKET_RETURNED");

        RentTicketResponse newState = rentTicketMapper.toResponse(ticket);
        auditLogService.logAction("CONFIRM_RETURN", "RENT_TICKET", ticketId, oldState, newState);

        if (ticket.getTicketType() != TicketType.ROOM_ONLY) {
            List<RentTicketDetail> problematic = rentTicketDetailRepository
                    .findProblematicDetails(ticketId);
            if (!problematic.isEmpty()) {
                String requesterName = (ticket.getRequester() != null)
                        ? UserNameResolver.resolve(ticket.getRequester())
                        : "Người dùng không xác định";

                notifyAllAdmins(
                        "Cảnh báo: Vấn đề hóa chất khi trả",
                        String.format("Phiếu mượn của [%s] tại phòng %s có %d hóa chất bị vấn đề khi trả (hỏng/mất/thiếu). Vui lòng kiểm tra.",
                                requesterName,
                                ticket.getFromRoom().getRoomName(),
                                problematic.size()),
                        "RETURN_ISSUE_ALERT");

                notifyUser(ticket.getRequester().getUserId(),
                        "Ghi nhận vấn đề khi trả đồ",
                        String.format("Phiếu mượn tại phòng %s đã được xác nhận trả, nhưng có %d hóa chất được ghi nhận là hỏng/mất/thiếu. Vui lòng liên hệ quản lý để biết thêm chi tiết.",
                                ticket.getFromRoom().getRoomName(),
                                problematic.size()),
                        "RETURN_ISSUE_REQUESTER");

                auditLogService.logAction("RETURN_ISSUE", "RENT_TICKET", ticketId,
                        newState, buildProblematicPayload(ticket, problematic));
            }
        }

        return newState;
    }

    // ── ADMIN ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<RentTicketSummaryResponse> getPendingTicketsForAdmin() {
        return rentTicketMapper.toSummaryList(
                rentTicketRepository.findAllByStatusOrderByCreatedAtDesc(TicketStatus.PENDING_ADMIN));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RentTicketSummaryResponse> getAllTicketsForAdmin(
            UUID roomId, TicketStatus status, TicketType ticketType,
            UUID requesterId, Pageable pageable) {
        Specification<RentTicket> spec = RentTicketSpecification.filter(
                roomId, status, ticketType, requesterId);
        return rentTicketRepository.findAll(spec, pageable)
                .map(rentTicketMapper::toSummaryResponse);
    }

    @Override
    @Transactional
    public RentTicketResponse adminApprove(UUID ticketId, UUID adminId, RentTicketApproveRequest request) {
        RentTicket ticket = findTicketById(ticketId);

        if (ticket.getStatus() != TicketStatus.PENDING_ADMIN) {
            throw new BadRequestException(
                    "Phiếu phải ở trạng thái PENDING_ADMIN! Trạng thái hiện tại: " + ticket.getStatus());
        }

        if (Boolean.FALSE.equals(request.getApproved())) {
            validateRejectedReason(request.getRejectedReason());
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng!"));

        RentTicketResponse oldState = rentTicketMapper.toResponse(ticket);

        if (Boolean.FALSE.equals(request.getApproved())) {
            ticket.setStatus(TicketStatus.REJECTED);
            ticket.setRejectedReason(request.getRejectedReason());
            ticket.setRejectedAt(LocalDateTime.now());
            ticket.setRejectedBy(admin);

            notifyUser(ticket.getRequester().getUserId(),
                    "Phiếu mượn bị từ chối",
                    String.format("Admin đã từ chối phiếu mượn tại phòng %s. Lý do: %s",
                            ticket.getFromRoom().getRoomName(), request.getRejectedReason()),
                    "TICKET_REJECTED_BY_ADMIN");
        } else {
            ticket.setStatus(TicketStatus.APPROVED);
            ticket.setAdminApprovedBy(admin);
            ticket.setAdminApprovedAt(LocalDateTime.now());

            lockInventory(ticket);

            notifyUser(ticket.getRequester().getUserId(),
                    "Phiếu mượn được duyệt",
                    String.format("Admin đã duyệt phiếu mượn tại phòng %s. Bạn có thể đến nhận đồ.",
                            ticket.getFromRoom().getRoomName()),
                    "TICKET_APPROVED");
        }

        rentTicketRepository.save(ticket);
        RentTicketResponse newState = rentTicketMapper.toResponse(ticket);
        auditLogService.logAction("ADMIN_APPROVE", "RENT_TICKET", ticketId, oldState, newState);
        return newState;
    }

    // ── REQUESTER ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<RentTicketSummaryResponse> getMyTickets(UUID requesterId, Pageable pageable) {
        return rentTicketRepository
                .findAllByRequester_UserIdOrderByCreatedAtDesc(requesterId, pageable)
                .map(rentTicketMapper::toSummaryResponse);
    }

    @Override
    @Transactional
    public RentTicketResponse requestReturn(UUID ticketId, UUID requesterId, ReturnTicketRequest request) {
        RentTicket ticket = findTicketById(ticketId);

        if (!ticket.getRequester().getUserId().equals(requesterId)) {
            throw new BadRequestException("Bạn không có quyền thực hiện trả phiếu này!");
        }
        if (ticket.getStatus() != TicketStatus.BORROWED) {
            throw new BadRequestException("Chỉ có thể yêu cầu trả khi phiếu đang ở trạng thái BORROWED!");
        }

        if (ticket.getTicketType() != TicketType.ROOM_ONLY) {
            List<RentTicketDetail> details = ticket.getTicketDetails();
            if (details == null || details.isEmpty()) {
                throw new BadRequestException("Phiếu có hóa chất nhưng không có chi tiết nào!");
            }
            if (request.getItems() == null || request.getItems().isEmpty()) {
                throw new BadRequestException(
                        "Vui lòng cung cấp trạng thái trả cho tất cả " + details.size() + " hóa chất!");
            }

            Set<UUID> submittedDetailIds = request.getItems().stream()
                    .map(ReturnTicketDetailRequest::getDetailId)
                    .collect(Collectors.toSet());

            Set<UUID> missing = details.stream()
                    .map(RentTicketDetail::getDetailId)
                    .filter(id -> !submittedDetailIds.contains(id))
                    .collect(Collectors.toSet());

            if (!missing.isEmpty()) {
                throw new BadRequestException(
                        "Thiếu trạng thái trả cho " + missing.size()
                                + " hóa chất. Vui lòng điền đầy đủ trước khi yêu cầu trả!");
            }
        }

        RentTicketResponse oldState = rentTicketMapper.toResponse(ticket);

        if (ticket.getTicketType() != TicketType.ROOM_ONLY
                && request.getItems() != null && !request.getItems().isEmpty()) {
            updateDetailReturnStatus(ticket, request.getItems());
        }

        ticket.setStatus(TicketStatus.PENDING_RETURN);
        rentTicketRepository.save(ticket);

        notifyRoomStaff(ticket.getFromRoom(),
                "Yêu cầu trả đồ",
                String.format("[%s] đã yêu cầu trả đồ tại phòng %s. Vui lòng xác nhận.",
                        UserNameResolver.resolve(ticket.getRequester()),
                        ticket.getFromRoom().getRoomName()),
                "TICKET_PENDING_RETURN");

        RentTicketResponse newState = rentTicketMapper.toResponse(ticket);
        auditLogService.logAction("REQUEST_RETURN", "RENT_TICKET", ticketId, oldState, newState);
        return newState;
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────

    private RentTicket findTicketById(UUID ticketId) {
        return rentTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu mượn!"));
    }

    private void validateRejectedReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new BadRequestException("Vui lòng nhập lý do từ chối!");
        }
    }

    private boolean hasNoDetails(RentTicket ticket) {
        return ticket.getTicketDetails() == null || ticket.getTicketDetails().isEmpty();
    }

    private List<RentTicketDetail> buildTicketDetails(
            RentTicket ticket, List<RentTicketDetailRequest> items, Room room) {

        List<UUID> itemIds = items.stream()
                .map(RentTicketDetailRequest::getItemId)
                .collect(Collectors.toList());

        Map<UUID, Item> itemMap = itemRepository.findAllById(itemIds).stream()
                .collect(Collectors.toMap(Item::getItemId, Function.identity()));

        Map<UUID, RoomInventory> inventoryMap =
                roomInventoryRepository.findAllByRoom_RoomIdAndItem_ItemIdIn(
                                room.getRoomId(), itemIds).stream()
                        .collect(Collectors.toMap(
                                inv -> inv.getItem().getItemId(),
                                Function.identity()));

        List<RentTicketDetail> details = new ArrayList<>();

        for (RentTicketDetailRequest dto : items) {
            Item item = itemMap.get(dto.getItemId());
            if (item == null) {
                throw new ResourceNotFoundException(
                        "Không tìm thấy hóa chất với ID: " + dto.getItemId());
            }
            if (item.isDeleted()) {
                throw new BadRequestException(
                        "Hóa chất [" + item.getName() + "] đã bị xóa khỏi hệ thống!");
            }

            RoomInventory inventory = inventoryMap.get(dto.getItemId());
            if (inventory == null) {
                throw new BadRequestException(
                        "Hóa chất [" + item.getName() + "] không có trong phòng ["
                                + room.getRoomName() + "]!");
            }

            BigDecimal available = inventory.getTotalQuantity()
                    .subtract(inventory.getLockedQuantity());
            if (dto.getQuantityBorrowed().compareTo(available) > 0) {
                throw new BadRequestException(
                        "Số lượng mượn [" + item.getName() + "] vượt quá tồn kho khả dụng! "
                                + "Khả dụng: " + available + " " + item.getUnit());
            }

            details.add(RentTicketDetail.builder()
                    .ticket(ticket)
                    .item(item)
                    .quantityBorrowed(dto.getQuantityBorrowed())
                    .returnStatus(ReturnStatus.NOT_RETURNED)
                    .build());
        }

        return details;
    }

    private Map<UUID, RoomInventory> loadInventoryMap(RentTicket ticket) {
        List<UUID> itemIds = ticket.getTicketDetails().stream()
                .map(d -> d.getItem().getItemId())
                .collect(Collectors.toList());

        return roomInventoryRepository.findAllByRoom_RoomIdAndItem_ItemIdIn(
                        ticket.getFromRoom().getRoomId(), itemIds).stream()
                .collect(Collectors.toMap(
                        inv -> inv.getItem().getItemId(),
                        Function.identity()));
    }

    private void lockInventory(RentTicket ticket) {
        if (hasNoDetails(ticket)) return;

        Map<UUID, RoomInventory> inventoryMap = loadInventoryMap(ticket);

        for (RentTicketDetail detail : ticket.getTicketDetails()) {
            RoomInventory inventory = inventoryMap.get(detail.getItem().getItemId());
            if (inventory == null) {
                throw new ResourceNotFoundException(
                        "Không tìm thấy tồn kho cho hóa chất: " + detail.getItem().getName());
            }

            BigDecimal available = inventory.getTotalQuantity()
                    .subtract(inventory.getLockedQuantity());
            if (detail.getQuantityBorrowed().compareTo(available) > 0) {
                throw new BadRequestException(
                        "Hóa chất [" + detail.getItem().getName() + "] không đủ số lượng khả dụng "
                                + "để duyệt! Khả dụng: " + available);
            }

            inventory.setLockedQuantity(
                    inventory.getLockedQuantity().add(detail.getQuantityBorrowed()));
        }

        roomInventoryRepository.saveAll(inventoryMap.values());
    }


    private void processInventoryOnReturn(RentTicket ticket) {
        if (hasNoDetails(ticket)) return;

        Map<UUID, RoomInventory> inventoryMap = loadInventoryMap(ticket);

        for (RentTicketDetail detail : ticket.getTicketDetails()) {
            RoomInventory inventory = inventoryMap.get(detail.getItem().getItemId());
            if (inventory == null) continue;

            BigDecimal borrowed = detail.getQuantityBorrowed();
            BigDecimal returned = detail.getQuantityReturned() != null
                    ? detail.getQuantityReturned() : BigDecimal.ZERO;

            // Unlock
            BigDecimal newLocked = inventory.getLockedQuantity().subtract(borrowed);
            inventory.setLockedQuantity(
                    newLocked.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newLocked);

            // Trừ số đã tiêu thụ/mất/hỏng
            BigDecimal consumed = borrowed.subtract(returned);
            BigDecimal newTotal = inventory.getTotalQuantity().subtract(consumed);
            inventory.setTotalQuantity(
                    newTotal.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newTotal);
        }

        roomInventoryRepository.saveAll(inventoryMap.values());
    }

    private void updateDetailReturnStatus(RentTicket ticket, List<ReturnTicketDetailRequest> items) {
        List<UUID> detailIds = items.stream()
                .map(ReturnTicketDetailRequest::getDetailId)
                .collect(Collectors.toList());

        Map<UUID, RentTicketDetail> detailMap = rentTicketDetailRepository.findAllById(detailIds)
                .stream()
                .collect(Collectors.toMap(RentTicketDetail::getDetailId, Function.identity()));

        for (ReturnTicketDetailRequest dto : items) {
            RentTicketDetail detail = detailMap.get(dto.getDetailId());
            if (detail == null) {
                throw new ResourceNotFoundException(
                        "Không tìm thấy chi tiết phiếu với ID: " + dto.getDetailId());
            }
            if (!detail.getTicket().getTicketId().equals(ticket.getTicketId())) {
                throw new BadRequestException("Chi tiết phiếu không thuộc phiếu mượn này!");
            }

            ReturnStatus returnStatus;
            try {
                returnStatus = ReturnStatus.valueOf(dto.getReturnStatus());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Trạng thái trả không hợp lệ: " + dto.getReturnStatus());
            }

            if ((returnStatus == ReturnStatus.DAMAGED
                    || returnStatus == ReturnStatus.LOST
                    || returnStatus == ReturnStatus.PARTIAL)
                    && !StringUtils.hasText(dto.getReturnNote())) {
                throw new BadRequestException(
                        "Vui lòng nhập ghi chú cho hóa chất ["
                                + detail.getItem().getName() + "] bị " + returnStatus + "!");
            }

            detail.setQuantityReturned(dto.getQuantityReturned());
            detail.setReturnStatus(returnStatus);
            detail.setReturnNote(dto.getReturnNote());
        }

        rentTicketDetailRepository.saveAll(detailMap.values());
    }

    private ProblematicReturnPayload buildProblematicPayload(
            RentTicket ticket, List<RentTicketDetail> problematic) {

        List<ProblematicReturnPayload.IssueItem> issues = problematic.stream()
                .map(d -> ProblematicReturnPayload.IssueItem.builder()
                        .itemName(d.getItem().getName())
                        .quantityBorrowed(d.getQuantityBorrowed())
                        .quantityReturned(d.getQuantityReturned())
                        .returnStatus(d.getReturnStatus().name())
                        .returnNote(d.getReturnNote())
                        .build())
                .collect(Collectors.toList());

        return ProblematicReturnPayload.builder()
                .ticketId(ticket.getTicketId())
                .roomName(ticket.getFromRoom().getRoomName())
                .requesterName(UserNameResolver.resolve(ticket.getRequester()))
                .issues(issues)
                .build();
    }

    private void notifyAllAdmins(String title, String message, String type) {
        userRepository.findAllByRole("ADMIN")
                .forEach(admin -> eventPublisher.publishEvent(
                        new NotificationEvent(admin.getUserId(), title, message, type)));
    }

    private void notifyRoomStaff(Room room, String title, String message, String type) {
        if (room.getStaffAssignments() == null || room.getStaffAssignments().isEmpty()) return;
        room.getStaffAssignments().forEach(sa ->
                eventPublisher.publishEvent(new NotificationEvent(
                        sa.getUser().getUserId(), title, message, type)));
    }

    private void notifyUser(UUID userId, String title, String message, String type) {
        eventPublisher.publishEvent(new NotificationEvent(userId, title, message, type));
    }
}