package com.keywords2dr.lablab.mapper;

import com.keywords2dr.lablab.dto.room.RoomStaffResponseDTO;
import com.keywords2dr.lablab.entity.RoomStaffAssignment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoomStaffAssignmentMapper {

    @Mapping(source = "room.roomId", target = "roomId")
    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.role", target = "role")
    RoomStaffResponseDTO toResponse(RoomStaffAssignment entity);
}