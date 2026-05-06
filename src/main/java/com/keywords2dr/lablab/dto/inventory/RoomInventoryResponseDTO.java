package com.keywords2dr.lablab.dto.inventory;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class RoomInventoryResponseDTO {
    private UUID inventoryId;
    private UUID itemId;
    private String itemCode;
    private String itemName;
    private String categoryType;
    private String unit;
    private BigDecimal totalQuantity;
    private BigDecimal lockedQuantity;
    private BigDecimal availableQuantity;
    private String note;
}