package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class UserFcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String token;

    @Enumerated(EnumType.STRING)
    private DeviceType deviceType;

    private String deviceId;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime lastUpdated;

    public enum DeviceType{
        MOBILE_WEB,
        DESKTOP_WEB
    }

}
