package com.example.demo.repository;

import com.example.demo.model.Comment;
import com.example.demo.model.Notification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
public class NotificationRepositoryCustomImpl implements NotificationRepositoryCustom{

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Notification> findByLatestWithLimit(Long userId, int limit) {
        log.debug("Finding notifications ordered by latest with limit {}", limit);

        String jpql = "SELECT n FROM Notification n WHERE n.user.id = :userId " +
                "ORDER BY n.createdAt DESC, n.id DESC";

        TypedQuery<Notification> query = entityManager.createQuery(jpql, Notification.class);
        query.setParameter("userId", userId);
        query.setMaxResults(limit);

        return query.getResultList();
    }

    @Override
    public List<Notification> findByLatestWithCursorAndLimit(Long userId, LocalDateTime cursorDate, int limit) {

        String jpql = "SELECT n FROM Notification n WHERE n.user.id = :userId AND n.createdAt < :cursorDate " +
                "ORDER BY n.createdAt DESC, n.id DESC";

        TypedQuery<Notification> query = entityManager.createQuery(jpql, Notification.class);
        query.setParameter("userId", cursorDate);
        query.setParameter("cursorDate", cursorDate);
        query.setMaxResults(limit);

        return query.getResultList();
    }
}
