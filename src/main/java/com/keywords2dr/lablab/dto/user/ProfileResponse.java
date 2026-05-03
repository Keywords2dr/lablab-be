package com.keywords2dr.lablab.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
    private String username;
    private String role;

    private String fullName;
    private String phoneNumber;
    private String email;
    private String faculty;
    private String major;
    private String department;
    private String avatar;
}