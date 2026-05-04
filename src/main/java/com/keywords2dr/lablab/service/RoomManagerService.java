package com.keywords2dr.lablab.service;

import com.keywords2dr.lablab.dto.room.RoomManagerRequestDTO;
import com.keywords2dr.lablab.dto.room.RoomManagerResponseDTO;

import java.util.List;
import java.util.UUID;

public interface RoomManagerService {
    List<RoomManagerResponseDTO> getManagersByRoom(UUID roomId);
    RoomManagerResponseDTO assignManager(UUID roomId, RoomManagerRequestDTO request);
    void removeManager(UUID roomId, UUID userId);
}