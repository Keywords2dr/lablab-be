package com.keywords2dr.lablab.service.impl;

import com.keywords2dr.lablab.dto.room.RoomRequestDTO;
import com.keywords2dr.lablab.dto.room.RoomResponseDTO;
import com.keywords2dr.lablab.entity.Room;
import com.keywords2dr.lablab.mapper.RoomMapper;
import com.keywords2dr.lablab.repository.RoomRepository;
import com.keywords2dr.lablab.repository.specification.RoomSpecification;
import com.keywords2dr.lablab.service.AuditLogService;
import com.keywords2dr.lablab.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;
    private final AuditLogService auditLogService; // Ghi vết hệ thống

    @Override
    @Transactional
    public RoomResponseDTO createRoom(RoomRequestDTO request) {
        if (roomRepository.existsByRoomNameIgnoreCase(request.getRoomName())) {
            throw new RuntimeException("Tên phòng [" + request.getRoomName() + "] đã tồn tại!");
        }

        Room room = roomMapper.toEntity(request);
        Room savedRoom = roomRepository.save(room);

        RoomResponseDTO responseDTO = roomMapper.toResponse(savedRoom);
        auditLogService.logAction("CREATE", "ROOM", savedRoom.getRoomId(), null, responseDTO);

        return responseDTO;
    }

    @Override
    @Transactional
    public RoomResponseDTO updateRoom(UUID id, RoomRequestDTO request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Phòng Lab!"));

        if (!room.getRoomName().equalsIgnoreCase(request.getRoomName()) &&
                roomRepository.existsByRoomNameIgnoreCase(request.getRoomName())) {
            throw new RuntimeException("Tên phòng [" + request.getRoomName() + "] đã tồn tại!");
        }

        RoomResponseDTO oldState = roomMapper.toResponse(room);

        roomMapper.updateEntityFromDto(request, room);
        Room updatedRoom = roomRepository.save(room);

        RoomResponseDTO newState = roomMapper.toResponse(updatedRoom);
        auditLogService.logAction("UPDATE", "ROOM", updatedRoom.getRoomId(), oldState, newState);

        return newState;
    }

    @Override
    @Transactional
    public String changeRoomStatus(UUID id, boolean isActive) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Phòng Lab!"));

        RoomResponseDTO oldState = roomMapper.toResponse(room);

        room.setIsActive(isActive);
        roomRepository.save(room);

        RoomResponseDTO newState = roomMapper.toResponse(room);
        String action = isActive ? "ACTIVATE" : "DEACTIVATE";
        auditLogService.logAction(action, "ROOM", id, oldState, newState);

        return isActive ? "Đã mở lại hoạt động cho phòng." : "Đã tạm ngưng hoạt động phòng.";
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoomResponseDTO> getRooms(String keyword, Boolean isActive, Pageable pageable) {
        Specification<Room> spec = RoomSpecification.filter(keyword, isActive);
        Page<Room> roomPage = roomRepository.findAll(spec, pageable);
        return roomPage.map(roomMapper::toResponse);
    }
}