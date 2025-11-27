package com.example.demo.service.Impl;

import com.example.demo.dto.user_fcm.UserFcmTokenDTO;
import com.example.demo.model.UserFcmToken;
import com.example.demo.service.FirebaseService;
import com.example.demo.service.UserService;
import com.google.api.core.ApiFuture;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirebaseServiceImpl implements FirebaseService {

    private final UserService userService;

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

    public UserFcmToken registerTokenForUser(UserFcmTokenDTO userFcmTokenDTO) {
        return new UserFcmToken()
                .setToken(userFcmTokenDTO.getToken())
                .setDeviceId(userFcmTokenDTO.getDeviceId())
                .setDeviceType(userFcmTokenDTO.getDeviceType())
                .setUser(userService.getCurrentUser());
    }


}
