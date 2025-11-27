package com.example.demo.controller;

import com.example.demo.dto.auth.JwtAuthenticationResponse;
import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.dto.auth.RegisterRequest;
import com.example.demo.dto.auth.ResetPasswordRequest;
import com.example.demo.dto.common.ApiResponse;
import com.example.demo.exception.InvalidTokenException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.TokenExpiredException;
import com.example.demo.exception.UsernameNotFoundException;
import com.example.demo.model.RefreshToken;
import com.example.demo.model.User;
import com.example.demo.security.CookieUtils;
import com.example.demo.service.AuthService;
import com.example.demo.service.PasswordResetTokenService;
import com.example.demo.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private CookieUtils cookieUtils;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private PasswordResetTokenService passwordResetTokenService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<JwtAuthenticationResponse>> register(
            @Valid @RequestBody RegisterRequest registerRequest, HttpServletResponse response) {

        log.info("Registering new user: {}", registerRequest.getUsername());

        JwtAuthenticationResponse authResponse = authService.register(registerRequest);
        cookieUtils.createRefreshTokenCookie(response, authResponse.getRefreshToken());
        ApiResponse<JwtAuthenticationResponse> apiResponse = ApiResponse.created(
                authResponse,"User registered and logged in successfully");
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtAuthenticationResponse>> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) throws IOException {

        log.info("User attempt for user: {}", loginRequest.getUsername());

        JwtAuthenticationResponse authResponse = authService.login(loginRequest);
        cookieUtils.createRefreshTokenCookie(response, authResponse.getRefreshToken());
        ApiResponse<JwtAuthenticationResponse> apiResponse = ApiResponse.success(authResponse, "Login successfully");
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request, HttpServletResponse response) {
        log.info("Logout request");

        String refreshToken = cookieUtils.getRefreshTokenFromCookie(request)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh Token not found in cookies"));
        authService.logout(refreshToken);

        cookieUtils.deleteRefreshTokenCookie(response);

        ApiResponse<String> apiResponse = ApiResponse.success("Logout Successfully", "User logout successfully");

        return ResponseEntity.ok(apiResponse);

    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<JwtAuthenticationResponse>> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        log.info("Refresh token request");

        String refreshToken = cookieUtils.getRefreshTokenFromCookie(request)
                .orElse(null);

        if(refreshToken == null) {
            log.warn("No refresh token found in cookies");
            throw new RuntimeException("Refresh token not found in cookies. Please login again.");
        }
        JwtAuthenticationResponse jwtAuthenticationResponse = authService.refreshToken(refreshToken);

        cookieUtils.createRefreshTokenCookie(response, jwtAuthenticationResponse.getRefreshToken());

        ApiResponse<JwtAuthenticationResponse> apiResponse = ApiResponse.success(jwtAuthenticationResponse, "Token refreshed successfully");

        return ResponseEntity.ok(apiResponse);
    }
    //enter email
    @PostMapping("/password/forgot")
    public ResponseEntity<ApiResponse<?>> forgotPasswordRequest(@RequestParam String email) {
        try {
            authService.sendResetPasswordRequest(email);
            log.info("Forget password request successfully for {}", email);

            ApiResponse<String> response = ApiResponse.success(null, "Password reset email has been sent successfully");
            return ResponseEntity.ok(response);

        } catch (UsernameNotFoundException e) {
            log.error("Email not found: {}", email);

            ApiResponse<String> response = ApiResponse.error("User not found", HttpStatus.BAD_REQUEST);

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        }
    }

    //redirect to page change password
    @GetMapping("/password/validate")
    public ResponseEntity<ApiResponse<?>> validateResetPasswordToken(@RequestParam String token) {
        passwordResetTokenService.validateToken(token);
        ApiResponse<String> apiResponse = ApiResponse.success(token, "Token is valid. Proceed to reset password.");
        return ResponseEntity.ok(apiResponse);

    }

    //fill new password
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<?>> resetPassword(@RequestBody ResetPasswordRequest request) {
        passwordResetTokenService.validateToken(request.getToken());
        passwordResetTokenService.resetPassword(request.getToken(), request.getPassword());
        ApiResponse<String> apiResponse = ApiResponse.success(null, "Password changed successfully");
        return ResponseEntity.ok(apiResponse);
    }

}
