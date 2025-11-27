package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType actionType;
    
    @Column(nullable = false)
    private String entityType;
    
    @Column(nullable = false)
    private Long entityId;
    
    @Column(nullable = false)
    private Long performedBy;
    
    private String performedByUsername;
    
    @Column(length = 1000)
    private String description;
    
    @Column(length = 2000)
    private String oldValue;
    
    @Column(length = 2000)
    private String newValue;
    
    private String reason;
    
    private String ipAddress;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
    
    public enum ActionType {
        USER_CREATED,
        USER_UPDATED,
        USER_DELETED,
        USER_ENABLED,
        USER_DISABLED,
        USER_ROLE_CHANGED,
        USER_PASSWORD_RESET,
        EVENT_CREATED,
        EVENT_UPDATED,
        EVENT_DELETED,
        EVENT_APPROVED,
        EVENT_REJECTED,
        REGISTRATION_APPROVED,
        REGISTRATION_REJECTED
    }
}

