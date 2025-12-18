package com.example.demo.dto.dashboard_volunteer;

import com.example.demo.model.Event;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class RecommendEventDTO {
    private Long eventId;
    private String eventTitle;
    private String description;
    private LocalDateTime date;
    private String location;
    private Event.EventStatus status;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private LocalDateTime createdAt = LocalDateTime.now();
    // Changed from User entity to simple fields
    private Long creatorId;
    private String creatorName;
}
