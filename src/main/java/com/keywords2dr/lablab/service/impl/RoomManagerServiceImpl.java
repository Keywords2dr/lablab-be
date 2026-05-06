package com.keywords2dr.lablab.service.impl;

import com.keywords2dr.lablab.dto.room.RoomManagerRequestDTO;
import com.keywords2dr.lablab.dto.room.RoomManagerResponseDTO;
import com.keywords2dr.lablab.entity.Room;
import com.keywords2dr.lablab.entity.RoomManager;
import com.keywords2dr.lablab.entity.User;
import com.keywords2dr.lablab.event.NotificationEvent;
import com.keywords2dr.lablab.mapper.RoomManagerMapper;
import com.keywords2dr.lablab.repository.RoomManagerRepository;
import com.keywords2dr.lablab.repository.RoomRepository;
import com.keywords2dr.lablab.repository.UserRepository;
import com.keywords2dr.lablab.service.AuditLogService;
import com.keywords2dr.lablab.service.RoomManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomManagerServiceImpl implements RoomManagerService {

    private final RoomManagerRepository roomManagerRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;
    private final RoomManagerMapper roomManagerMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RoomManagerResponseDTO> getManagersByRoom(UUID roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new RuntimeException("Không tìm thấy Phòng Lab!");
        }

        return roomManagerRepository.findAllByRoom_RoomId(roomId)
                .stream()
                .map(roomManagerMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public RoomManagerResponseDTO assignManager(UUID roomId, RoomManagerRequestDTO request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Phòng Lab!"));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Người dùng!"));

        if (!"TEACHER".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("Chỉ có Giáo viên (TEACHER) mới được phép quản lý phòng Lab!");
        }

        if (roomManagerRepository.existsByRoom_RoomIdAndUser_UserId(roomId, user.getUserId())) {
            throw new RuntimeException("Giáo viên này đã được phân công quản lý phòng này rồi!");
        }

        RoomManager roomManager = RoomManager.builder()
                .room(room)
                .user(user)
                .build();

        RoomManagerResponseDTO responseDTO = roomManagerMapper.toResponse(roomManagerRepository.save(roomManager));
        auditLogService.logAction("ASSIGN_MANAGER", "ROOM", roomId, null, responseDTO);

        eventPublisher.publishEvent(new NotificationEvent(
                user.getUserId(),
                "Phân công Phòng Lab",
                String.format("Bạn vừa được phân công quản lý phòng Lab: %s", room.getRoomName()),
                "ROOM_ASSIGN"
        ));

        return responseDTO;
    }

    @Override
    @Transactional
    public void removeManager(UUID roomId, UUID userId) {
        RoomManager roomManager = roomManagerRepository.findByRoom_RoomIdAndUser_UserId(roomId, userId)
                .orElseThrow(() -> new RuntimeException("Giáo viên này hiện không quản lý phòng Lab này!"));

        RoomManagerResponseDTO oldState = roomManagerMapper.toResponse(roomManager);
        String roomName = roomManager.getRoom().getRoomName();

        roomManagerRepository.delete(roomManager);
        auditLogService.logAction("REMOVE_MANAGER", "ROOM", roomId, oldState, null);

        eventPublisher.publishEvent(new NotificationEvent(
                userId,
                "Thu hồi quyền Quản lý",
                String.format("Admin đã thu hồi quyền quản lý của bạn tại phòng Lab: %s", roomName),
                "ROOM_REMOVE"
        ));
    }
}