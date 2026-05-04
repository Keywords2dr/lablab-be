package com.keywords2dr.lablab.controller;

import com.keywords2dr.lablab.entity.AuditLog;
import com.keywords2dr.lablab.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page; // IMPORT PAGE
import org.springframework.data.domain.PageRequest; // IMPORT
import org.springframework.data.domain.Pageable; // IMPORT
import org.springframework.data.domain.Sort; // IMPORT
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLog>> getLogs(
                                                   @RequestParam(required = false) String role,
                                                   @RequestParam(required = false) String module,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size,
                                                   @RequestParam(defaultValue = "createdAt") String sortBy,
                                                   @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AuditLog> logs = auditLogService.getFilteredLogs(role, module, pageable);

        return ResponseEntity.ok(logs);
    }
}