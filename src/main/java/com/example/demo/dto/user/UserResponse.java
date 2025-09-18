package com.example.demo.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserResponse {
    @JsonProperty("user_id")
    private final Long id;

    private String username;

    @JsonProperty("email_address")
    private final String email;

    @JsonProperty("first_name")
    private final String firstName;

    @JsonProperty("last_name")
    private final String lastName;

    @JsonProperty("phone")
    private final String phoneNumber;

    private final String address;

    @JsonProperty("user_role")
    private final String role;

    @JsonProperty("created_at")
    private final LocalDateTime createdAt;

    public UserResponse(Long id, String username, String email, String firstName,
                         String lastName, String phoneNumber, String address,
                         String role, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.role = role;
        this.createdAt = createdAt;
    }
}
