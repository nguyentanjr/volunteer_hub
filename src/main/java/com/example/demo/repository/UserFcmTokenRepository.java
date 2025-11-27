package com.example.demo.repository;

import com.example.demo.model.User;
import com.example.demo.model.UserFcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserFcmTokenRepository extends JpaRepository<UserFcmToken, Long> {

    @Query("SELECT u FROM UserFcmToken u WHERE u.user = :user")
    Optional<UserFcmToken> findByUser(User user);
}
