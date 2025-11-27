package com.example.demo.service.Impl;

import com.example.demo.exception.InvalidTokenException;
import com.example.demo.exception.TokenExpiredException;
import com.example.demo.exception.UnsupportedOperationException;
import com.example.demo.model.PasswordResetToken;
import com.example.demo.model.User;
import com.example.demo.repository.PasswordResetTokenRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.PasswordResetTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PassWordResetTokenServiceImpl implements PasswordResetTokenService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public PasswordResetToken generatePasswordResetToken(User user) {

        log.info("Generate password reset token for user: {}", user.getUsername());
        String token = UUID.randomUUID().toString();
        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setToken(token);
        passwordResetToken.setUser(user);
        passwordResetToken.setExpiry(Instant.now().plusMillis(8640000));
        return passwordResetTokenRepository.save(passwordResetToken);
    }

    @Override
    public void validateToken(String token) {
        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findPasswordResetTokenByToken(token);

        if (passwordResetToken == null) {
            throw new InvalidTokenException("Token not found");
        }

        User user = passwordResetToken.getUser();
        if (user.getAuthProvider() != User.AuthProvider.LOCAL) {
            throw new UnsupportedOperationException("Password reset is not supported for OAuth accounts");
        }

        if (passwordResetToken.getExpiry().isBefore(Instant.now())) {
            throw new TokenExpiredException("Password reset token has expired");
        }
    }

    @Override
    public void resetPassword(String token, String password) {
        validateToken(token);
        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findPasswordResetTokenByToken(token);
        User user = passwordResetToken.getUser();
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        passwordResetTokenRepository.delete(passwordResetToken);
    }
}
