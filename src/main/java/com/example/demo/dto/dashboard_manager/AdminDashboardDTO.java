package com.example.demo.dto.dashboard_manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardDTO {
    private Long totalUsers;
    private Long totalEvents;
    private Long totalRegistrations;
    
    private Long totalVolunteers;
    private Long totalEventManagers;
    private Long totalAdmins;
    
    private Long plannedEvents;
    private Long ongoingEvents;
    private Long completedEvents;
    private Long cancelledEvents;
    
    private Long pendingRegistrations;
    private Long approvedRegistrations;
    private Long rejectedRegistrations;
    private Long cancelledRegistrations;

    private List<PendingEventDTO> pendingEvents;
    
    private List<SystemActivityDTO> recentActivities;
    
    private TimeStatisticsDTO timeStatistics;
    
    private List<UserManagementDTO> recentUsers;
    private Long enabledUsers;
    private Long disabledUsers;
}

