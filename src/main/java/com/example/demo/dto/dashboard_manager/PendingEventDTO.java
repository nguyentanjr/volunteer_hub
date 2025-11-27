package com.example.demo.dto.dashboard_manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingEventDTO {
    private Long eventId;
    private String title;
    private String description;
    private String location;
    private LocalDateTime date;
    private Integer maxParticipants;
    private LocalDateTime createdAt;
    private String creatorName;
    private String creatorEmail;
}

