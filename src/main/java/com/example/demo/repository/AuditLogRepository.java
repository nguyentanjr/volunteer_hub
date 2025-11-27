package com.example.demo.repository;

import com.example.demo.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.timestamp DESC")
    List<AuditLog> findByEntityTypeAndEntityId(@Param("entityType") String entityType, 
                                                @Param("entityId") Long entityId);
    
    @Query("SELECT a FROM AuditLog a WHERE a.performedBy = :userId ORDER BY a.timestamp DESC")
    Page<AuditLog> findByPerformedBy(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT a FROM AuditLog a WHERE a.actionType = :actionType ORDER BY a.timestamp DESC")
    Page<AuditLog> findByActionType(@Param("actionType") AuditLog.ActionType actionType, Pageable pageable);
    
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp >= :startDate ORDER BY a.timestamp DESC")
    List<AuditLog> findRecentLogs(@Param("startDate") LocalDateTime startDate, Pageable pageable);
    
    @Query("SELECT a FROM AuditLog a ORDER BY a.timestamp DESC")
    Page<AuditLog> findAllLogs(Pageable pageable);
}

