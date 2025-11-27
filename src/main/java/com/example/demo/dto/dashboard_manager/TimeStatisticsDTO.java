package com.example.demo.dto.dashboard_manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeStatisticsDTO {
    private Long newUsersLast7Days;
    private Long newEventsLast7Days;
    private Long newRegistrationsLast7Days;
    
    private Long newUsersLast30Days;
    private Long newEventsLast30Days;
    private Long newRegistrationsLast30Days;
    
    private Long usersThisMonth;
    private Long eventsThisMonth;
    private Long registrationsThisMonth;
    
    private Double userGrowthRate;
    private Double eventGrowthRate;
    private Double registrationGrowthRate;
}

