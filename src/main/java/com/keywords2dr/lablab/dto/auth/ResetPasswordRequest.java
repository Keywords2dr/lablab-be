package com.keywords2dr.lablab.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Thiếu Email!")
    @Email(message = "Email không hợp lệ!")
    private String email;

    @NotBlank(message = "Thiếu mã xác thực!")
    private String code;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới!")
    @Size(min = 6, message = "Mật khẩu mới phải từ 6 ký tự")
    private String newPassword;

    @NotBlank(message = "Vui lòng xác nhận lại mật khẩu mới")
    private String confirmPassword;
}