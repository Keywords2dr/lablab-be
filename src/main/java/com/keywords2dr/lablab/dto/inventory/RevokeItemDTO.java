package com.keywords2dr.lablab.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class RevokeItemDTO {

    @NotNull(message = "ID Vật tư không được để trống!")
    private UUID itemId;

    @NotNull(message = "Số lượng chai/hộp thu hồi không được để trống!")
    @Min(value = 1, message = "Số lượng chai/hộp thu hồi phải ít nhất là 1!")
    private Integer packageCount;
}