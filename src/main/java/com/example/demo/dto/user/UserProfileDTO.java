package com.example.demo.dto.user;

import com.example.demo.model.Role;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    @JsonProperty("user_id")
    private Long userId;
    
    private String username;
    
    private String email;
    
    @JsonProperty("first_name")
    private String firstName;
    
    @JsonProperty("last_name")
    private String lastName;
    
    private Set<Role.RoleName> roles;
    
    @JsonProperty("active_role")
    private Role.RoleName activeRole;
    
    private Boolean enabled;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}

