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
public class SwitchRoleDTO {
    @NotNull(message = "Role is required")
    private Role.RoleName role;
}

