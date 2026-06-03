package com.keywords2dr.lablab.controller;

import com.keywords2dr.lablab.dto.dashboard.RoomCurrentUsageDTO;
import com.keywords2dr.lablab.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/current-usage")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RoomCurrentUsageDTO>> getCurrentUsage() {
        return ResponseEntity.ok(dashboardService.getCurrentRoomUsage());
    }
}