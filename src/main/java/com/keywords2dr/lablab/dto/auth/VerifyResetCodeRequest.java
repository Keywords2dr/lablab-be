package com.keywords2dr.lablab.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyResetCodeRequest {
    @NotBlank(message = "Thiếu Email!")
    @Email(message = "Email không hợp lệ!")
    private String email;

    @NotBlank(message = "Vui lòng nhập mã xác thực!")
    private String code;
}