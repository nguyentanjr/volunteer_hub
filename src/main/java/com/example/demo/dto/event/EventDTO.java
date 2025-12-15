package com.example.demo.dto.event;

import com.example.demo.model.Event;
import com.example.demo.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventDTO {
    private Long eventId;
    private String title;
    private String description;
    private LocalDateTime date;
    private String location;
    private Event.EventStatus status;
    private Integer maxParticipants;
    private LocalDateTime createdAt = LocalDateTime.now();
    private String creatorUsername;
    private Long creatorId;
}
