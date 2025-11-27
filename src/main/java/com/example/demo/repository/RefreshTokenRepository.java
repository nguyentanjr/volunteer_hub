package com.example.demo.repository;

import com.example.demo.model.RefreshToken;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Transactional
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id =:userId")
    int deleteByUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(rt) > 0 FROM RefreshToken rt WHERE rt.token =: refreshToken AND rt.expiryDate > :now AND rt.revoked = false ")
    boolean isRefreshTokenValid(String refreshToken);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.token = :refreshToken")
    Optional<RefreshToken> findByToken(String refreshToken);
}
