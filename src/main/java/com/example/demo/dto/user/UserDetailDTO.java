package com.example.demo.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailDTO {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;
    private String role;
    private Boolean enabled;
    private String authProvider;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private Integer eventsCreatedCount;
    private Integer registrationsCount;
    private Integer postsCount;
    private Integer commentsCount;
    
    private List<String> tags;
}

