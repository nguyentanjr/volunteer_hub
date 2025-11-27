
package com.example.demo.dto.dashboard_manager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventDashboardDTO {
    
    // Tổng quan
    private Integer totalManagedEvents;
    private Integer activeEvents;
    private Integer upcomingEvents;
    private Integer completedEvents;
    
    private RegistrationStatisticsDTO registrationStatistics;
    
    private List<EventWithPendingRegistrationsDTO> eventsNeedingApproval;
    
    private List<EventActivityDTO> recentActivities;

    private List<UpcomingEventDTO> upcomingEventsNeedPreparation;
    
    private List<EventAttendanceReportDTO> attendanceReports;
}

