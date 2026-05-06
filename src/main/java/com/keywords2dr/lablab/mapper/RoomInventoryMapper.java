package com.keywords2dr.lablab.mapper;

import com.keywords2dr.lablab.dto.inventory.RoomInventoryResponseDTO;
import com.keywords2dr.lablab.entity.RoomInventory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoomInventoryMapper {

    @Mapping(source = "item.itemId",       target = "itemId")
    @Mapping(source = "item.itemCode",     target = "itemCode")
    @Mapping(source = "item.name",         target = "itemName")
    @Mapping(source = "item.categoryType", target = "categoryType")
    @Mapping(source = "item.unit",         target = "unit")
    @Mapping(
            target = "availableQuantity",
            expression = "java(entity.getTotalQuantity().subtract(entity.getLockedQuantity()))"
    )
    RoomInventoryResponseDTO toResponse(RoomInventory entity);
}