package com.example.demo.service.Impl;

import com.example.demo.model.RefreshToken;
import com.example.demo.model.User;
import com.example.demo.repository.RefreshTokenRepository;
import com.example.demo.security.JWTUtils;
import com.example.demo.service.RefreshTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    @Autowired
    private JWTUtils jwtUtils;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Override
    public RefreshToken createRefreshToken(User user) {
        log.info("Create refresh token for user:");

        deleteByUser(user);
        RefreshToken refreshToken = new RefreshToken()
                .setUser(user)
                .setExpiryDate(Instant.now().plusMillis(jwtUtils.getJwtTokenExpirationMs()))
                .setToken(UUID.randomUUID().toString());
        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public int deleteByUser(User user) {
        log.info("Delete refresh token from user: {}", user.getUsername());
        return refreshTokenRepository.deleteByUser(user);
    }

    public void revokeToken(RefreshToken refreshToken) {
        refreshToken.revoke();
    }

    public void markAsUsed(RefreshToken refreshToken) {
        refreshToken.markAsUsed();
    }

    public boolean isRefreshTokenValid(RefreshToken refreshToken) {
        return refreshTokenRepository.isRefreshTokenValid(refreshToken.getToken());
    }

}
