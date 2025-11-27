package com.example.demo.service.Impl;

import com.example.demo.dto.comment.CommentCursorPageResponse;
import com.example.demo.dto.comment.CommentDTO;
import com.example.demo.dto.comment.CommentSortType;
import com.example.demo.dto.event.EventDTO;
import com.example.demo.dto.notification.NotificationCursorPageResponse;
import com.example.demo.dto.notification.NotificationDTO;
import com.example.demo.dto.registration.RegistrationDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.NotificationMapper;
import com.example.demo.mapper.RegistrationMapper;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.NotificationService;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final RegistrationMapper registrationMapper;
    private final RegistrationRepository registrationRepository;
    private final UserService userService;
    private final NotificationMapper notificationMapper;
    private final EventRepository eventRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;

    @Async("notificationExecutor")
    public void notifyAdminsOfNewEvent(EventDTO eventDTO) {
        Map<String, Object> message = new HashMap<>();
        message.put("event_manager", eventDTO);
        simpMessagingTemplate.convertAndSend("/topic/admin", message);
        List<User> admins = userRepository.findAllAdmin();
        for (User admin : admins) {
            Notification notification = new Notification()
                    .setTitle("New Event Pending Approval")
                    .setMessage("A new event: " + eventDTO.getTitle() + " created by " + eventDTO.getCreatorUsername() + " is pending your approval.")
                    .setUser(admin)
                    .setRelatedType(Notification.RelatedType.EVENT)
                    .setRelatedId(eventDTO.getEventId());
            notificationRepository.save(notification);
        }
    }

    @Async("notificationExecutor")
    public void notifyManagerOnEventRejected(Event event, String message) {
        log.info("Notify event manager on rejection");
        User eventCreator = event.getCreator();
        Notification notification = new Notification()
                .setUser(eventCreator)
                .setTitle("Event Creation Request Rejected")
                .setMessage(message)
                .setRelatedType(Notification.RelatedType.EVENT)
                .setRelatedId(event.getId());
        notificationRepository.save(notification);
        simpMessagingTemplate.convertAndSendToUser(eventCreator.getUsername(), "/queue/notification", notification);
    }

    @Async("notificationExecutor")
    public void notifyManagerOnEventApproved(Event event, String message) {
        log.info("Notify event manager on approval");
        User eventCreator = event.getCreator();
        Notification notification = new Notification()
                .setUser(eventCreator)
                .setTitle("Event Creation Request Approved")
                .setMessage(message)
                .setRelatedType(Notification.RelatedType.EVENT)
                .setRelatedId(event.getId());
        notificationRepository.save(notification);
        simpMessagingTemplate.convertAndSendToUser(eventCreator.getUsername(), "/queue/notification", notification);
    }

    @Async("notificationExecutor")
    public void notifyVolunteerOnEventUpdated(Event event) {
        log.info("Notify volunteer on event updated: {}", event.getId());
        List<Registration> registrationList = event.getRegistrations();
        for(Registration registration : registrationList) {
            User volunteer = registration.getUser();
            String message = "Event '" + event.getTitle() + "' has been updated. Please check the latest details.";
            Notification notification = new Notification()
                    .setUser(volunteer)
                    .setTitle("Event Updated")
                    .setMessage(message)
                    .setRelatedType(Notification.RelatedType.EVENT)
                    .setRelatedId(event.getId());
            notificationRepository.save(notification);
            simpMessagingTemplate.convertAndSendToUser(
                    volunteer.getUsername(), "/queue/notification", notification);
        }
    }

    @Async("notificationExecutor")
    public void notifyManagerOnUserRegistrationCancelled(Long registrationId) {
        log.info("Notify Manager on user registration cancelled");
        RegistrationDTO registrationDTO = registrationMapper.toRegistrationDTO(registrationRepository.findRegistrationById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found")));
        Event event = eventRepository.getEventById(registrationDTO.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        User eventCreator = event.getCreator();
        String message = "Volunteer " + registrationDTO.getUserResponse().getUsername()
                + " has cancelled their registration for your event '" + event.getTitle() + "'.";
        Notification notification = new Notification()
                .setUser(eventCreator)
                .setTitle("Volunteer Registration Cancelled")
                .setRelatedType(Notification.RelatedType.REGISTRATION)
                .setRelatedId(registrationId)
                .setMessage(message);
        notificationRepository.save(notification);
        simpMessagingTemplate.convertAndSendToUser(eventCreator.getUsername(), "/queue/notification", notification);
    }

    @Async("notificationExecutor")
    public void notifyAllMembersInEventOnNewPost(Event event, Long postId) {
        log.info("Notify all member on new post");
        List<Registration> registrationList = event.getRegistrations();
        registrationList.stream()
                .filter(registration -> registration.getStatus() == Registration.RegistrationStatus.APPROVED)
                .forEach(registration -> {
                    User user = registration.getUser();

                    Notification notification = new Notification()
                            .setTitle("New post coming!")
                            .setMessage("A new post has been posted. Click to see! ")
                            .setUser(user)
                            .setRelatedType(Notification.RelatedType.POST)
                            .setRelatedId(postId);
                    notificationRepository.save(notification);

                    simpMessagingTemplate.convertAndSendToUser(
                            user.getUsername(),
                            "/queue/notification",
                            notification
                    );
                });

    }

    @Override
    public NotificationCursorPageResponse getAllNotifications(String cursor, int limit) {
        if(limit <= 0 || limit >= 100) {
            limit = 20;
        }

        User user = userService.getCurrentUser();

        List<Notification> notifications;
        notifications = getNotificationsByLatest(user.getId(), cursor, limit);

        boolean hasNext = notifications.size() > limit;
        if(hasNext) {
            notifications = notifications.subList(0, limit);
        }

        List<NotificationDTO> notificationDTOS = notifications
                .stream()
                .map(notificationMapper::toNotificationDTO)
                .toList();

        Notification nextCursor = hasNext && !notifications.isEmpty()
                ? notifications.get(notifications.size() - 1)
                : null;
        String encodedCursor = nextCursor != null ? encodeCursor(nextCursor) : null;
        return NotificationCursorPageResponse.of(notificationDTOS, encodedCursor, hasNext);

    }

    private List<Notification> getNotificationsByLatest(Long userId, String cursor, int limit) {
        if(cursor == null || cursor.isEmpty()) {
            return notificationRepository.findByLatestWithLimit(userId, limit + 1);
        }
        else {
            return notificationRepository.findByLatestWithCursorAndLimit(userId, decodeDateCursor(cursor), limit + 1);
        }
    }

    private String encodeCursor(Notification notification) {
        String cursorData;
        cursorData = notification.getCreatedAt().toString();
        return Base64.getEncoder().encodeToString(cursorData.getBytes());
    }

    private LocalDateTime decodeDateCursor(String cursor) {
        byte[] decodeBytes = Base64.getDecoder().decode(cursor);
        String decodeString = new String(decodeBytes);
        return LocalDateTime.parse(decodeString);
    }

    public void markAsRead(Long notificationId) {
        User user = userService.getCurrentUser();
        notificationRepository.markAsReadForUser(user.getId(), notificationId);
    }

    public void markAllAsRead() {
        log.info("Mark all as read for current user");
        User user = userService.getCurrentUser();
        notificationRepository.markAllAsReadForUser(user.getId());
    }

    @Override
    public Map<String, Object> getNotificationRedirectInfo(Long notificationId) {
        log.info("Getting redirect info for notification: {}", notificationId);
        
        User currentUser = userService.getCurrentUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        
        // Verify that the notification belongs to the current user
        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Notification not found");
        }
        
        Map<String, Object> redirectInfo = new HashMap<>();
        redirectInfo.put("relatedType", notification.getRelatedType());
        redirectInfo.put("relatedId", notification.getRelatedId());
        
        // Get detailed information based on the type
        switch (notification.getRelatedType()) {
            case POST:
                Post post = postRepository.findById(notification.getRelatedId())
                        .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
                redirectInfo.put("eventId", post.getEvent().getId());
                redirectInfo.put("postId", post.getId());
                redirectInfo.put("redirectPath", "/events/" + post.getEvent().getId() + "/posts/" + post.getId());
                break;
                
            case COMMENT:
                Comment comment = commentRepository.findByIdWithDetails(notification.getRelatedId())
                        .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
                redirectInfo.put("eventId", comment.getPost().getEvent().getId());
                redirectInfo.put("postId", comment.getPost().getId());
                redirectInfo.put("commentId", comment.getId());
                redirectInfo.put("redirectPath", "/events/" + comment.getPost().getEvent().getId() 
                        + "/posts/" + comment.getPost().getId() + "#comment-" + comment.getId());
                break;
                
            case EVENT:
                Event event = eventRepository.findById(notification.getRelatedId())
                        .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
                redirectInfo.put("eventId", event.getId());
                redirectInfo.put("redirectPath", "/events/" + event.getId());
                break;
                
            case REGISTRATION:
                Registration registration = registrationRepository.findById(notification.getRelatedId())
                        .orElseThrow(() -> new ResourceNotFoundException("Registration not found"));
                redirectInfo.put("eventId", registration.getEvent().getId());
                redirectInfo.put("registrationId", registration.getId());
                redirectInfo.put("redirectPath", "/events/" + registration.getEvent().getId() + "/registrations");
                break;
                
            case LIKE:
                Like like = likeRepository.findById(notification.getRelatedId())
                        .orElseThrow(() -> new ResourceNotFoundException("Like not found"));
                
                if (like.getPost() != null) {
                    redirectInfo.put("eventId", like.getPost().getEvent().getId());
                    redirectInfo.put("postId", like.getPost().getId());
                    redirectInfo.put("redirectPath", "/events/" + like.getPost().getEvent().getId() 
                            + "/posts/" + like.getPost().getId());
                } else if (like.getComment() != null) {
                    redirectInfo.put("eventId", like.getComment().getPost().getEvent().getId());
                    redirectInfo.put("postId", like.getComment().getPost().getId());
                    redirectInfo.put("commentId", like.getComment().getId());
                    redirectInfo.put("redirectPath", "/events/" + like.getComment().getPost().getEvent().getId() 
                            + "/posts/" + like.getComment().getPost().getId() + "#comment-" + like.getComment().getId());
                }
                break;
                
            default:
                redirectInfo.put("redirectPath", "/");
        }
        
        // Mark as read when getting redirect info
        markAsRead(notificationId);
        
        return redirectInfo;
    }

    @Async("notificationExecutor")
    @Override
    public void notifyUserAccountDisabled(User user, String reason) {
        log.info("Notifying user {} that their account has been disabled", user.getUsername());
        
        Notification notification = new Notification()
                .setTitle("Account Disabled")
                .setMessage("Your account has been disabled by an administrator. Reason: " + 
                           (reason != null ? reason : "No reason provided"))
                .setUser(user)
                .setRelatedType(Notification.RelatedType.GENERAL)
                .setRelatedId(user.getId());
        
        notificationRepository.save(notification);
        
        NotificationDTO notificationDTO = notificationMapper.toNotificationDTO(notification);
        simpMessagingTemplate.convertAndSendToUser(
                user.getUsername(),
                "/queue/notifications",
                notificationDTO
        );
    }

    @Async("notificationExecutor")
    @Override
    public void notifyUserAccountEnabled(User user) {
        log.info("Notifying user {} that their account has been enabled", user.getUsername());
        
        Notification notification = new Notification()
                .setTitle("Account Enabled")
                .setMessage("Your account has been enabled by an administrator. You can now access all features.")
                .setUser(user)
                .setRelatedType(Notification.RelatedType.GENERAL)
                .setRelatedId(user.getId());
        
        notificationRepository.save(notification);
        
        NotificationDTO notificationDTO = notificationMapper.toNotificationDTO(notification);
        simpMessagingTemplate.convertAndSendToUser(
                user.getUsername(),
                "/queue/notifications",
                notificationDTO
        );
    }

    @Async("notificationExecutor")
    @Override
    public void notifyUserRoleChanged(User user, String oldRole, String newRole) {
        log.info("Notifying user {} of role change from {} to {}", user.getUsername(), oldRole, newRole);
        
        Notification notification = new Notification()
                .setTitle("Role Changed")
                .setMessage("Your role has been changed from " + oldRole + " to " + newRole + " by an administrator.")
                .setUser(user)
                .setRelatedType(Notification.RelatedType.GENERAL)
                .setRelatedId(user.getId());
        
        notificationRepository.save(notification);
        
        NotificationDTO notificationDTO = notificationMapper.toNotificationDTO(notification);
        simpMessagingTemplate.convertAndSendToUser(
                user.getUsername(),
                "/queue/notifications",
                notificationDTO
        );
    }

    @Async("notificationExecutor")
    @Override
    public void notifyUserPasswordReset(User user, String newPassword) {
        log.info("Notifying user {} that their password has been reset", user.getUsername());
        
        Notification notification = new Notification()
                .setTitle("Password Reset")
                .setMessage("Your password has been reset by an administrator. New password: " + newPassword)
                .setUser(user)
                .setRelatedType(Notification.RelatedType.GENERAL)
                .setRelatedId(user.getId());
        
        notificationRepository.save(notification);
        
        NotificationDTO notificationDTO = notificationMapper.toNotificationDTO(notification);
        simpMessagingTemplate.convertAndSendToUser(
                user.getUsername(),
                "/queue/notifications",
                notificationDTO
        );
    }

    @Async("notificationExecutor")
    @Override
    public void notifyUserOnNewComment(User postCreator, Comment comment) {

        String commentCreator = comment.getUser().getUsername();

        Notification notification = new Notification()
                .setTitle("New comment")
                .setMessage(commentCreator + " has commented on your post")
                .setUser(postCreator)
                .setRelatedType(Notification.RelatedType.COMMENT)
                .setRelatedId(comment.getId());

        notificationRepository.save(notification);

        NotificationDTO notificationDTO = notificationMapper.toNotificationDTO(notification);
        simpMessagingTemplate.convertAndSendToUser(
                postCreator.getUsername(),
                "/queue/notifications",
                notificationDTO
        );
    }

    @Async("notificationExecutor")
    @Override
    public void notifyUserOnNewChildComment(User commentCreator, Comment comment) {

        Notification notification = new Notification()
                .setTitle("New replied comment")
                .setMessage(comment.getUser().getUsername() + " has replied your comment")
                .setUser(commentCreator)
                .setRelatedType(Notification.RelatedType.COMMENT)
                .setRelatedId(comment.getId());

        notificationRepository.save(notification);

        NotificationDTO notificationDTO = notificationMapper.toNotificationDTO(notification);
        simpMessagingTemplate.convertAndSendToUser(
                commentCreator.getUsername(),
                "/queue/notifications",
                notificationDTO
        );
    }

}
