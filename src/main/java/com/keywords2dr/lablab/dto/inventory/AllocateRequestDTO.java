package com.keywords2dr.lablab.dto.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AllocateRequestDTO {

    @NotEmpty(message = "Danh sách phân bổ không được rỗng!")
    @Valid
    private List<RoomAllocationDTO> allocations;

    @Pattern(regexp = "^[a-zA-Z0-9À-ỹ\\s.,\\-_]*$", message = "Ghi chú chứa ký tự không hợp lệ!")
    @Size(max = 255, message = "Ghi chú không được dài quá 255 ký tự!")
    private String note;
}