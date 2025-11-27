package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ToString(exclude = {"user", "event"})
public class Registration implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatus status;


    private LocalDateTime registeredAt = LocalDateTime.now();

    private LocalDateTime completedAt;

    public Registration complete() {
        this.status = RegistrationStatus.APPROVED;
        this.completedAt = LocalDateTime.now();
        return this;
    }

    @Column(name = "is_event_completed")
    private Boolean eventCompleted;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    @JsonIgnore
    private Event event;

    public enum RegistrationStatus {
        PENDING,
        APPROVED,
        REJECTED,
        CANCELLED,
        WAITING
    }
}