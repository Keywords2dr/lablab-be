package com.keywords2dr.lablab.controller;

import com.keywords2dr.lablab.dto.room.RoomRequestDTO;
import com.keywords2dr.lablab.dto.room.RoomResponseDTO;
import com.keywords2dr.lablab.dto.room.RoomStaffResponseDTO;
import com.keywords2dr.lablab.dto.room.RoomStatsDTO;
import com.keywords2dr.lablab.security.SecurityUtils;
import com.keywords2dr.lablab.service.RoomService;
import com.keywords2dr.lablab.service.RoomStaffAssignmentService;
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
@RequestMapping("/api/rooms")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final RoomStaffAssignmentService roomStaffAssignmentService; // Thêm service này

    @PostMapping
    public ResponseEntity<RoomResponseDTO> createRoom(@Valid @RequestBody RoomRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.createRoom(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomResponseDTO> updateRoom(
            @PathVariable UUID id,
            @Valid @RequestBody RoomRequestDTO request) {
        return ResponseEntity.ok(roomService.updateRoom(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> changeRoomStatus(
            @PathVariable UUID id,
            @RequestParam boolean isActive) {
        return ResponseEntity.ok(Map.of("message", roomService.changeRoomStatus(id, isActive)));
    }

    @GetMapping("/my-rooms")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<List<RoomStaffResponseDTO>> getMyManagedRooms() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(roomStaffAssignmentService.getRoomsByStaff(currentUserId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RoomResponseDTO> getRoomById(@PathVariable UUID id) {
        return ResponseEntity.ok(roomService.getRoomById(id));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<RoomResponseDTO>> getRooms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Sort sort = Sort.by(Sort.Order.desc("isActive"), Sort.Order.asc("roomName"));
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(roomService.getRooms(keyword, isActive, pageable));
    }

    @GetMapping("/stats")
    public ResponseEntity<RoomStatsDTO> getRoomStats() {
        return ResponseEntity.ok(roomService.getRoomStats());
    }
}