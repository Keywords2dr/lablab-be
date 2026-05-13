package com.keywords2dr.lablab.service.impl;

import com.keywords2dr.lablab.dto.user.*;
import com.keywords2dr.lablab.entity.Profile;
import com.keywords2dr.lablab.entity.User;
import com.keywords2dr.lablab.exception.BadRequestException;
import com.keywords2dr.lablab.exception.ConflictException;
import com.keywords2dr.lablab.exception.ResourceNotFoundException;
import com.keywords2dr.lablab.mapper.UserMapper;
import com.keywords2dr.lablab.repository.UserRepository;
import com.keywords2dr.lablab.repository.specification.UserSpecification;
import com.keywords2dr.lablab.security.SecurityUtils;
import com.keywords2dr.lablab.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponseDTO createUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username '" + request.getUsername() + "' đã tồn tại!");
        }

        User user = User.builder()
                .username(request.getUsername().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole().toUpperCase())
                .isActive(true)
                .build();

        Profile profile = Profile.builder()
                .user(user)
                .fullName(request.getFullName())
                .email(request.getUsername().toLowerCase() + "@lablab.local")
                .phoneNumber("")
                .faculty(request.getFaculty())
                .major(request.getMajor())
                .department(request.getDepartment())
                .build();

        user.setProfile(profile);

        try {
            User savedUser = userRepository.save(user);
            return userMapper.toUserResponse(savedUser);
        } catch (Exception e) {
            log.error("Lỗi lưu User: {}", e.getMessage());
            throw new RuntimeException("Lỗi hệ thống khi tạo người dùng.");
        }
    }

    @Override
    @Transactional
    public void resetPassword(UUID userId, String newPassword) {
        User user = findUserById(userId);

        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new BadRequestException("Mật khẩu mới không được để trống!");
        }

        // Lưu mật khẩu mới do Admin nhập từ giao diện
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(user);
        log.info("Admin đã đổi mật khẩu cho user: {}", user.getUsername());
    }

    @Override
    @Transactional
    public UserResponseDTO updateUser(UUID userId, UserUpdateRequest request) {
        User user = findUserById(userId);
        if (user.getProfile() == null) {
            user.setProfile(Profile.builder().user(user).build());
        }
        Profile profile = user.getProfile();

        if (request.getRole() != null) user.setRole(request.getRole().toUpperCase());
        if (request.getIsActive() != null) user.setIsActive(request.getIsActive());
        if (request.getFullName() != null) profile.setFullName(request.getFullName());
        if (request.getEmail() != null) profile.setEmail(request.getEmail());
        if (request.getFaculty() != null) profile.setFaculty(request.getFaculty());
        if (request.getMajor() != null) profile.setMajor(request.getMajor());
        if (request.getDepartment() != null) profile.setDepartment(request.getDepartment());

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void toggleUserActive(UUID userId) {
        User user = findUserById(userId);
        user.setIsActive(!user.getIsActive());
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> getUsers(String role, String keyword, Boolean isActive, Pageable pageable) {
        Specification<User> spec = UserSpecification.filter(role, keyword, isActive);
        return userRepository.findAll(spec, pageable).map(userMapper::toUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(UUID userId) {
        return userMapper.toUserResponse(findUserById(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile() {
        return userMapper.toProfileResponse(findUserById(currentUserIdOrThrow()));
    }

    @Override
    @Transactional
    public ProfileResponse updateMyProfile(UpdateProfileRequest request) {
        User user = findUserById(currentUserIdOrThrow());
        if (user.getProfile() == null) {
            user.setProfile(Profile.builder().user(user).build());
        }
        userMapper.updateProfileFromRequest(request, user.getProfile());
        return userMapper.toProfileResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = findUserById(currentUserIdOrThrow());
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu cũ không chính xác!");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUsers(String role, String keyword, Boolean isActive) {
        return userRepository.count(UserSpecification.filter(role, keyword, isActive));
    }

    private User findUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng!"));
    }

    private UUID currentUserIdOrThrow() {
        UUID id = SecurityUtils.getCurrentUserId();
        if (id == null) throw new BadRequestException("Phiên làm việc hết hạn!");
        return id;
    }
}