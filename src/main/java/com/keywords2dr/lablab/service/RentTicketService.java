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

    /** Tạo phiếu mượn mới (Student hoặc Teacher) */
    RentTicketResponse createTicket(UUID requesterId, RentTicketCreateRequest request);

    /** Hủy phiếu — chỉ người tạo mới được hủy, chỉ khi còn PENDING_OWNER hoặc PENDING_ADMIN */
    void cancelTicket(UUID ticketId, UUID requesterId);

    /** Xem chi tiết 1 phiếu */
    RentTicketResponse getTicketById(UUID ticketId);

    // ── TEACHER ───────────────────────────────────────────────────────────────

    /** Lấy danh sách phiếu đang chờ Teacher duyệt (phòng mình quản lý) */
    List<RentTicketSummaryResponse> getPendingTicketsForTeacher(UUID teacherId);

    /** Lấy toàn bộ phiếu của phòng mình (tất cả trạng thái), có phân trang */
    Page<RentTicketSummaryResponse> getAllTicketsForTeacher(UUID teacherId, Pageable pageable);

    /** Teacher duyệt hoặc từ chối phiếu */
    RentTicketResponse teacherApprove(UUID ticketId, UUID teacherId, RentTicketApproveRequest request);

    /** FIX #10: Teacher xác nhận đã bàn giao đồ (APPROVED → BORROWED) */
    RentTicketResponse activateTicket(UUID ticketId, UUID teacherId);

    /** Teacher xác nhận đã nhận lại đồ (PENDING_RETURN → RETURNED) */
    RentTicketResponse teacherConfirmReturn(UUID ticketId, UUID teacherId);

    // ── ADMIN ─────────────────────────────────────────────────────────────────

    /** Admin lấy danh sách phiếu đang chờ Admin duyệt */
    List<RentTicketSummaryResponse> getPendingTicketsForAdmin();

    /** Admin lấy tất cả phiếu, filter động theo roomId / status / ticketType */
    Page<RentTicketSummaryResponse> getAllTicketsForAdmin(
            UUID roomId,
            TicketStatus status,
            TicketType ticketType,
            UUID requesterId,
            Pageable pageable);

    /** Admin duyệt hoặc từ chối phiếu (sau Teacher) */
    RentTicketResponse adminApprove(UUID ticketId, UUID adminId, RentTicketApproveRequest request);

    // ── REQUESTER (Student / Teacher xem phiếu của mình) ─────────────────────

    /** Lấy danh sách phiếu của người tạo, có phân trang */
    Page<RentTicketSummaryResponse> getMyTickets(UUID requesterId, Pageable pageable);

    /** Người mượn yêu cầu trả (BORROWED → PENDING_RETURN) */
    RentTicketResponse requestReturn(UUID ticketId, UUID requesterId, ReturnTicketRequest request);
}