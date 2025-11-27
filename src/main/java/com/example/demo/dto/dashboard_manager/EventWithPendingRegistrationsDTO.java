package com.example.demo.dto.dashboard_manager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventWithPendingRegistrationsDTO {
    private Long eventId;
    private String eventTitle;
    private Integer pendingCount;
    private Integer approvedCount;
    private Integer maxParticipants;
    private LocalDateTime eventDate;
    private Boolean isFull;  // Đã đầy chỗ chưa
}

