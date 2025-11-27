package com.example.demo.dto.dashboard;

import com.example.demo.model.Event;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpcomingEventDTO {
    private Long eventId;
    private String title;
    private LocalDateTime eventDate;
    private Integer daysUntilEvent;
    private Integer approvedParticipants;
    private Integer maxParticipants;
    private String location;
    private Event.EventStatus status;
    private Boolean hasAllPreparationsDone;  // Có thể thêm logic kiểm tra
}

