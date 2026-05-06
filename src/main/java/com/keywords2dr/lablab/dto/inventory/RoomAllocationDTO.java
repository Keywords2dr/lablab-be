package com.keywords2dr.lablab.dto.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class RoomAllocationDTO {
    @NotNull(message = "Vui lòng chọn Phòng đích đến!")
    private UUID roomId;

    @NotEmpty(message = "Danh sách hóa chất cho phòng này không được rỗng!")
    @Valid
    private List<AllocateItemDTO> items;
}