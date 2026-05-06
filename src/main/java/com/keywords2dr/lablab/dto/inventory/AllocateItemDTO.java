package com.keywords2dr.lablab.dto.inventory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class AllocateItemDTO {

    @NotNull(message = "ID Vật tư/Hóa chất không được để trống!")
    private UUID itemId;

    @Min(value = 0, message = "Số lượng hộp/chai không được âm!")
    private Integer packageCount = 0;

    @DecimalMin(value = "0.0", inclusive = true, message = "Số lượng phân bổ lẻ không được âm!")
    private BigDecimal quantity = BigDecimal.ZERO;
}