package com.example.demo.controller;

import com.example.demo.dto.auth.JwtAuthenticationResponse;
import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.dto.auth.RegisterRequest;
import com.example.demo.dto.common.ApiResponse;
import com.example.demo.model.RefreshToken;
import com.example.demo.model.User;
import com.example.demo.security.CookieUtils;
import com.example.demo.service.AuthService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<ApiResponse<JwtAuthenticationResponse>> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {

        log.info("User attempt for user: {}", loginRequest.getUsername());

        JwtAuthenticationResponse authResponse = authService.login(loginRequest);
        cookieUtils.createRefreshTokenCookie(response, authResponse.getRefreshToken());
        ApiResponse<JwtAuthenticationResponse> apiResponse = ApiResponse.success(authResponse, "Login successfully");
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

}
