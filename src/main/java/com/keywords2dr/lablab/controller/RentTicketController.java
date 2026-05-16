package com.keywords2dr.lablab.controller;

import com.keywords2dr.lablab.dto.ticket.*;
import com.keywords2dr.lablab.entity.enums.TicketStatus;
import com.keywords2dr.lablab.entity.enums.TicketType;
import com.keywords2dr.lablab.security.SecurityUtils;
import com.keywords2dr.lablab.service.RentTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class RentTicketController {

    private final RentTicketService ticketService;

    // ── REQUESTER ─────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ResponseEntity<RentTicketResponse> create(
            @Valid @RequestBody RentTicketCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.createTicket(SecurityUtils.getCurrentUserId(), req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ResponseEntity<Map<String, String>> cancel(@PathVariable UUID id) {
        ticketService.cancelTicket(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(Map.of("message", "Đã hủy phiếu mượn thành công."));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ResponseEntity<Page<RentTicketSummaryResponse>> getMyTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ticketService.getMyTickets(SecurityUtils.getCurrentUserId(), pageable));
    }

    @GetMapping("/my/filter") 
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ResponseEntity<Page<RentTicketSummaryResponse>> getMyTicketsByStatus(
            @RequestParam TicketStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                ticketService.getMyTicketsByStatus(SecurityUtils.getCurrentUserId(), status, pageable));
    }

    @PutMapping("/{id}/request-return")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ResponseEntity<RentTicketResponse> requestReturn(
            @PathVariable UUID id,
            @Valid @RequestBody ReturnTicketRequest req) {
        return ResponseEntity.ok(
                ticketService.requestReturn(id, SecurityUtils.getCurrentUserId(), req));
    }

    // ── SHARED ────────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RentTicketResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    // ── TEACHER ───────────────────────────────────────────────────────────────

    @GetMapping("/teacher/pending")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<RentTicketSummaryResponse>> teacherPending() {
        return ResponseEntity.ok(
                ticketService.getPendingTicketsForTeacher(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/teacher/all")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Page<RentTicketSummaryResponse>> teacherAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                ticketService.getAllTicketsForTeacher(SecurityUtils.getCurrentUserId(), pageable));
    }

    @GetMapping("/teacher/filter")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Page<RentTicketSummaryResponse>> teacherFilterByStatus(
            @RequestParam TicketStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                ticketService.getTicketsByStatusForTeacher(
                        SecurityUtils.getCurrentUserId(), status, pageable));
    }

    @GetMapping("/my/search")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ResponseEntity<Page<RentTicketSummaryResponse>> getMyTicketsFiltered(
            @RequestParam(required = false) List<TicketStatus> excludeStatus,
            @RequestParam(required = false) TicketType ticketType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                ticketService.getMyTicketsFiltered(
                        SecurityUtils.getCurrentUserId(), excludeStatus, ticketType, pageable));
    }

    @PutMapping("/{id}/teacher-approve")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<RentTicketResponse> teacherApprove(
            @PathVariable UUID id,
            @Valid @RequestBody RentTicketApproveRequest req) {
        return ResponseEntity.ok(
                ticketService.teacherApprove(id, SecurityUtils.getCurrentUserId(), req));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<RentTicketResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ticketService.activateTicket(id, SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/{id}/confirm-return")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<RentTicketResponse> confirmReturn(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ticketService.teacherConfirmReturn(id, SecurityUtils.getCurrentUserId()));
    }

    // ── ADMIN ─────────────────────────────────────────────────────────────────

    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RentTicketSummaryResponse>> adminPending() {
        return ResponseEntity.ok(ticketService.getPendingTicketsForAdmin());
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<RentTicketSummaryResponse>> adminAll(
            @RequestParam(required = false) UUID roomId,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketType ticketType,
            @RequestParam(required = false) UUID requesterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Pageable pageable = PageRequest.of(page, size,
                sortDir.equalsIgnoreCase("asc")
                        ? Sort.by("createdAt").ascending()
                        : Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                ticketService.getAllTicketsForAdmin(roomId, status, ticketType, requesterId, pageable));
    }

    @PutMapping("/{id}/admin-approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RentTicketResponse> adminApprove(
            @PathVariable UUID id,
            @Valid @RequestBody RentTicketApproveRequest req) {
        return ResponseEntity.ok(
                ticketService.adminApprove(id, SecurityUtils.getCurrentUserId(), req));
    }
}