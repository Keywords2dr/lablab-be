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

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    private record OtpEntry(String code, Instant expiresAt) {
        boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    }

    private final Map<String, OtpEntry> otpStorage = new ConcurrentHashMap<>();

    private final SecureRandom secureRandom = new SecureRandom();

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

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));

        otpStorage.put(request.getEmail(), new OtpEntry(code, Instant.now().plusSeconds(300)));
        emailService.sendOtp(request.getEmail(), code);
    }

    @Override
    public void verifyResetCode(VerifyResetCodeRequest request) {
        validateOtp(request.getEmail(), request.getCode());
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không trùng khớp!");
        }

        validateOtp(request.getEmail(), request.getCode());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Lỗi hệ thống: Không tìm thấy User!"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        otpStorage.remove(request.getEmail());
    }

    // Kiểm tra OTP hợp lệ và chưa hết hạn — dùng chung cho verify + reset
    private void validateOtp(String email, String code) {
        OtpEntry entry = otpStorage.get(email);
        if (entry == null || entry.isExpired()) {
            otpStorage.remove(email); // dọn entry hết hạn
            throw new RuntimeException("Mã xác thực không đúng hoặc đã hết hạn!");
        }
        if (!entry.code().equals(code)) {
            throw new RuntimeException("Mã xác thực không đúng hoặc đã hết hạn!");
        }
    }
}