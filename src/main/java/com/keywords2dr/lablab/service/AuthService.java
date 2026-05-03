package com.keywords2dr.lablab.service;

import com.keywords2dr.lablab.dto.auth.ForgotPasswordRequest;
import com.keywords2dr.lablab.dto.auth.LoginRequest;
import com.keywords2dr.lablab.dto.auth.LoginResponse;
import com.keywords2dr.lablab.dto.auth.ResetPasswordRequest;
import com.keywords2dr.lablab.dto.auth.VerifyResetCodeRequest;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void verifyResetCode(VerifyResetCodeRequest request);

    void resetPassword(ResetPasswordRequest request);
}