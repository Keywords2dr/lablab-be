package com.keywords2dr.lablab.controller;

import com.keywords2dr.lablab.dto.room.RoomManagerRequestDTO;
import com.keywords2dr.lablab.dto.room.RoomManagerResponseDTO;
import com.keywords2dr.lablab.service.RoomManagerService;
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
@RequestMapping("/api/rooms/{roomId}/managers")
@RequiredArgsConstructor
public class RoomManagerController {

    private final RoomManagerService roomManagerService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RoomManagerResponseDTO>> getManagersByRoom(@PathVariable UUID roomId) {
        return ResponseEntity.ok(roomManagerService.getManagersByRoom(roomId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomManagerResponseDTO> assignManager(
            @PathVariable UUID roomId,
            @Valid @RequestBody RoomManagerRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomManagerService.assignManager(roomId, request));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> removeManager(
            @PathVariable UUID roomId,
            @PathVariable UUID userId) {
        roomManagerService.removeManager(roomId, userId);
        return ResponseEntity.ok(Map.of("message", "Đã gỡ quyền quản lý của Giáo viên khỏi phòng Lab."));
    }
}