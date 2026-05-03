package com.keywords2dr.lablab.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendOtp(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[LabLab] Mã xác thực khôi phục mật khẩu");
        message.setText("Mã OTP của bạn là: " + code + ". Mã có hiệu lực trong 5 phút.");
        mailSender.send(message);
    }
}