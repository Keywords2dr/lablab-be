package com.keywords2dr.lablab.dto.user;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UserResponseDTO {

    private UUID userId;
    private String username;
    private String role;
    private Boolean isActive;

    // Thông tin từ Profile
    private String fullName;
    private String email;
    private String phoneNumber;
    private String faculty;
    private String department;
    private String major;
    private String avatar;

    // Thêm thông tin hữu ích cho Admin
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}