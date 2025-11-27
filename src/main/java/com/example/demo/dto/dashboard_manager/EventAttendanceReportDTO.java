package com.example.demo.dto.dashboard_manager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventAttendanceReportDTO {
    private Long eventId;
    private String eventTitle;
    private LocalDateTime eventDate;
    private Integer registeredCount;
    private Integer approvedCount;
    private Integer attendedCount;
    private Integer maxParticipants;
    private Double attendanceRate;
    private Double fillRate;
}

