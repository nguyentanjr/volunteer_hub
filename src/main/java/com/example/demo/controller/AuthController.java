package com.example.demo.controller;

import com.example.demo.dto.auth.JwtAuthenticationResponse;
import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.dto.auth.RegisterRequest;
import com.example.demo.dto.common.ApiResponse;
import com.example.demo.security.CookieUtils;
import com.example.demo.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<JwtAuthenticationResponse>> register(
            @Valid @RequestBody RegisterRequest registerRequest) {

        log.info("Registering new user: {}", registerRequest.getUsername());


        JwtAuthenticationResponse response = authService.register(registerRequest);
        ApiResponse<JwtAuthenticationResponse> apiResponse = ApiResponse.created(
                response,"User registered and logged in successfully");
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtAuthenticationResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {

        log.info("User attempt for user: {}", loginRequest.getUsername());

        JwtAuthenticationResponse response = authService.login(loginRequest);
        ApiResponse<JwtAuthenticationResponse> apiResponse = ApiResponse.success(response, "Login successfully");
        return ResponseEntity.ok(apiResponse);
    }

}
