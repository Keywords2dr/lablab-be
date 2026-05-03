package com.keywords2dr.lablab.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @NotBlank(message = "Họ tên không được để trống")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "Họ tên không được chứa ký số hoặc ký hiệu đặc biệt!")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại phải đúng 10 chữ số và bắt đầu bằng số 0!")
    private String phoneNumber;

    @Email(message = "Email không hợp lệ")
    private String email;

    private String avatar;
}