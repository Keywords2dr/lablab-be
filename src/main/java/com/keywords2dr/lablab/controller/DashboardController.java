package com.keywords2dr.lablab.controller;

import com.keywords2dr.lablab.dto.dashboard.ActivityFeedItemDTO;
import com.keywords2dr.lablab.dto.dashboard.DailyTicketStatsDTO;
import com.keywords2dr.lablab.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    // ── Biểu đồ phiếu mượn 7 ngày ────────────────────────────────────────────
    @GetMapping("/api/tickets/admin/stats/weekly")
    public ResponseEntity<List<DailyTicketStatsDTO>> getWeeklyStats() {
        return ResponseEntity.ok(dashboardService.getWeeklyTicketStats());
    }

    // ── Activity Feed ─────────────────────────────────────────────────────────
    @GetMapping("/api/audit-logs/feed")
    public ResponseEntity<List<ActivityFeedItemDTO>> getActivityFeed(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(dashboardService.getActivityFeed(limit));
    }
}