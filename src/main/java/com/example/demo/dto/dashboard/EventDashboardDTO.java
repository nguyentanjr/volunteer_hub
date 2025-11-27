
package com.example.demo.dto.dashboard;

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
    
    // Thống kê đăng ký
    private RegistrationStatisticsDTO registrationStatistics;
    
    // Sự kiện cần duyệt đăng ký (có pending registrations)
    private List<EventWithPendingRegistrationsDTO> eventsNeedingApproval;
    
    // Hoạt động gần đây trong các sự kiện
    private List<EventActivityDTO> recentActivities;

    // Sự kiện sắp tới cần chuẩn bị (trong 7 ngày)
    private List<UpcomingEventDTO> upcomingEventsNeedPreparation;
    
    // Báo cáo tham dự
    private List<EventAttendanceReportDTO> attendanceReports;
}

