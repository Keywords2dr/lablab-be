package com.keywords2dr.lablab.service;

import com.keywords2dr.lablab.dto.user.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    void changePassword(ChangePasswordRequest request);

    ProfileResponse getMyProfile();

    ProfileResponse updateMyProfile(UpdateProfileRequest request);

    Page<UserResponseDTO> getUsers(String role, String keyword, Boolean isActive, Pageable pageable);

    long countUsers(String role, String keyword, Boolean isActive);

    UserResponseDTO createUser(UserCreateRequest request);

    UserResponseDTO getUserById(UUID userId);

    UserResponseDTO updateUser(UUID userId, UserUpdateRequest request);

    void toggleUserActive(UUID userId);

    void resetPassword(UUID userId, String newPassword);
}