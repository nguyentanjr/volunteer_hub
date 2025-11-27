package com.example.demo.controller;

import com.example.demo.dto.common.ApiResponse;
import com.example.demo.dto.common.PaginationResponse;
import com.example.demo.dto.dashboard_manager.AdminDashboardDTO;
import com.example.demo.dto.event.EventDTO;
import com.example.demo.dto.user.ChangeUserRoleDTO;
import com.example.demo.dto.user.EnableUserDTO;
import com.example.demo.dto.user.ResetPasswordDTO;
import com.example.demo.dto.user.UserDetailDTO;
import com.example.demo.dto.user.UserResponse;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.Event;
import com.example.demo.model.User;
import com.example.demo.service.*;
import com.google.firebase.messaging.FirebaseMessagingException;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final AdminService adminService;
    private final ExportService exportService;
    private final UserMapper userMapper;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PaginationResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        PaginationResponse<UserResponse> pagniationResponse = userService.getAllUsers(pageable);

        return ResponseEntity.ok(ApiResponse.success(pagniationResponse, "Users retrieved successfully"));
    }

    @GetMapping("/users/enabled/{enabled}")
    public ResponseEntity<ApiResponse<PaginationResponse<UserResponse>>> getAllUsersByEnabled(
            @PathVariable boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        log.info("Getting users by enabled status: {} - page: {}, size: {}", enabled, page, size);

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        PaginationResponse<UserResponse> pagniationResponse = userService.getUsersByEnabled(enabled, pageable);

        return ResponseEntity.ok(ApiResponse.success(pagniationResponse, "Users by enabled status retrieved successfully"));
    }

    @GetMapping("/users/search")
    public ResponseEntity<ApiResponse<PaginationResponse<UserResponse>>> getAllUsersWithSearch(
            @RequestParam String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        log.info("Searching users with term: '{}' - page: {}, size: {}", search, page, size);

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        PaginationResponse<UserResponse> pagniationResponse = userService.getUsersWithSearch(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(pagniationResponse, "Users search completed successfully"));
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<ApiResponse<EventDTO>> getEventDetail(@PathVariable Long id) {
        EventDTO eventDTO = adminService.getEventDetails(id);
        return ResponseEntity.ok(ApiResponse.success(eventDTO));
    }

    @PutMapping("/events/{id}/approve")
    public ResponseEntity<ApiResponse<String>> approveEvent(@PathVariable Long id) throws FirebaseMessagingException {
        Event event = adminService.approveEvent(id);
        return ResponseEntity.ok(ApiResponse.success("Event approved"));
    }

    @PutMapping("/events/{id}/reject")
    public ResponseEntity<ApiResponse<String>> rejectEvent(@PathVariable Long id, @RequestBody Map<String, String> body) throws FirebaseMessagingException {
        Event event = adminService.rejectEvent(id,body.get("reason"));
        return ResponseEntity.ok(ApiResponse.success("Event rejected"));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminDashboardDTO>> getAdminDashboard() {
        log.info("Admin requesting dashboard");
        AdminDashboardDTO dashboard = adminService.getAdminDashboard();
        return ResponseEntity.ok(ApiResponse.success(dashboard, "Admin dashboard retrieved successfully"));
    }
    

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDetailDTO>> getUserDetail(@PathVariable Long userId) {
        log.info("Admin requesting user detail for user ID: {}", userId);
        UserDetailDTO userDetail = adminService.getUserDetail(userId);
        return ResponseEntity.ok(ApiResponse.success(userDetail, "User detail retrieved successfully"));
    }
    
    @PutMapping("/users/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> enableOrDisableUser(
            @Valid @RequestBody EnableUserDTO enableUserDTO,
            @AuthenticationPrincipal User admin) {
        log.info("Admin {} {} user ID: {}", 
                admin.getUsername(),
                enableUserDTO.getEnabled() ? "enabling" : "disabling",
                enableUserDTO.getUserId());
        
        UserResponse userResponse = adminService.enableOrDisableUser(enableUserDTO, admin);
        String message = enableUserDTO.getEnabled() ? "User enabled successfully" : "User disabled successfully";
        return ResponseEntity.ok(ApiResponse.success(userResponse, message));
    }
    
    @PutMapping("/users/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> changeUserRole(
            @Valid @RequestBody ChangeUserRoleDTO changeUserRoleDTO,
            @AuthenticationPrincipal User admin) {
        log.info("Admin {} changing role for user ID: {}", admin.getUsername(), changeUserRoleDTO.getUserId());
        UserResponse userResponse = adminService.changeUserRole(changeUserRoleDTO, admin);
        return ResponseEntity.ok(ApiResponse.success(userResponse, "User role changed successfully"));
    }
    
    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteUser(
            @PathVariable Long userId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal User admin) {
        log.info("Admin {} deleting user ID: {}", admin.getUsername(), userId);
        String reason = body != null ? body.get("reason") : null;
        adminService.deleteUser(userId, reason, admin);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
    }
    
    @PutMapping("/users/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> resetUserPassword(
            @Valid @RequestBody ResetPasswordDTO resetPasswordDTO,
            @AuthenticationPrincipal User admin) {
        log.info("Admin {} resetting password for user ID: {}", admin.getUsername(), resetPasswordDTO.getUserId());
        adminService.resetUserPassword(resetPasswordDTO, admin);
        return ResponseEntity.ok(ApiResponse.success( "User password reset successfully"));
    }
    
    
    @GetMapping("/export/events/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportEventsToCSV(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) throws IOException {
        log.info("Admin exporting events to CSV - status: {}, startDate: {}, endDate: {}", status, startDate, endDate);
        
        byte[] csvData;
        if (status != null || startDate != null || endDate != null) {
            csvData = exportService.exportEventsToCSV(status, startDate, endDate);
        } else {
            csvData = exportService.exportEventsToCSV();
        }
        
        String filename = "events_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvData);
    }
    
    @GetMapping("/export/events/json")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportEventsToJSON() throws IOException {
        log.info("Admin exporting events to JSON");
        
        byte[] jsonData = exportService.exportEventsToJSON();
        
        String filename = "events_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".json";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(jsonData);
    }
    
    @GetMapping("/export/users/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportUsersToCSV(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean enabled) throws IOException {
        log.info("Admin exporting users to CSV - role: {}, enabled: {}", role, enabled);
        
        byte[] csvData;
        if (role != null || enabled != null) {
            csvData = exportService.exportUsersToCSV(role, enabled);
        } else {
            csvData = exportService.exportUsersToCSV();
        }
        
        String filename = "users_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvData);
    }
    
    @GetMapping("/export/users/json")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportUsersToJSON() throws IOException {
        log.info("Admin exporting users to JSON");
        
        byte[] jsonData = exportService.exportUsersToJSON();
        
        String filename = "users_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".json";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(jsonData);
    }

    @PostMapping("/users/{userId}/promote")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> promoteToEventManager(
            @PathVariable Long userId,
            @AuthenticationPrincipal User admin) {
        log.info("Admin {} promoting user ID: {} to EVENT_MANAGER", admin.getUsername(), userId);
        UserResponse userResponse = adminService.promoteToEventManager(userId);
        return ResponseEntity.ok(ApiResponse.success(
            userResponse, 
            "User promoted to EVENT_MANAGER successfully"
        ));
    }

    @PostMapping("/users/{userId}/demote")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> demoteFromEventManager(
            @PathVariable Long userId,
            @AuthenticationPrincipal User admin) {
        log.info("Admin {} demoting user ID: {} from EVENT_MANAGER", admin.getUsername(), userId);
        UserResponse userResponse = adminService.demoteFromEventManager(userId);
        return ResponseEntity.ok(ApiResponse.success(
            userResponse, 
            "User demoted from EVENT_MANAGER successfully"
        ));
    }
}
