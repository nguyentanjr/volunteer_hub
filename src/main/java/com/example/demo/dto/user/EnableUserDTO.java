package com.example.demo.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnableUserDTO {
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Enabled status is required")
    private Boolean enabled;
    
    private String reason;
}

