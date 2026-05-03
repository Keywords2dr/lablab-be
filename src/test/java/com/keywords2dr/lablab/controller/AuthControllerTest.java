package com.keywords2dr.lablab.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keywords2dr.lablab.dto.auth.ForgotPasswordRequest;
import com.keywords2dr.lablab.dto.auth.LoginRequest;
import com.keywords2dr.lablab.dto.auth.LoginResponse;
import com.keywords2dr.lablab.dto.auth.ResetPasswordRequest;
import com.keywords2dr.lablab.dto.auth.VerifyResetCodeRequest;
import com.keywords2dr.lablab.exception.GlobalExceptionHandler;
import com.keywords2dr.lablab.security.JwtAuthFilter;
import com.keywords2dr.lablab.security.JwtUtil;
import com.keywords2dr.lablab.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Tắt màng lọc Security
@Import(GlobalExceptionHandler.class) // Nhập bộ bắt lỗi
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    // Giả lập 2 Bean của Security để tránh lỗi Context
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;
    @MockitoBean
    private JwtUtil jwtUtil;

    // =========================================================================
    // 1. TEST API LOGIN (/api/auth/login)
    // =========================================================================

    @Test
    @DisplayName("Login - Happy Path: Đăng nhập thành công")
    void testLogin_HappyPath() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        LoginResponse mockResponse = LoginResponse.builder()
                .accessToken("mock-token")
                .username("admin")
                .role("ADMIN")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-token"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("Login - Sad Path: Sai tài khoản hoặc mật khẩu")
    void testLogin_SadPath_WrongCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrongpass");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Sai tài khoản hoặc mật khẩu!"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Sai tài khoản hoặc mật khẩu!"));
    }

    @Test
    @DisplayName("Login - Bad Path: Thiếu dữ liệu (Validation)")
    void testLogin_BadPath_MissingData() throws Exception {
        LoginRequest request = new LoginRequest();
        // Cố tình để trống Username và Password

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Bị chặn ở Controller bởi @Valid
    }

    // =========================================================================
    // 2. TEST API FORGOT PASSWORD (/api/auth/forgot-password)
    // =========================================================================

    @Test
    @DisplayName("Forgot Password - Happy Path: Email hợp lệ")
    void testForgotPassword_HappyPath() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("user@qnu.edu.vn");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Mã xác thực đã được gửi về Email!"));
    }

    @Test
    @DisplayName("Forgot Password - Sad Path: Email không tồn tại")
    void testForgotPassword_SadPath_EmailNotFound() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("notfound@qnu.edu.vn");

        doThrow(new RuntimeException("Email chưa được đăng ký trong hệ thống!"))
                .when(authService).forgotPassword(any(ForgotPasswordRequest.class));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email chưa được đăng ký trong hệ thống!"));
    }

    @Test
    @DisplayName("Forgot Password - Bad Path: Sai định dạng Email")
    void testForgotPassword_BadPath_InvalidEmail() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("khong-phai-email"); // Sai định dạng

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // 3. TEST API VERIFY RESET CODE (/api/auth/verify-reset-code)
    // =========================================================================

    @Test
    @DisplayName("Verify Code - Happy Path: Mã hợp lệ")
    void testVerifyCode_HappyPath() throws Exception {
        VerifyResetCodeRequest request = new VerifyResetCodeRequest();
        request.setEmail("user@qnu.edu.vn");
        request.setCode("123456");

        mockMvc.perform(post("/api/auth/verify-reset-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Mã xác thực hợp lệ!"));
    }

    // =========================================================================
    // 4. TEST API RESET PASSWORD (/api/auth/reset-password)
    // =========================================================================

    @Test
    @DisplayName("Reset Password - Happy Path: Đổi mật khẩu thành công")
    void testResetPassword_HappyPath() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("user@qnu.edu.vn");
        request.setCode("123456");
        request.setNewPassword("newpass123");
        request.setConfirmPassword("newpass123");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đổi mật khẩu thành công!"));
    }

    @Test
    @DisplayName("Reset Password - Sad Path: Mật khẩu xác nhận không khớp")
    void testResetPassword_SadPath_PasswordsNotMatch() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("user@qnu.edu.vn");
        request.setCode("123456");
        request.setNewPassword("newpass123");
        request.setConfirmPassword("khacnhauhoantoan");

        doThrow(new RuntimeException("Mật khẩu xác nhận không trùng khớp!"))
                .when(authService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mật khẩu xác nhận không trùng khớp!"));
    }
}