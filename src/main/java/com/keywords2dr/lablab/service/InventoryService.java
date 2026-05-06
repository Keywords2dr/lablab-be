package com.keywords2dr.lablab.service;

import com.keywords2dr.lablab.dto.chemical.GlobalInventoryResponse;
import com.keywords2dr.lablab.dto.inventory.AllocateRequestDTO;
import com.keywords2dr.lablab.dto.inventory.RevokeRequestDTO;
import com.keywords2dr.lablab.dto.inventory.RoomInventoryResponseDTO;

import java.util.List;
import java.util.UUID;

public interface InventoryService {
    List<GlobalInventoryResponse> getGlobalChemicalInventory();

    void allocateItems(AllocateRequestDTO request);

    List<RoomInventoryResponseDTO> getInventoryByRoom(UUID roomId);

    void revokeItems(RevokeRequestDTO request);
}