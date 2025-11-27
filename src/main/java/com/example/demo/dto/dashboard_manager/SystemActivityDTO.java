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
public class SystemActivityDTO {
    private String activityType;
    private String description;
    private LocalDateTime timestamp;
    private String userName;
    private Long relatedId;
}

