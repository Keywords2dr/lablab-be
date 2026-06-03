package com.keywords2dr.lablab.service.impl;

import com.keywords2dr.lablab.dto.user.*;
import com.keywords2dr.lablab.entity.Profile;
import com.keywords2dr.lablab.entity.User;
import com.keywords2dr.lablab.entity.enums.AllowedRole;
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

        String role = AllowedRole.validateAndNormalize(request.getRole());

        String email = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmailAndUserIdNot(email, null) ||
                userRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("Email '" + email + "' đã được sử dụng!");
        }

        User user = User.builder()
                .username(trimmedUsername)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        Profile profile = Profile.builder()
                .user(user)
                .fullName(request.getFullName() != null ? request.getFullName().trim() : "Chưa cập nhật")
                .email(email)
                .phoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null)
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

        if (request.getRole() != null) {
            user.setRole(AllowedRole.validateAndNormalize(request.getRole()));
        }

        if (request.getFullName() != null) profile.setFullName(request.getFullName().trim());

        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(profile.getEmail())) {
            if (userRepository.existsByEmailAndUserIdNot(request.getEmail(), userId)) {
                throw new ConflictException("Email '" + request.getEmail() + "' đã được sử dụng!");
            }
            profile.setEmail(request.getEmail().toLowerCase().trim());
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            String phone = request.getPhoneNumber().trim();
            if (userRepository.existsByPhoneNumberAndUserIdNot(phone, userId)) {
                throw new ConflictException("Số điện thoại '" + phone + "' đã được sử dụng!");
            }
            profile.setPhoneNumber(phone);
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
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new BadRequestException("Mật khẩu mới không được để trống!");
        }
        if (newPassword.trim().length() < 6) {
            throw new BadRequestException("Mật khẩu phải có ít nhất 6 ký tự!");
        }

        User user = findUserById(userId);

        if (passwordEncoder.matches(newPassword.trim(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu mới không được trùng với mật khẩu hiện tại!");
        }

        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(user);

        auditLogService.logAction("RESET_PASSWORD", "USER", userId,
                Map.of("note", "Mật khẩu cũ"),
                Map.of("note", "Đã đặt lại mật khẩu mới"));
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile() {
        return userMapper.toProfileResponse(findUserById(currentUserIdOrThrow()));
    }

    @Override
    @Transactional
    public ProfileResponse updateMyProfile(UpdateProfileRequest request) {
        UUID currentUserId = currentUserIdOrThrow();
        User user = findUserById(currentUserId);
        Map<String, Object> oldData = userToLogMap(user);

        if (user.getProfile() == null) {
            user.setProfile(Profile.builder().user(user).build());
        }
        Profile profile = user.getProfile();

        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(profile.getEmail())) {
            if (userRepository.existsByEmailAndUserIdNot(request.getEmail(), currentUserId)) {
                throw new ConflictException("Email '" + request.getEmail() + "' đã được sử dụng!");
            }
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            String phone = request.getPhoneNumber().trim();
            if (userRepository.existsByPhoneNumberAndUserIdNot(phone, currentUserId)) {
                throw new ConflictException("Số điện thoại '" + phone + "' đã được sử dụng!");
            }
        }

        userMapper.updateProfileFromRequest(request, profile);

        User savedUser = userRepository.save(user);
        auditLogService.logAction("UPDATE_MY_PROFILE", "USER", savedUser.getUserId(), oldData, userToLogMap(savedUser));

        return userMapper.toProfileResponse(savedUser);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp!");
        }

        User user = findUserById(currentUserIdOrThrow());

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu cũ không chính xác!");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu mới không được trùng với mật khẩu cũ!");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        auditLogService.logAction("CHANGE_PASSWORD", "USER", user.getUserId(),
                Map.of("note", "Mật khẩu cũ"),
                Map.of("note", "Đã đổi mật khẩu cá nhân"));
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
            map.put("phoneNumber", user.getProfile().getPhoneNumber());
            map.put("faculty", user.getProfile().getFaculty());
            map.put("major", user.getProfile().getMajor());
            map.put("department", user.getProfile().getDepartment());
        }
        return map;
    }
}