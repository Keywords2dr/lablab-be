package com.keywords2dr.lablab.dto.auth;

import lombok.*;

@Data
@Builder
public class LoginResponse {
    private String accessToken;
    private String username;
    private String role;
}