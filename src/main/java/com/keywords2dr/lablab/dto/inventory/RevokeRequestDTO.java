package com.keywords2dr.lablab.dto.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RevokeRequestDTO {
    @NotEmpty(message = "Danh sách thu hồi không được rỗng!")
    @Valid
    private List<RoomRevokeDTO> revocations;

    @Pattern(regexp = "^[a-zA-Z0-9À-ỹ\\s.,\\-_]*$", message = "Lý do chứa ký tự không hợp lệ!")
    @Size(max = 255, message = "Lý do thu hồi không được dài quá 255 ký tự!")
    private String note;
}