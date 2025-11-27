package com.example.demo.controller;

import com.example.demo.dto.common.ApiResponse;
import com.example.demo.dto.notification.NotificationCursorPageResponse;
import com.example.demo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final NotificationService notificationService;

    @PostMapping("/noti")
    public ResponseEntity<ApiResponse<String>> sendNoti() {
        Map<String, String> payload = new HashMap<>();
        payload.put("message", "HI");
        simpMessagingTemplate.convertAndSend("/topic/notifications", payload);
        return ResponseEntity.ok(ApiResponse.created("OK"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<NotificationCursorPageResponse>> getAllNotifications(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getAllNotifications(cursor, limit)));
    }

    @PutMapping("/me/{notificationId}/read")
    public ResponseEntity<ApiResponse<String>> markNotificationAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read"));
    }

    @PutMapping("/me/read-all")
    public ResponseEntity<ApiResponse<String>> markAllNotificationsAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read"));
    }

    @GetMapping("/me/{notificationId}/redirect")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNotificationRedirectInfo(@PathVariable Long notificationId) {
        Map<String, Object> redirectInfo = notificationService.getNotificationRedirectInfo(notificationId);
        return ResponseEntity.ok(ApiResponse.success(redirectInfo));
    }

}
