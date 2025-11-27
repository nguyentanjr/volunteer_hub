package com.example.demo.dto.dashboard_manager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventActivityDTO {
    private Long eventId;
    private String eventTitle;
    private ActivityType activityType;
    private String activityDescription;
    private LocalDateTime activityTime;
    private String userName;  // Người thực hiện hoạt động
    
    public enum ActivityType {
        NEW_REGISTRATION,
        NEW_POST,
        NEW_COMMENT,
        REGISTRATION_CANCELLED
    }
}

