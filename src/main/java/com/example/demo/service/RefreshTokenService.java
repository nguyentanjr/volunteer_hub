package com.example.demo.service;

import com.example.demo.model.RefreshToken;
import com.example.demo.model.User;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(User user);

    int deleteByUser(User user);
}
