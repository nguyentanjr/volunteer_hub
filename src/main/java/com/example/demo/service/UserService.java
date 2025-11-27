package com.example.demo.service;

import com.example.demo.dto.common.PaginationResponse;
import com.example.demo.dto.user.UserResponse;
import com.example.demo.model.Notification;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface UserService {

    PaginationResponse<UserResponse> getAllUsers(Pageable pageable);

    PaginationResponse<UserResponse> getUsersByEnabled(boolean enabled, Pageable pageable);

    PaginationResponse<UserResponse> getUsersWithSearch(String search, Pageable pageable);

    List<User> getAllAdmin();

    User getUserByUsername(String username);

//    Notification getNotificationById

    User getCurrentUser();

    User switchUserRole(Role.RoleName roleName);

}
