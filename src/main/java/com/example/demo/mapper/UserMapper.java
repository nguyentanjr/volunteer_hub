package com.example.demo.mapper;
import com.example.demo.dto.user.UserLoginResponseDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.mapstruct.factory.Mappers;

import com.example.demo.dto.auth.RegisterRequest;
import com.example.demo.dto.user.UserResponse;
import com.example.demo.model.User;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.util.Set;

@Mapper(
        componentModel = "spring"
)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)                    // ID is generated
    @Mapping(target = "roles", ignore = true)                 // Roles set by service layer
    @Mapping(target = "enabled", constant = "true")           // Default enabled
    @Mapping(target = "createdAt", ignore = true)             // Set by @PrePersist
    @Mapping(target = "updatedAt", ignore = true)             // Set by @PrePersist
    @Mapping(target = "authorities", ignore = true)           // UserDetails method
    @Mapping(target = "accountNonExpired", ignore = true)     // UserDetails method
    @Mapping(target = "accountNonLocked", ignore = true)      // UserDetails method
    @Mapping(target = "credentialsNonExpired", ignore = true) // UserDetails method
    @Mapping(target = "likes", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "posts", ignore = true)
    @Mapping(target = "events", ignore = true)
    @Mapping(target = "notifications", ignore = true)
    @Mapping(target = "fileRecords", ignore = true)
    @Mapping(target = "authProvider", ignore = true)
    @Mapping(target = "providerId", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "userFcmTokens", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "activeRole", ignore = true)
    @Mapping(target = "roleNames", ignore = true)
    User toUser(RegisterRequest registerRequest);

    @Mapping(target = "roles", expression = "java(mapRoles(user.getRoles()))")
    UserResponse toUserResponse(User user);
    
    // Helper method to map roles without circular references
    default Set<com.example.demo.model.Role> mapRoles(Set<com.example.demo.model.Role> roles) {
        if (roles == null) {
            return null;
        }
        // Return roles as-is (Role entity doesn't have back reference to User)
        return roles;
    }

    UserLoginResponseDTO toUserLoginResponseDTO(User user);

}

