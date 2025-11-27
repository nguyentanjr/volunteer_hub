package com.example.demo.controller;

import com.example.demo.dto.common.ApiResponse;
import com.example.demo.dto.dashboard_manager.EventDashboardDTO;
import com.example.demo.dto.dashboard_volunteer.VolunteerDashBoardDTO;
import com.example.demo.service.EventDashboardService;
import com.example.demo.service.VolunteerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final EventDashboardService eventDashboardService;
    private final VolunteerDashboardService volunteerDashboardService;

    @GetMapping("/manager")
    @PreAuthorize("hasAnyRole('EVENT_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<EventDashboardDTO>> getManagerDashboard() {
        EventDashboardDTO dashboard = eventDashboardService.getManagerDashboard();
        return ResponseEntity.ok(ApiResponse.success(dashboard, "Dashboard retrieved successfully"));
    }

    @GetMapping("/volunteer")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<ApiResponse<VolunteerDashBoardDTO>> getVolunteerDashboard() {
        VolunteerDashBoardDTO dashBoard = volunteerDashboardService.getVolunteerDashBoard();
        return ResponseEntity.ok(ApiResponse.success(dashBoard, "Dashboard retrieved successfully"));
    }
}

