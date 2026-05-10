package com.keywords2dr.lablab.service.impl;

import com.keywords2dr.lablab.dto.room.AssignStaffRequestDTO;
import com.keywords2dr.lablab.dto.room.RoomStaffResponseDTO;
import com.keywords2dr.lablab.entity.Room;
import com.keywords2dr.lablab.entity.RoomStaffAssignment;
import com.keywords2dr.lablab.entity.User;
import com.keywords2dr.lablab.event.NotificationEvent;
import com.keywords2dr.lablab.exception.BadRequestException;
import com.keywords2dr.lablab.exception.ConflictException;
import com.keywords2dr.lablab.exception.ResourceNotFoundException;
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

    private static final String ROLE_TEACHER = "TEACHER";

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
            throw new ResourceNotFoundException("Không tìm thấy Phòng Lab!");
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Phòng Lab!"));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Người dùng!"));

        if (!ROLE_TEACHER.equalsIgnoreCase(user.getRole())) {
            throw new BadRequestException("Chỉ có Giáo viên (TEACHER) mới được phép quản lý phòng Lab!");
        }

        if (roomStaffAssignmentRepository.existsByRoom_RoomIdAndUser_UserId(roomId, user.getUserId())) {
            throw new ConflictException("Giáo viên này đã được phân công quản lý phòng này rồi!");
        }

        if (roomStaffAssignmentRepository.existsByUser_UserId(user.getUserId())) {
            throw new ConflictException(
                    "Giáo viên [" + user.getUsername() + "] đang quản lý một phòng Lab khác. " +
                            "Mỗi giáo viên chỉ được quản lý 1 phòng duy nhất!"
            );
        }

        RoomStaffAssignment assignment = RoomStaffAssignment.builder()
                .room(room)
                .user(user)
                .build();

        RoomStaffResponseDTO responseDTO = roomStaffAssignmentMapper.toResponse(
                roomStaffAssignmentRepository.save(assignment)
        );
        auditLogService.logAction("ASSIGN_STAFF", "ROOM_STAFF", roomId, null, responseDTO);

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
        RoomStaffAssignment assignment = roomStaffAssignmentRepository
                .findByRoom_RoomIdAndUser_UserId(roomId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Giáo viên này hiện không quản lý phòng Lab này!"));

        RoomStaffResponseDTO oldState = roomStaffAssignmentMapper.toResponse(assignment);
        String roomName = assignment.getRoom().getRoomName();

        roomStaffAssignmentRepository.delete(assignment);
        auditLogService.logAction("REMOVE_STAFF", "ROOM_STAFF", roomId, oldState, null);

        eventPublisher.publishEvent(new NotificationEvent(
                userId,
                "Thu hồi quyền Quản lý",
                String.format("Admin đã thu hồi quyền quản lý của bạn tại phòng Lab: %s", roomName),
                "ROOM_REMOVE"
        ));
    }
}