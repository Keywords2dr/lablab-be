package com.keywords2dr.lablab.service;

import com.keywords2dr.lablab.dto.room.AssignStaffRequestDTO;
import com.keywords2dr.lablab.dto.room.RoomStaffResponseDTO;
import com.keywords2dr.lablab.dto.user.UserResponseDTO;

import java.util.List;
import java.util.UUID;

public interface RoomStaffAssignmentService {
    List<RoomStaffResponseDTO> getStaffByRoom(UUID roomId);
    RoomStaffResponseDTO assignStaff(UUID roomId, AssignStaffRequestDTO request);
    void removeStaff(UUID roomId, UUID userId);
    List<UserResponseDTO> getAssignableTeachers(UUID roomId, String keyword);
}