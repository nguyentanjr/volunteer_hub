package com.example.demo.service.Impl;

import com.example.demo.model.AuditLog;
import com.example.demo.model.User;
import com.example.demo.repository.AuditLogRepository;
import com.example.demo.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {
    
    private final AuditLogRepository auditLogRepository;
    
    @Override
    @Transactional
    public void logUserAction(AuditLog.ActionType actionType, Long userId, String oldValue, 
                              String newValue, String reason, User performedBy) {
        log.info("Logging user action: {} for user ID: {} by admin: {}", 
                actionType, userId, performedBy.getUsername());
        
        AuditLog auditLog = AuditLog.builder()
                .actionType(actionType)
                .entityType("USER")
                .entityId(userId)
                .performedBy(performedBy.getId())
                .performedByUsername(performedBy.getUsername())
                .description(buildDescription(actionType, userId))
                .oldValue(oldValue)
                .newValue(newValue)
                .reason(reason)
                .build();
        
        auditLogRepository.save(auditLog);
    }
    
    @Override
    @Transactional
    public void logEventAction(AuditLog.ActionType actionType, Long eventId, 
                               String description, User performedBy) {
        log.info("Logging event action: {} for event ID: {} by admin: {}", 
                actionType, eventId, performedBy.getUsername());
        
        AuditLog auditLog = AuditLog.builder()
                .actionType(actionType)
                .entityType("EVENT")
                .entityId(eventId)
                .performedBy(performedBy.getId())
                .performedByUsername(performedBy.getUsername())
                .description(description)
                .build();
        
        auditLogRepository.save(auditLog);
    }
    
    @Override
    @Transactional
    public void logRegistrationAction(AuditLog.ActionType actionType, Long registrationId, 
                                      String description, User performedBy) {
        log.info("Logging registration action: {} for registration ID: {} by admin: {}", 
                actionType, registrationId, performedBy.getUsername());
        
        AuditLog auditLog = AuditLog.builder()
                .actionType(actionType)
                .entityType("REGISTRATION")
                .entityId(registrationId)
                .performedBy(performedBy.getId())
                .performedByUsername(performedBy.getUsername())
                .description(description)
                .build();
        
        auditLogRepository.save(auditLog);
    }
    
    private String buildDescription(AuditLog.ActionType actionType, Long userId) {
        return switch (actionType) {
            case USER_ENABLED -> "User ID " + userId + " was enabled";
            case USER_DISABLED -> "User ID " + userId + " was disabled";
            case USER_ROLE_CHANGED -> "User ID " + userId + " role was changed";
            case USER_PASSWORD_RESET -> "Password was reset for user ID " + userId;
            case USER_DELETED -> "User ID " + userId + " was deleted";
            default -> "Action " + actionType + " performed on user ID " + userId;
        };
    }
}

