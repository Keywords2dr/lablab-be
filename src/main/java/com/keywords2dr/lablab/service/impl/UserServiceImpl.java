package com.keywords2dr.lablab.service.impl;

import com.keywords2dr.lablab.dto.user.*;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile() {
        UUID currentUserId = currentUserIdOrThrow();
        User user = findUserById(currentUserId);
        return userMapper.toProfileResponse(user);
    }

    @Override
    @Transactional
    public ProfileResponse updateMyProfile(UpdateProfileRequest request) {
        UUID currentUserId = currentUserIdOrThrow();
        User user = findUserById(currentUserId);

        if (request.getEmail() != null
                && userRepository.existsByEmailAndUserIdNot(request.getEmail(), currentUserId)) {
            throw new ConflictException("Email [" + request.getEmail() + "] đã được sử dụng bởi tài khoản khác!");
        }
        if (request.getPhoneNumber() != null
                && userRepository.existsByPhoneNumberAndUserIdNot(request.getPhoneNumber(), currentUserId)) {
            throw new ConflictException("Số điện thoại [" + request.getPhoneNumber() + "] đã được sử dụng bởi tài khoản khác!");
        }

        userMapper.updateProfileFromRequest(request, user.getProfile());
        userRepository.save(user);
        return userMapper.toProfileResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        UUID currentUserId = currentUserIdOrThrow();
        User user = findUserById(currentUserId);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu hiện tại không chính xác!");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không trùng khớp!");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu mới không được giống mật khẩu hiện tại!");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
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
    public long countUsers(String role, String keyword, Boolean isActive) {
        Specification<User> spec = UserSpecification.filter(role, keyword, isActive);
        return userRepository.count(spec);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private UUID currentUserIdOrThrow() {
        UUID id = SecurityUtils.getCurrentUserId();
        if (id == null) {
            throw new BadRequestException("Không xác định được người dùng hiện tại (Token không hợp lệ)!");
        }
        return id;
    }

    private User findUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng!"));
    }
}