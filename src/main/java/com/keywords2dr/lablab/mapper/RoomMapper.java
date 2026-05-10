package com.keywords2dr.lablab.mapper;

import com.keywords2dr.lablab.dto.room.RoomRequestDTO;
import com.keywords2dr.lablab.dto.room.RoomResponseDTO;
import com.keywords2dr.lablab.entity.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RoomMapper {

    @Mapping(target = "roomId", ignore = true)
    @Mapping(target = "inventories", ignore = true)
    @Mapping(target = "staffAssignments", ignore = true)
    Room toEntity(RoomRequestDTO request);

    @Mapping(
            target = "staffCount",
            expression = "java(room.getStaffAssignments() != null ? room.getStaffAssignments().size() : 0)"
    )
    RoomResponseDTO toResponse(Room room);

    @Mapping(target = "staffCount", source = "staffCount")
    @Mapping(target = "roomId",     source = "room.roomId")
    @Mapping(target = "roomName",   source = "room.roomName")
    @Mapping(target = "description",source = "room.description")
    @Mapping(target = "isActive",   source = "room.isActive")
    RoomResponseDTO toResponseWithCount(Room room, int staffCount);

    @Mapping(target = "roomId", ignore = true)
    @Mapping(target = "inventories", ignore = true)
    @Mapping(target = "staffAssignments", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    void updateEntityFromDto(RoomRequestDTO dto, @MappingTarget Room entity);
}