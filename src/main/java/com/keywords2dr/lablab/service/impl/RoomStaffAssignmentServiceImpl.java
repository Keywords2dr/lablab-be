package com.keywords2dr.lablab.service.impl;

import com.keywords2dr.lablab.dto.room.AssignStaffRequestDTO;
import com.keywords2dr.lablab.dto.room.RoomStaffResponseDTO;
import com.keywords2dr.lablab.entity.Room;
import com.keywords2dr.lablab.entity.RoomStaffAssignment;
import com.keywords2dr.lablab.entity.User;
import com.keywords2dr.lablab.event.NotificationEvent;
import com.keywords2dr.lablab.mapper.RoomStaffAssignmentMapper;
import com.keywords2dr.lablab.repository.RoomStaffAssignmentRepository;
import com.keywords2dr.lablab.repository.RoomRepository;
import com.keywords2dr.lablab.repository.UserRepository;
import com.keywords2dr.lablab.service.AuditLogService;
import com.keywords2dr.lablab.service.RoomStaffAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomStaffAssignmentServiceImpl implements RoomStaffAssignmentService {

    private final RoomStaffAssignmentRepository roomStaffAssignmentRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;
    private final RoomStaffAssignmentMapper roomStaffAssignmentMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RoomStaffResponseDTO> getStaffByRoom(UUID roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new RuntimeException("Không tìm thấy Phòng Lab!");
        }

        return roomStaffAssignmentRepository.findAllByRoom_RoomId(roomId)
                .stream()
                .map(roomStaffAssignmentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public RoomStaffResponseDTO assignStaff(UUID roomId, AssignStaffRequestDTO request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Phòng Lab!"));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Người dùng!"));

        if (!"TEACHER".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("Chỉ có Giáo viên (TEACHER) mới được phép quản lý phòng Lab!");
        }

        if (roomStaffAssignmentRepository.existsByRoom_RoomIdAndUser_UserId(roomId, user.getUserId())) {
            throw new RuntimeException("Giáo viên này đã được phân công quản lý phòng này rồi!");
        }

        RoomStaffAssignment roomStaffAssignment = RoomStaffAssignment.builder()
                .room(room)
                .user(user)
                .build();

        RoomStaffResponseDTO responseDTO = roomStaffAssignmentMapper.toResponse(roomStaffAssignmentRepository.save(roomStaffAssignment));
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
    public void removeStaff(UUID roomId, UUID userId) {
        RoomStaffAssignment roomStaffAssignment = roomStaffAssignmentRepository.findByRoom_RoomIdAndUser_UserId(roomId, userId)
                .orElseThrow(() -> new RuntimeException("Giáo viên này hiện không quản lý phòng Lab này!"));

        RoomStaffResponseDTO oldState = roomStaffAssignmentMapper.toResponse(roomStaffAssignment);
        String roomName = roomStaffAssignment.getRoom().getRoomName();

        roomStaffAssignmentRepository.delete(roomStaffAssignment);
        auditLogService.logAction("REMOVE_MANAGER", "ROOM", roomId, oldState, null);

        eventPublisher.publishEvent(new NotificationEvent(
                userId,
                "Thu hồi quyền Quản lý",
                String.format("Admin đã thu hồi quyền quản lý của bạn tại phòng Lab: %s", roomName),
                "ROOM_REMOVE"
        ));
    }
}