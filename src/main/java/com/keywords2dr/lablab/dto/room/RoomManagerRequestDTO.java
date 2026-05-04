package com.keywords2dr.lablab.dto.room;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class RoomManagerRequestDTO {
    @NotNull(message = "ID của Người dùng không được để trống!")
    private UUID userId;
}