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
import com.keywords2dr.lablab.service.AuditLogService;
import com.keywords2dr.lablab.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public UserResponseDTO createUser(UserCreateRequest request) {
        String trimmedUsername = request.getUsername().trim();
        if (userRepository.existsByUsername(trimmedUsername)) {
            throw new ConflictException("Username '" + trimmedUsername + "' đã tồn tại!");
        }

        String cleanUserForEmail = trimmedUsername.toLowerCase().replaceAll("\\s+", "");
        String generatedEmail = cleanUserForEmail + "@gmail.com";

        User user = User.builder()
                .username(trimmedUsername)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole().toUpperCase())
                .isActive(true)
                .build();

        Profile profile = Profile.builder()
                .user(user)
                .fullName(request.getFullName() != null ? request.getFullName().trim() : "Chưa cập nhật")
                .email(generatedEmail)
                .faculty(request.getFaculty())
                .major(request.getMajor())
                .department(request.getDepartment())
                .build();

        user.setProfile(profile);

        User savedUser = userRepository.save(user);
        auditLogService.logAction("CREATE", "USER", savedUser.getUserId(), null, userToLogMap(savedUser));
        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponseDTO updateUser(UUID userId, UserUpdateRequest request) {
        User user = findUserById(userId);
        Map<String, Object> oldData = userToLogMap(user);

        if (user.getProfile() == null) {
            user.setProfile(Profile.builder().user(user).build());
        }
        Profile profile = user.getProfile();

        if (request.getRole() != null) user.setRole(request.getRole().toUpperCase());
        if (request.getIsActive() != null) user.setIsActive(request.getIsActive());
        if (request.getFullName() != null) profile.setFullName(request.getFullName().trim());

        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(profile.getEmail())) {
            if (userRepository.existsByEmailAndUserIdNot(request.getEmail(), userId)) {
                throw new ConflictException("Email '" + request.getEmail() + "' đã được sử dụng!");
            }
            profile.setEmail(request.getEmail().toLowerCase());
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            profile.setPhoneNumber(request.getPhoneNumber().trim());
        }

        if (request.getFaculty() != null) profile.setFaculty(request.getFaculty());
        if (request.getMajor() != null) profile.setMajor(request.getMajor());
        if (request.getDepartment() != null) profile.setDepartment(request.getDepartment());

        User updatedUser = userRepository.save(user);
        auditLogService.logAction("UPDATE", "USER", userId, oldData, userToLogMap(updatedUser));
        return userMapper.toUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public void toggleUserActive(UUID userId) {
        User user = findUserById(userId);
        boolean oldStatus = user.getIsActive();
        user.setIsActive(!user.getIsActive());
        userRepository.save(user);

        auditLogService.logAction("UPDATE_STATUS", "USER", userId,
                Map.of("isActive", oldStatus), Map.of("isActive", user.getIsActive()));
    }

    @Override
    @Transactional
    public void resetPassword(UUID userId, String newPassword) {
        User user = findUserById(userId);
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new BadRequestException("Mật khẩu mới không được để trống!");
        }
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(user);
        auditLogService.logAction("RESET_PASSWORD", "USER", userId, "Mật khẩu cũ", "Đã đặt lại mật khẩu mới");
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
        Map<String, Object> oldData = userToLogMap(user);

        if (user.getProfile() == null) {
            user.setProfile(Profile.builder().user(user).build());
        }
        userMapper.updateProfileFromRequest(request, user.getProfile());

        User savedUser = userRepository.save(user);
        auditLogService.logAction("UPDATE_MY_PROFILE", "USER", savedUser.getUserId(), oldData, userToLogMap(savedUser));

        return userMapper.toProfileResponse(savedUser);
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
        auditLogService.logAction("CHANGE_PASSWORD", "USER", user.getUserId(), "Mật khẩu cũ", "Đã đổi mật khẩu cá nhân");
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
    public long countUsers(String role, String keyword, Boolean isActive) {
        return userRepository.count(UserSpecification.filter(role, keyword, isActive));
    }

    // ==================== HELPER METHODS ====================

    private User findUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng!"));
    }

    private UUID currentUserIdOrThrow() {
        UUID id = SecurityUtils.getCurrentUserId();
        if (id == null) throw new BadRequestException("Phiên làm việc hết hạn!");
        return id;
    }

    private Map<String, Object> userToLogMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("username", user.getUsername());
        map.put("role", user.getRole());
        map.put("isActive", user.getIsActive());
        if (user.getProfile() != null) {
            map.put("fullName", user.getProfile().getFullName());
            map.put("email", user.getProfile().getEmail());
            map.put("faculty", user.getProfile().getFaculty());
            map.put("major", user.getProfile().getMajor());
            map.put("department", user.getProfile().getDepartment());
        }
        return map;
    }
}