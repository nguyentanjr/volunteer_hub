package com.example.demo.dto.user;

import com.example.demo.model.Role;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeUserRoleDTO {
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "New role is required")
    private Role.RoleName newRole;
    
    private String reason;
}

