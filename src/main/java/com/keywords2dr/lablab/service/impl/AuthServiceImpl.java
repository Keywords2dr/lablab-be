package com.keywords2dr.lablab.service.impl;

import com.keywords2dr.lablab.dto.auth.*;
import com.keywords2dr.lablab.entity.User;
import com.keywords2dr.lablab.repository.UserRepository;
import com.keywords2dr.lablab.security.JwtUtil;
import com.keywords2dr.lablab.service.AuthService;
import com.keywords2dr.lablab.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Sai tài khoản hoặc mật khẩu!"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Sai tài khoản hoặc mật khẩu!");
        }

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new RuntimeException("Tài khoản bị khóa! Vui lòng liên hệ Admin.");
        }

        String token = jwtUtil.generateToken(user.getUsername());

        return LoginResponse.builder()
                .accessToken(token)
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email chưa được đăng ký trong hệ thống!"));

        String code = String.format("%06d", new Random().nextInt(999999));
        otpStorage.put(request.getEmail(), code);
        emailService.sendOtp(request.getEmail(), code);
    }

    @Override
    public void verifyResetCode(VerifyResetCodeRequest request) {
        String savedCode = otpStorage.get(request.getEmail());
        if (savedCode == null || !savedCode.equals(request.getCode())) {
            throw new RuntimeException("Mã xác thực không đúng hoặc đã hết hạn!");
        }
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không trùng khớp!");
        }

        String savedCode = otpStorage.get(request.getEmail());
        if (savedCode == null || !savedCode.equals(request.getCode())) {
            throw new RuntimeException("Mã xác thực không đúng hoặc đã hết hạn!");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Lỗi hệ thống: Không tìm thấy User!"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        otpStorage.remove(request.getEmail());
    }
}