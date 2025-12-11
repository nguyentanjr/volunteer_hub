package com.example.demo.dto.registration;

import com.example.demo.dto.user.UserResponse;
import com.example.demo.model.Event;
import com.example.demo.model.Registration;
import com.example.demo.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationDTO {

    private Long id;

    private Registration.RegistrationStatus status;

    private LocalDateTime registeredAt ;

    private LocalDateTime completedAt;

    private Boolean eventCompleted;

    private UserResponse userResponse;

    private Long eventId;

    private String eventTitle;
    private String eventLocation;
    private LocalDateTime eventStartTime;

}