package com.example.demo.service;

import com.example.demo.model.PasswordResetToken;
import com.example.demo.model.User;

public interface PasswordResetTokenService {
    PasswordResetToken generatePasswordResetToken(User user);

    void validateToken(String token);

    void resetPassword(String token, String password);
}
