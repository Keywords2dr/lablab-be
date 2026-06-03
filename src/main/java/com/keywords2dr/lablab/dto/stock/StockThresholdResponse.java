package com.keywords2dr.lablab.dto.stock;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class StockThresholdResponse {

    private UUID id;

    private UUID itemId;
    private String itemCode;
    private String itemName;
    private String unit;

    private BigDecimal minQuantity;
    private String note;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}