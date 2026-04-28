package com.fullstack.backend.controller;

import com.fullstack.backend.dto.ApiMessageResponse;
import com.fullstack.backend.dto.AuthRequest;
import com.fullstack.backend.dto.AuthResponse;
import com.fullstack.backend.dto.ForgotPasswordOtpRequest;
import com.fullstack.backend.dto.RegisterRequest;
import com.fullstack.backend.dto.ResetPasswordRequest;
import com.fullstack.backend.dto.VerifyOtpRequest;
import com.fullstack.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        return userService.login(request);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordOtpRequest request) {
        userService.requestPasswordResetOtp(request);
        return ResponseEntity.ok(Map.of(
                "message", "OTP sent to your email. Please check your inbox."
        ));
    }

    @PostMapping("/verify-otp")
    public ApiMessageResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return userService.verifyPasswordResetOtp(request);
    }

    @PostMapping("/reset-password")
    public ApiMessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return userService.resetPasswordWithOtp(request);
    }
}
