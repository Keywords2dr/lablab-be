package com.keywords2dr.lablab.service.impl;

import com.keywords2dr.lablab.dto.room.RoomRequestDTO;
import com.keywords2dr.lablab.dto.room.RoomResponseDTO;
import com.keywords2dr.lablab.dto.room.RoomStatsDTO;
import com.keywords2dr.lablab.entity.Room;
import com.keywords2dr.lablab.exception.ConflictException;
import com.keywords2dr.lablab.exception.ResourceNotFoundException;
import com.keywords2dr.lablab.mapper.RoomMapper;
import com.keywords2dr.lablab.repository.RoomRepository;
import com.keywords2dr.lablab.repository.UserRepository;
import com.keywords2dr.lablab.repository.specification.RoomSpecification;
import com.keywords2dr.lablab.repository.specification.UserSpecification;
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
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public RoomResponseDTO createRoom(RoomRequestDTO request) {
        if (roomRepository.existsByRoomNameIgnoreCase(request.getRoomName())) {
            throw new ConflictException("Tên phòng [" + request.getRoomName() + "] đã tồn tại!");
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Phòng Lab!"));

        if (!room.getRoomName().equalsIgnoreCase(request.getRoomName()) &&
                roomRepository.existsByRoomNameIgnoreCase(request.getRoomName())) {
            throw new ConflictException("Tên phòng [" + request.getRoomName() + "] đã tồn tại!");
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Phòng Lab!"));

        if (Boolean.valueOf(isActive).equals(room.getIsActive())) {
            return isActive ? "Phòng đã đang hoạt động." : "Phòng đã tạm ngưng.";
        }

        RoomResponseDTO oldState = roomMapper.toResponse(room);
        room.setIsActive(isActive);
        roomRepository.save(room);

        RoomResponseDTO newState = roomMapper.toResponse(room);
        auditLogService.logAction(isActive ? "ACTIVATE" : "DEACTIVATE", "ROOM", id, oldState, newState);
        return isActive ? "Đã mở lại hoạt động cho phòng." : "Đã tạm ngưng hoạt động phòng.";
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponseDTO getRoomById(UUID id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Phòng Lab!"));
        return roomMapper.toResponse(room);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoomResponseDTO> getRooms(String keyword, Boolean isActive, Pageable pageable) {
        Specification<Room> spec = RoomSpecification.filter(keyword, isActive);
        return roomRepository.findAll(spec, pageable).map(roomMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomStatsDTO getRoomStats() {
        long totalRooms            = roomRepository.countByIsActive(true);
        long roomsWithoutStaff     = roomRepository.countRoomsWithoutStaff();
        long totalActiveTeachers   = userRepository.count(UserSpecification.filter("TEACHER", null, true));
        return new RoomStatsDTO(totalRooms, roomsWithoutStaff, totalActiveTeachers);
    }
}