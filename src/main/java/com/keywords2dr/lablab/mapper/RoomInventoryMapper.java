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

    @Mapping(
            target = "chemicalFormula",
            expression = "java(entity.getItem() instanceof com.keywords2dr.lablab.entity.Chemical ? ((com.keywords2dr.lablab.entity.Chemical) entity.getItem()).getFormula() : null)"
    )
    RoomInventoryResponseDTO toResponse(RoomInventory entity);
}