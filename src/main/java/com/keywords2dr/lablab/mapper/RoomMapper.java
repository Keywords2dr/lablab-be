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

    RoomResponseDTO toResponse(Room room);

    @Mapping(target = "roomId", ignore = true)
    @Mapping(target = "inventories", ignore = true)
    @Mapping(target = "staffAssignments", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    void updateEntityFromDto(RoomRequestDTO dto, @MappingTarget Room entity);
}