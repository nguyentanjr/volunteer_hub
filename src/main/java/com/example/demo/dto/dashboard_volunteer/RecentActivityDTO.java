package com.example.demo.dto.dashboard_volunteer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class RecentActivityDTO {
    private Long eventId;
    private String eventTitle;
    private ActivityType activityType;
    private LocalDateTime createdAt;
    private String activityDescription;
    public enum ActivityType {
        POST,
        COMMENT,
        LIKE_COMMENT,
        LIKE_POST,
        PENDING_REGISTRATION,
        APPROVED_REGISTRATION
    }
}


