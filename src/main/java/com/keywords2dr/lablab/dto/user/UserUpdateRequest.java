package com.keywords2dr.lablab.dto.user;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String username;
    private String role;
    private Boolean isActive;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String faculty;
    private String major;
    private String department;
}