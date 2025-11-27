package com.example.demo.service;

import com.example.demo.dto.dashboard_manager.AdminDashboardDTO;
import com.example.demo.dto.event.EventDTO;
import com.example.demo.dto.user.ChangeUserRoleDTO;
import com.example.demo.dto.user.EnableUserDTO;
import com.example.demo.dto.user.ResetPasswordDTO;
import com.example.demo.dto.user.UserDetailDTO;
import com.example.demo.dto.user.UserResponse;
import com.example.demo.model.Event;
import com.example.demo.model.User;
import com.google.firebase.messaging.FirebaseMessagingException;

import java.util.List;

public interface AdminService {
    Event approveEvent(Long eventId) throws FirebaseMessagingException;

    Event rejectEvent(Long eventId, String message) throws FirebaseMessagingException;


    EventDTO getEventDetails(Long eventId);
    
    AdminDashboardDTO getAdminDashboard();
    
    // User Management
    UserDetailDTO getUserDetail(Long userId);
    
    UserResponse enableOrDisableUser(EnableUserDTO enableUserDTO, User admin);
    
    UserResponse changeUserRole(ChangeUserRoleDTO changeUserRoleDTO, User admin);
    
    void deleteUser(Long userId, String reason, User admin);
    
    void resetUserPassword(ResetPasswordDTO resetPasswordDTO, User admin);

    UserResponse promoteToEventManager(Long userId);
    
    UserResponse demoteFromEventManager(Long userId);
}
