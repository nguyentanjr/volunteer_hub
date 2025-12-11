package com.example.demo.service.Impl;

import com.example.demo.dto.user_fcm.UserFcmTokenDTO;
import com.example.demo.model.User;
import com.example.demo.model.UserFcmToken;
import com.example.demo.repository.UserFcmTokenRepository;
import com.example.demo.service.FirebaseService;
import com.example.demo.service.UserService;
import com.google.api.core.ApiFuture;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirebaseServiceImpl implements FirebaseService {

    private final UserService userService;
    private final UserFcmTokenRepository userFcmTokenRepository;

    public ApiFuture<String> sendToToken(String token, String title, String body) throws FirebaseMessagingException {
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        Message message = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .build();

        return FirebaseMessaging.getInstance().sendAsync(message);
    }

    public void registerTopicForUser(String topic, List<String> tokens) throws FirebaseMessagingException {
        FirebaseMessaging.getInstance().subscribeToTopic(tokens, topic);
    }

    public ApiFuture<String> sendToTopic(String topic, String title, String body) {
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        Message message = Message.builder()
                .setTopic(topic)
                .setNotification(notification)
                .build();

        return FirebaseMessaging.getInstance().sendAsync(message);
    }

    @Transactional
    public UserFcmToken registerTokenForUser(UserFcmTokenDTO userFcmTokenDTO) {
        User currentUser = userService.getCurrentUser();
        
        // Kiểm tra xem user đã có token chưa
        Optional<UserFcmToken> existingToken = userFcmTokenRepository.findByUser(currentUser);
        
        if (existingToken.isPresent()) {
            // Update token nếu đã tồn tại
            UserFcmToken token = existingToken.get();
            token.setToken(userFcmTokenDTO.getToken());
            token.setDeviceId(userFcmTokenDTO.getDeviceId());
            token.setDeviceType(userFcmTokenDTO.getDeviceType());
            token.setLastUpdated(LocalDateTime.now());
            log.info("Updated FCM token for user: {}", currentUser.getId());
            return userFcmTokenRepository.save(token);
        } else {
            // Tạo token mới
            UserFcmToken newToken = new UserFcmToken()
                    .setToken(userFcmTokenDTO.getToken())
                    .setDeviceId(userFcmTokenDTO.getDeviceId())
                    .setDeviceType(userFcmTokenDTO.getDeviceType())
                    .setUser(currentUser)
                    .setCreatedAt(LocalDateTime.now());
            log.info("Registered new FCM token for user: {}", currentUser.getId());
            return userFcmTokenRepository.save(newToken);
        }
    }


}
