package com.example.demo.service;

import com.example.demo.dto.auth.JwtAuthenticationResponse;
import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.dto.auth.RegisterRequest;
import com.example.demo.model.RefreshToken;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    JwtAuthenticationResponse login(LoginRequest loginRequest);

    JwtAuthenticationResponse register(RegisterRequest request);

    JwtAuthenticationResponse refreshToken(String RefreshToken);
}
