package com.example.demo.service.Impl;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.RefreshToken;
import com.example.demo.model.User;
import com.example.demo.repository.RefreshTokenRepository;
import com.example.demo.security.JWTUtils;
import com.example.demo.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final JWTUtils jwtUtils;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public RefreshToken createRefreshToken(User user) {
        deleteByUser(user);
        log.info("Create refresh token for user:");
        RefreshToken refreshToken = new RefreshToken()
                .setUser(user)
                .setExpiryDate(Instant.now().plusMillis(jwtUtils.getJwtTokenExpirationMs()))
                .setToken(UUID.randomUUID().toString());
        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public Optional<RefreshToken> findByToken(String refreshToken) {
        return refreshTokenRepository.findByToken(refreshToken);
    }

    @Override
    public int deleteByUser(User user) {
        log.info("Delete refresh token from user: {}", user.getUsername());
        return refreshTokenRepository.deleteByUser(user.getId());
    }

    public void revokeToken(RefreshToken refreshToken) {
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);
    }

    public void markAsUsed(RefreshToken refreshToken) {
        refreshToken.markAsUsed();
    }



    @Override
    public RefreshToken isRefreshTokenValid(RefreshToken refreshToken) {
        if(refreshToken.isExpired()) {
            log.warn("Refresh token is expired for user: {}", refreshToken.getUser().getUsername());
            throw new ResourceNotFoundException("Refresh Token is expired");
        }

        if(refreshToken.isRevoked()) {
            log.warn("Refresh token is revoked for user: {}", refreshToken.getUser().getUsername());
            throw new ResourceNotFoundException("Refresh Token is revoked");
        }

        return refreshToken;
    }

    @Override
    public RefreshToken rotateToken(RefreshToken currentToken) {
        log.info("Rotating token for user: {}", currentToken.getUser().getUsername());

        currentToken.markAsUsed();
        currentToken.revoke();
        refreshTokenRepository.save(currentToken);

        RefreshToken newRefreshToken = new RefreshToken()
                .setToken(UUID.randomUUID().toString())
                .setExpiryDate(Instant.now().plusMillis(jwtUtils.getJwtTokenExpirationMs()))
                .setUser(currentToken.getUser());
        RefreshToken savedRefreshToken = refreshTokenRepository.save(newRefreshToken);

        log.info("Created new refresh token rotation for user: {}", currentToken.getUser().getUsername());

        return savedRefreshToken;


    }
}
