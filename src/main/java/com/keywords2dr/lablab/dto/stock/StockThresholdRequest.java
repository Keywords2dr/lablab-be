package com.keywords2dr.lablab.dto.stock;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class StockThresholdRequest {

    @NotNull(message = "ID hóa chất không được để trống!")
    private UUID itemId;

    @NotNull(message = "Ngưỡng tối thiểu không được để trống!")
    @DecimalMin(value = "0.01", message = "Ngưỡng tối thiểu phải lớn hơn 0!")
    private BigDecimal minQuantity;

    private String note;
}