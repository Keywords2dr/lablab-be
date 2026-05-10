package com.keywords2dr.lablab.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Pattern(regexp = "^[\\p{L} ]+$", message = "Họ tên không được chứa ký số hoặc ký hiệu đặc biệt!")
    private String fullName;

    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại phải đúng 10 chữ số và bắt đầu bằng số 0!")
    private String phoneNumber;

    @Email(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$",
            message = "Email không hợp lệ (Ví dụ hợp lệ: name@domain.com)"
    )
    private String email;

    private String avatar;
}