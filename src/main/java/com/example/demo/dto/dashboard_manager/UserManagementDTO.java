package com.example.demo.dto.dashboard_manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserManagementDTO {
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private List<String> roles;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivity;
    private Integer eventsCreated;
    private Integer registrationsCount;
}

