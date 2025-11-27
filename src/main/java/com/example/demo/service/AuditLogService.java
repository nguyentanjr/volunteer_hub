package com.example.demo.service;

import com.example.demo.model.AuditLog;
import com.example.demo.model.User;

public interface AuditLogService {
    void logUserAction(AuditLog.ActionType actionType, Long userId, String oldValue, String newValue, String reason, User performedBy);
    
    void logEventAction(AuditLog.ActionType actionType, Long eventId, String description, User performedBy);
    
    void logRegistrationAction(AuditLog.ActionType actionType, Long registrationId, String description, User performedBy);
}

