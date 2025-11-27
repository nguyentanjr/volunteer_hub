package com.example.demo.service;

import com.example.demo.model.RefreshToken;
import com.example.demo.model.User;

import java.sql.Ref;
import java.util.Optional;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(User user);

    int deleteByUser(User user);

    RefreshToken isRefreshTokenValid(RefreshToken refreshToken);

    Optional<RefreshToken> findByToken(String refreshToken);

    RefreshToken rotateToken(RefreshToken refreshToken);

    void revokeToken(RefreshToken refreshToken);
}
