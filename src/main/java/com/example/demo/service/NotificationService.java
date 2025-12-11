package com.example.demo.service;

import com.example.demo.dto.event.EventDTO;
import com.example.demo.dto.notification.NotificationCursorPageResponse;
import com.example.demo.dto.notification.NotificationDTO;
import com.example.demo.model.Comment;
import com.example.demo.model.Event;
import com.example.demo.model.Notification;
import com.example.demo.model.User;

import java.util.Map;

public interface NotificationService {
    void notifyAdminsOfNewEvent(EventDTO eventDTO);

    void notifyManagerOnEventRejected(Event event, String message);

    void notifyManagerOnEventApproved(Event event, String message);

    void notifyManagerOnUserRegistrationCancelled(Long registrationId);
    
    void notifyManagerOnNewRegistration(Long registrationId);

    void notifyVolunteerOnEventUpdated(Event event);

    void notifyAllMembersInEventOnNewPost(Event event, com.example.demo.model.Post post);

    NotificationCursorPageResponse getAllNotifications(String cursor, int limit);

    void markAsRead(Long id);

    void markAllAsRead();

    Map<String, Object> getNotificationRedirectInfo(Long notificationId);
    
    // User management notifications
    void notifyUserAccountDisabled(User user, String reason);
    
    void notifyUserAccountEnabled(User user);
    
    void notifyUserRoleChanged(User user, String oldRole, String newRole);
    
    void notifyUserPasswordReset(User user, String newPassword);

    void notifyUserOnNewComment(User postCreator, Comment comment);

    void notifyUserOnNewChildComment(User commentCreator, Comment comment);
    
    void notifyUserOnNewLike(User postCreator, com.example.demo.model.Like like);
    
    void notifyUserOnCommentLike(User commentCreator, com.example.demo.model.Like like);
    
    void notifyVolunteerOnRegistrationApproved(User volunteer, Event event, Long registrationId);
    
    void notifyVolunteerOnRegistrationRejected(User volunteer, Event event, Long registrationId);

}
