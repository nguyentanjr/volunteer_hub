package com.example.demo.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
public class UserLoginResponseDTO {
    private final Long id;
    private final String username;
    private final String role;
}
