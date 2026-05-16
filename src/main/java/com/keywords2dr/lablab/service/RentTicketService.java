package com.keywords2dr.lablab.service;

import com.keywords2dr.lablab.dto.ticket.*;
import com.keywords2dr.lablab.entity.enums.TicketStatus;
import com.keywords2dr.lablab.entity.enums.TicketType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface RentTicketService {

    // ── CRUD ─────────────────────────────────────────────────────────────────

    RentTicketResponse createTicket(UUID requesterId, RentTicketCreateRequest request);
    void cancelTicket(UUID ticketId, UUID requesterId);
    RentTicketResponse getTicketById(UUID ticketId);

    // ── TEACHER ───────────────────────────────────────────────────────────────

    List<RentTicketSummaryResponse> getPendingTicketsForTeacher(UUID teacherId);
    Page<RentTicketSummaryResponse> getAllTicketsForTeacher(UUID teacherId, Pageable pageable);

    /** Lọc phiếu theo status cho Teacher (chỉ phiếu thuộc phòng mình quản lý) */
    Page<RentTicketSummaryResponse> getTicketsByStatusForTeacher(
            UUID teacherId, TicketStatus status, Pageable pageable);

    RentTicketResponse teacherApprove(UUID ticketId, UUID teacherId, RentTicketApproveRequest request);

    /** Teacher xác nhận đã bàn giao đồ (APPROVED → BORROWED) */
    RentTicketResponse activateTicket(UUID ticketId, UUID teacherId);

    /** Teacher xác nhận đã nhận lại đồ (PENDING_RETURN → RETURNED) */
    RentTicketResponse teacherConfirmReturn(UUID ticketId, UUID teacherId);

    // ── ADMIN ─────────────────────────────────────────────────────────────────

    List<RentTicketSummaryResponse> getPendingTicketsForAdmin();
    Page<RentTicketSummaryResponse> getAllTicketsForAdmin(
            UUID roomId,
            TicketStatus status,
            TicketType ticketType,
            UUID requesterId,
            Pageable pageable);

    RentTicketResponse adminApprove(UUID ticketId, UUID adminId, RentTicketApproveRequest request);

    // ── REQUESTER (Student / Teacher xem phiếu của mình) ─────────────────────

    Page<RentTicketSummaryResponse> getMyTickets(UUID requesterId, Pageable pageable);

    /** Lọc phiếu của chính mình theo status */
    Page<RentTicketSummaryResponse> getMyTicketsByStatus(
            UUID requesterId, TicketStatus status, Pageable pageable);

    Page<RentTicketSummaryResponse> getMyTicketsFiltered(
            UUID requesterId,
            List<TicketStatus> excludedStatuses,
            TicketType ticketType,
            Pageable pageable);


    /** Người mượn yêu cầu trả (BORROWED → PENDING_RETURN) */
    RentTicketResponse requestReturn(UUID ticketId, UUID requesterId, ReturnTicketRequest request);
}