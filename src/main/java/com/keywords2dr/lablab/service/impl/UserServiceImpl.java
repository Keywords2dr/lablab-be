package com.keywords2dr.lablab.service.impl;

import com.keywords2dr.lablab.dto.user.*;
import com.keywords2dr.lablab.entity.User;
import com.keywords2dr.lablab.mapper.UserMapper;
import com.keywords2dr.lablab.repository.UserRepository;
import com.keywords2dr.lablab.security.SecurityUtils;
import com.keywords2dr.lablab.service.UserService;
import lombok.RequiredArgsConstructor;
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
    public ProfileResponse getMyProfile() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("Không xác định được người dùng hiện tại (Token không hợp lệ)!");
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dữ liệu người dùng!"));

        return userMapper.toProfileResponse(user);
    }

    @Override
    @Transactional
    public ProfileResponse updateMyProfile(UpdateProfileRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("Không xác định được người dùng hiện tại!");
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dữ liệu người dùng!"));

        if (request.getEmail() != null
                && userRepository.existsByEmailAndUserIdNot(request.getEmail(), currentUserId)) {
            throw new RuntimeException("Email [" + request.getEmail() + "] đã được sử dụng bởi tài khoản khác!");
        }

        if (request.getPhoneNumber() != null
                && userRepository.existsByPhoneNumberAndUserIdNot(request.getPhoneNumber(), currentUserId)) {
            throw new RuntimeException("Số điện thoại [" + request.getPhoneNumber() + "] đã được sử dụng bởi tài khoản khác!");
        }

        userMapper.updateProfileFromRequest(request, user.getProfile());
        userRepository.save(user);

        return userMapper.toProfileResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("Không xác định được người dùng hiện tại!");
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu hiện tại không chính xác!");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không trùng khớp!");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu mới không được giống mật khẩu hiện tại!");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}