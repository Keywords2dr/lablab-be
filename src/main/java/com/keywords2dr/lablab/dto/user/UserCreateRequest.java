package com.keywords2dr.lablab.dto.user;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {

    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 50, message = "Username phải từ 3 đến 50 ký tự")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    @NotBlank(message = "Role không được để trống")
    private String role;

    private Boolean isActive = true;

    @Pattern(regexp = "^[\\p{L} ]+$", message = "Họ tên không được chứa ký số hoặc ký hiệu đặc biệt!")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$",
            message = "Email không hợp lệ (Ví dụ hợp lệ: name@domain.com)"
    )
    private String email;

    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại phải đúng 10 chữ số và bắt đầu bằng số 0!")
    private String phoneNumber;

    @Pattern(regexp = "^[\\p{L}\\p{N} ]+$", message = "Khoa không được chứa ký hiệu đặc biệt!")
    private String faculty;

    @Pattern(regexp = "^[\\p{L}\\p{N} ]+$", message = "Ngành không được chứa ký hiệu đặc biệt!")
    private String major;

    @Pattern(regexp = "^[\\p{L}\\p{N} ]+$", message = "Bộ môn không được chứa ký hiệu đặc biệt!")
    private String department;
}