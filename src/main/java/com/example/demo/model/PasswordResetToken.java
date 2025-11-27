package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;

    private Instant expiry;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;
}
