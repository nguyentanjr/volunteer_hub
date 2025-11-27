package com.example.demo.dto.user;

import com.example.demo.model.Role;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRolesDTO {
    @JsonProperty("user_id")
    private Long userId;
    
    private String username;
    
    private Set<Role.RoleName> roles;
    
    @JsonProperty("active_role")
    private Role.RoleName activeRole;
}

