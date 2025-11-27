package com.example.demo.repository;

import com.example.demo.model.Notification;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepositoryCustom {

    public List<Notification> findByLatestWithLimit(Long userId, int limit);

    public List<Notification> findByLatestWithCursorAndLimit(Long userId, LocalDateTime cursorDate, int limit);
}
