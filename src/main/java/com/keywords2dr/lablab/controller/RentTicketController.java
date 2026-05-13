package com.keywords2dr.lablab.controller;

import com.keywords2dr.lablab.dto.ticket.*;
import com.keywords2dr.lablab.repository.UserRepository;
import com.keywords2dr.lablab.service.RentTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class RentTicketController {

    private final RentTicketService ticketService;
    private final UserRepository userRepository; // Đã bổ sung UserRepository

    // ĐÃ FIX: Lấy username từ token -> Quét DB -> Trả về UUID thật
    private UUID getUid(Authentication auth) {
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user: " + username))
                .getUserId();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ResponseEntity<RentTicketResponse> create(@Valid @RequestBody RentTicketCreateRequest req, Authentication auth) {
        return ResponseEntity.ok(ticketService.createTicket(req, getUid(auth)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ResponseEntity<Page<RentTicketSummaryResponse>> getMy(Pageable p, Authentication auth) {
        return ResponseEntity.ok(ticketService.getMyTickets(getUid(auth), p));
    }

    @PutMapping("/{id}/teacher-approve")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<RentTicketResponse> teacherApprove(@PathVariable UUID id, @Valid @RequestBody RentTicketApproveRequest req, Authentication auth) {
        return ResponseEntity.ok(ticketService.teacherApprove(id, getUid(auth), req));
    }

    @PutMapping("/{id}/admin-approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RentTicketResponse> adminApprove(@PathVariable UUID id, @Valid @RequestBody RentTicketApproveRequest req, Authentication auth) {
        return ResponseEntity.ok(ticketService.adminApprove(id, getUid(auth), req));
    }

    @PutMapping("/{id}/start-borrow")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ResponseEntity<Void> start(@PathVariable UUID id, Authentication auth) {
        ticketService.startBorrowing(id, getUid(auth));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/request-return")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ResponseEntity<Void> reqReturn(@PathVariable UUID id, Authentication auth) {
        ticketService.requestReturn(id, getUid(auth));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/teacher-confirm-return")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<RentTicketResponse> confirmReturn(@PathVariable UUID id, @Valid @RequestBody ReturnTicketRequest req, Authentication auth) {
        return ResponseEntity.ok(ticketService.teacherConfirmReturn(id, getUid(auth), req));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RentTicketResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ticketService.getTicketDetail(id));
    }
}