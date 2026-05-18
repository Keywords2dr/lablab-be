package com.keywords2dr.lablab.service;

import com.keywords2dr.lablab.dto.report.ReportTicketRequest;
import com.keywords2dr.lablab.dto.report.ReportTicketResponse;
import com.keywords2dr.lablab.entity.enums.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReportTicketService {

    // User gửi phiếu
    ReportTicketResponse createReport(ReportTicketRequest request);

    // User xem lịch sử của mình
    Page<ReportTicketResponse> getMyReports(Pageable pageable);

    // Admin xem tất cả + filter
    Page<ReportTicketResponse> getAllReports(ReportType reportType, UUID roomId, UUID itemId, Pageable pageable);
}