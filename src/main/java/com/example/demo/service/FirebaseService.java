package com.example.demo.service;

import com.example.demo.dto.user_fcm.UserFcmTokenDTO;
import com.example.demo.model.UserFcmToken;
import com.google.api.core.ApiFuture;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import java.util.List;

public interface FirebaseService {
    ApiFuture<String> sendToToken(String token, String title, String body) throws FirebaseMessagingException;

    void registerTopicForUser(String topic, List<String> tokens) throws FirebaseMessagingException;

    ApiFuture<String> sendToTopic(String topic, String title, String body);

    UserFcmToken registerTokenForUser(UserFcmTokenDTO userFcmTokenDTO);
}
