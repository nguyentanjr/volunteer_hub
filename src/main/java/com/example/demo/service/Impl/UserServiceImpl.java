package com.example.demo.service.Impl;

import com.example.demo.dto.common.PaginationResponse;
import com.example.demo.dto.user.UserResponse;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.Registration;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;

@Slf4j
@Service("userService")
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    @Override
    public PaginationResponse<UserResponse> getAllUsers(Pageable pageable) {
        log.info("Fetching all users with pagination - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<User> page = userRepository.findAll(pageable);
        Page<UserResponse> userResponsePage = page.map(userMapper::toUserResponse);;
        return new PaginationResponse<>(userResponsePage);
    }

    @Override
    public PaginationResponse<UserResponse> getUsersByEnabled(boolean enabled, Pageable pageable) {
        log.info("Fetching users by enabled status: {}, page: {}, size: {}",
                enabled, pageable.getPageNumber(), pageable.getPageSize());

        Page<User> page = userRepository.findALlByEnabled(enabled, pageable);
        Page<UserResponse> userResponsePage = page.map(userMapper::toUserResponse);
        return new PaginationResponse<>(userResponsePage);
    }

    @Override
    public PaginationResponse<UserResponse> getUsersWithSearch(String search, Pageable pageable) {
        log.info("Searching users with term: '{}', page: {}, size: {}",
                search, pageable.getPageNumber(), pageable.getPageSize());

        Page<User> page = userRepository.findUsersWithSearch(search, pageable);
        Page<UserResponse> userResponsePage = page.map(userMapper::toUserResponse);
        return new PaginationResponse<>(userResponsePage);
    }

    @Override
    public List<User> getAllAdmin() {
        return userRepository.findAllAdmin();
    }

    @Override
    @Cacheable(value = "users", key = "#username")
    public User getUserByUsername(String username) {
        return userRepository.getUserByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    public User getCurrentUser() {
        String username;
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            username =  ((UserDetails) principal).getUsername();
        }
        else {
            username = principal.toString();
        }
        return getUserByUsername(username);
    }

    @Override
    @Transactional
    public User switchUserRole(Role.RoleName roleName) {
        log.info("Switching user role to: {}", roleName);
        User user = getCurrentUser();
        user.switchActiveRole(roleName);
        User savedUser = userRepository.save(user);
        log.info("User {} switched to role: {}", user.getUsername(), roleName);
        return savedUser;
    }


}
