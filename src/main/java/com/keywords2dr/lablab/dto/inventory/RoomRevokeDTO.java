package com.keywords2dr.lablab.dto.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class RoomRevokeDTO {
    @NotNull(message = "Vui lòng chọn Phòng Lab để thu hồi!")
    private UUID roomId;

    @NotEmpty(message = "Danh sách vật tư cần thu hồi không được rỗng!")
    @Valid
    private List<RevokeItemDTO> items;
}