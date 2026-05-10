package com.keywords2dr.lablab.controller;

import com.keywords2dr.lablab.dto.room.AssignStaffRequestDTO;
import com.keywords2dr.lablab.dto.room.RoomStaffResponseDTO;
import com.keywords2dr.lablab.dto.user.UserResponseDTO;
import com.keywords2dr.lablab.service.RoomStaffAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms/{roomId}/staff")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class RoomStaffAssignmentController {

    private final RoomStaffAssignmentService roomStaffAssignmentService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RoomStaffResponseDTO>> getStaffByRoom(@PathVariable UUID roomId) {
        return ResponseEntity.ok(roomStaffAssignmentService.getStaffByRoom(roomId));
    }

    @GetMapping("/assignable")
    public ResponseEntity<List<UserResponseDTO>> getAssignableTeachers(
            @PathVariable UUID roomId,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(roomStaffAssignmentService.getAssignableTeachers(roomId, keyword));
    }

    @PostMapping
    public ResponseEntity<RoomStaffResponseDTO> assignStaff(
            @PathVariable UUID roomId,
            @Valid @RequestBody AssignStaffRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roomStaffAssignmentService.assignStaff(roomId, request));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, String>> removeStaff(
            @PathVariable UUID roomId,
            @PathVariable UUID userId) {
        roomStaffAssignmentService.removeStaff(roomId, userId);
        return ResponseEntity.ok(Map.of("message", "Đã gỡ quyền quản lý của Giáo viên khỏi phòng Lab."));
    }
}