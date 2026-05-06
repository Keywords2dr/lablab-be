package com.keywords2dr.lablab.mapper;

import com.keywords2dr.lablab.dto.room.RoomManagerResponseDTO;
import com.keywords2dr.lablab.entity.RoomManager;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoomManagerMapper {

    @Mapping(source = "room.roomId",     target = "roomId")
    @Mapping(source = "user.userId",     target = "userId")
    @Mapping(source = "user.username",   target = "username")
    @Mapping(source = "user.role",       target = "role")
    RoomManagerResponseDTO toResponse(RoomManager entity);
}