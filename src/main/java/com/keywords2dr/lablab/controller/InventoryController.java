package com.keywords2dr.lablab.controller;

import com.keywords2dr.lablab.dto.chemical.GlobalInventoryResponse;
import com.keywords2dr.lablab.dto.inventory.AllocateRequestDTO;
import com.keywords2dr.lablab.dto.inventory.RevokeRequestDTO;
import com.keywords2dr.lablab.dto.inventory.RoomInventoryResponseDTO;
import com.keywords2dr.lablab.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/chemicals/global")
    public ResponseEntity<List<GlobalInventoryResponse>> getGlobalChemicalInventory() {
        return ResponseEntity.ok(inventoryService.getGlobalChemicalInventory());
    }

    @PostMapping("/allocate")
    public ResponseEntity<Map<String, String>> allocateItems(@Valid @RequestBody AllocateRequestDTO request) {
        inventoryService.allocateItems(request);
        return ResponseEntity.ok(Map.of("message", "Đã phân bổ vật tư thành công vào các phòng Lab."));
    }

    @GetMapping("/rooms/{roomId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RoomInventoryResponseDTO>> getInventoryByRoom(@PathVariable UUID roomId) {
        return ResponseEntity.ok(inventoryService.getInventoryByRoom(roomId));
    }

    @PostMapping("/revoke")
    public ResponseEntity<Map<String, String>> revokeItems(@Valid @RequestBody RevokeRequestDTO request) {
        inventoryService.revokeItems(request);
        return ResponseEntity.ok(Map.of("message", "Đã thu hồi vật tư khỏi phòng Lab thành công."));
    }
}