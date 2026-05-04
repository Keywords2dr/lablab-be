package com.keywords2dr.lablab.dto.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoomRequestDTO {

    @NotBlank(message = "Tên phòng không được để trống!")
    @Size(min = 3, max = 100, message = "Tên phòng phải từ 3 đến 100 ký tự!")
    private String roomName;

    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự!")
    private String description;

    private Boolean isActive = true;
}