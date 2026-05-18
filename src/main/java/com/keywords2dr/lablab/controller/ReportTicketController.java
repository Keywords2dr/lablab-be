package com.keywords2dr.lablab.controller;

import com.keywords2dr.lablab.dto.report.ReportTicketRequest;
import com.keywords2dr.lablab.dto.report.ReportTicketResponse;
import com.keywords2dr.lablab.entity.enums.ReportType;
import com.keywords2dr.lablab.service.ReportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportTicketController {

    private final ReportTicketService reportTicketService;

    // USER gửi phiếu báo cáo
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportTicketResponse> createReport(
            @Valid @RequestBody ReportTicketRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reportTicketService.createReport(request));
    }

    // USER xem lịch sử phiếu của mình
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ReportTicketResponse>> getMyReports(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(reportTicketService.getMyReports(pageable));
    }

    // ADMIN xem tất cả + filter
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ReportTicketResponse>> getAllReports(
            @RequestParam(required = false) ReportType reportType,
            @RequestParam(required = false) UUID roomId,
            @RequestParam(required = false) UUID itemId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(reportTicketService.getAllReports(reportType, roomId, itemId, pageable));
    }
}