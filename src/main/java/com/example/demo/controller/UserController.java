package com.example.demo.controller;

import com.example.demo.dto.common.ApiResponse;
import com.example.demo.dto.user.*;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    /**
     * Get current user's available roles
     */
    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<UserRolesDTO>> getMyRoles() {
        log.info("Getting roles for current user");
        User user = userService.getCurrentUser();
        
        UserRolesDTO data = UserRolesDTO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .roles(user.getRoleNames())
                .activeRole(user.getRole())
                .build();
        
        log.info("User {} has roles: {}, active: {}", user.getUsername(), data.getRoles(), data.getActiveRole());
        return ResponseEntity.ok(ApiResponse.success(data, "User roles retrieved successfully"));
    }

    /**
     * Switch the active role
     */
    @PostMapping("/switch-role")
    public ResponseEntity<ApiResponse<UserResponse>> switchRole(@Valid @RequestBody SwitchRoleDTO switchRoleDTO) {
        log.info("User requesting role switch to: {}", switchRoleDTO.getRole());
        User savedUser = userService.switchUserRole(switchRoleDTO.getRole());
        return ResponseEntity.ok(ApiResponse.success(
            userMapper.toUserResponse(savedUser), 
            "Role switched successfully to " + switchRoleDTO.getRole()
        ));
    }

    /**
     * Get current user profile with all roles
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileDTO>> getCurrentUser() {
        log.info("Getting current user profile");
        User user = userService.getCurrentUser();
        
        UserProfileDTO data = UserProfileDTO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(user.getRoleNames())
                .activeRole(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(data, "User profile retrieved successfully"));
    }
}
