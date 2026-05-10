package com.keywords2dr.lablab.service;

import com.keywords2dr.lablab.dto.user.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    void changePassword(ChangePasswordRequest request);

    ProfileResponse getMyProfile();

    ProfileResponse updateMyProfile(UpdateProfileRequest request);

    Page<UserResponseDTO> getUsers(String role, String keyword, Boolean isActive, Pageable pageable);

    long countUsers(String role, String keyword, Boolean isActive);
}