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
public class
UpcomingEventDTO {
    private Long eventId;
    private String eventTitle;
    private LocalDateTime eventDate;
    private Integer daysUntilEvent;
    private Integer maxParticipants;
    private String location;
    private Event.EventStatus status;
}
