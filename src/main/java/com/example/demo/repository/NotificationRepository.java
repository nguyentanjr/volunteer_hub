package com.example.demo.repository;

import com.example.demo.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long>, NotificationRepositoryCustom {
    Optional<Notification> getNotificationById(Long id);

    List<Notification> findAllByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.readStatus = true WHERE n.user.id = :userId AND n.id = :notificationId")
    void markAsReadForUser(Long userId, Long notificationId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.readStatus = true WHERE n.user.id = :userId")
    void markAllAsReadForUser(Long userId);

}
