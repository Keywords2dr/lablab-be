package com.keywords2dr.lablab.service;

import com.keywords2dr.lablab.dto.ticket.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RentTicketService {

    // REQUESTER (Sinh viên / Giáo viên tạo phiếu)
    RentTicketResponse createTicket(RentTicketCreateRequest request, UUID requesterId);
    Page<RentTicketSummaryResponse> getMyTickets(UUID requesterId, Pageable pageable);
    void cancelTicket(UUID ticketId, UUID requesterId);
    void startBorrowing(UUID ticketId, UUID requesterId);
    void requestReturn(UUID ticketId, UUID requesterId);

    // TEACHER (Duyệt bước 1 & Xác nhận trả)
    Page<RentTicketSummaryResponse> getTeacherPendingTickets(UUID teacherId, Pageable pageable);
    RentTicketResponse teacherApprove(UUID ticketId, UUID teacherId, RentTicketApproveRequest request);
    RentTicketResponse teacherConfirmReturn(UUID ticketId, UUID teacherId, ReturnTicketRequest request);

    // ADMIN (Duyệt bước 2 & Quản lý chung)
    Page<RentTicketSummaryResponse> getAdminPendingTickets(Pageable pageable);
    RentTicketResponse adminApprove(UUID ticketId, UUID adminId, RentTicketApproveRequest request);
    Page<RentTicketSummaryResponse> getAllTickets(UUID roomId, String status, String type, Pageable pageable);

    // CHI TIẾT
    RentTicketResponse getTicketDetail(UUID ticketId);
}