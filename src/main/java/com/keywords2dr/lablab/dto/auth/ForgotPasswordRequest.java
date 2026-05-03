package com.keywords2dr.lablab.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "Vui lòng nhập Email!")
    @Email(message = "Email không hợp lệ!")
    private String email;
}