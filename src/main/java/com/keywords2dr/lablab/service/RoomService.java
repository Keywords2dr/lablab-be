package com.keywords2dr.lablab.service;

import com.keywords2dr.lablab.dto.room.RoomRequestDTO;
import com.keywords2dr.lablab.dto.room.RoomResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RoomService {
    RoomResponseDTO createRoom(RoomRequestDTO request);
    RoomResponseDTO updateRoom(UUID id, RoomRequestDTO request);
    String changeRoomStatus(UUID id, boolean isActive);
    Page<RoomResponseDTO> getRooms(String keyword, Boolean isActive, Pageable pageable);
}