package com.keywords2dr.lablab.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AllocateItemDTO {

    @NotNull(message = "ID Vật tư/Hóa chất không được để trống!")
    private UUID itemId;

    @NotNull(message = "Số lượng chai/hộp không được để trống!")
    @Min(value = 1, message = "Số lượng chai/hộp phải ít nhất là 1!")
    private Integer packageCount;
}