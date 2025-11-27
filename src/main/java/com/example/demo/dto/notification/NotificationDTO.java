package com.example.demo.dto.notification;

import com.example.demo.model.Notification;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDTO {
    private Long id;
    private String title;
    private String message;
    private Notification.RelatedType relatedType;
    private Long relatedId;
    private LocalDateTime createdAt = LocalDateTime.now();
    private boolean readStatus = false;
}
