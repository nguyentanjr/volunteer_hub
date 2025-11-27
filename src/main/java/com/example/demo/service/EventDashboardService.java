package com.example.demo.service;

import com.example.demo.dto.dashboard_manager.EventDashboardDTO;

public interface EventDashboardService {
    
    /**
     * Get complete dashboard data for the current event manager
     * @return EventDashboardDTO with all statistics and information
     */
    EventDashboardDTO getManagerDashboard();
}

