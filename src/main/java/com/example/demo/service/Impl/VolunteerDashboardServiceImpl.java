package com.example.demo.service.Impl;

import com.example.demo.dto.dashboard_volunteer.*;
import com.example.demo.mapper.EventMapper;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.EventService;
import com.example.demo.service.UserService;
import com.example.demo.service.VolunteerDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class VolunteerDashboardServiceImpl implements VolunteerDashboardService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final UserService userService;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final RegistrationRepository registrationRepository;
    private final EventService eventService;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard", key = "'volunteer:' + #root.target.getCurrentUser().id")
    public VolunteerDashBoardDTO getVolunteerDashBoard() {
        log.info("Getting dashboard for volunteer");

        User currentUser = userService.getCurrentUser();
        Long userId = currentUser.getId();

        CompletableFuture<List<RecentActivityDTO>> recentFuture = getRecentActivitiesAsync(currentUser);
        CompletableFuture<List<RegisteredEventDTO>> registeredFuture = getRegisteredEventAsync(currentUser);
        CompletableFuture<List<UpcomingEventDTO>> upcomingFuture = getUpcomingEventAsync(userId);
        CompletableFuture<List<EventParticipationHistoryDTO>> historyFuture = getEventParticipationHistoryAsync(currentUser);

        CompletableFuture.allOf(recentFuture, registeredFuture, upcomingFuture, historyFuture).join();

        VolunteerDashBoardDTO dashboard = new VolunteerDashBoardDTO();
        dashboard.setRecentActivityDTOS(recentFuture.join());
        dashboard.setRegisteredEventDTOS(registeredFuture.join());
        dashboard.setUpcomingEventDTOS(upcomingFuture.join());
        dashboard.setEventParticipationHistoryDTOS(historyFuture.join());

        log.info("Dashboard generated successfully for volunteer: {}", userId);
        return dashboard;
    }

    @Async("dashboardExecutor")
    public CompletableFuture<List<RecentActivityDTO>> getRecentActivitiesAsync(User user) {
        return CompletableFuture.completedFuture(getRecentActivities(user));
    }

    @Async("dashboardExecutor")
    public CompletableFuture<List<RegisteredEventDTO>> getRegisteredEventAsync(User user) {
        return CompletableFuture.completedFuture(getRegisteredEvent(user));
    }

    @Async("dashboardExecutor")
    public CompletableFuture<List<UpcomingEventDTO>> getUpcomingEventAsync(Long userId) {
        return CompletableFuture.completedFuture(getUpcomingEvent(userId));
    }

    @Async("dashboardExecutor")
    public CompletableFuture<List<EventParticipationHistoryDTO>> getEventParticipationHistoryAsync(User user) {
        return CompletableFuture.completedFuture(getEventParticipationHistory(user));
    }


    private List<RecommendEventDTO> getRecommendEvent() {
        log.info("RecommendEventDTO");

        return eventService.recommendEvent()
                .stream()
                .map(event -> {
                    RecommendEventDTO dto = eventMapper.toRecommendEventDTO(event);
                    // Calculate real-time count from registrations
                    int approvedCount = registrationRepository.countByEventIdAndStatus(event.getId(), Registration.RegistrationStatus.APPROVED);
                    int pendingCount = registrationRepository.countByEventIdAndStatus(event.getId(), Registration.RegistrationStatus.PENDING);
                    dto.setCurrentParticipants(approvedCount + pendingCount);
                    return dto;
                })
                .toList();
    }

    private List<EventParticipationHistoryDTO> getEventParticipationHistory(User user) {
        log.info("EventParticipationHistoryDTO");

        return eventRepository
                .findEventsByVolunteerAndStatus(user.getId(), Event.EventStatus.COMPLETED)
                .stream()
                .map(eventMapper::toEventParticipationHistoryDTO)
                .toList();
    }

    private List<RegisteredEventDTO> getRegisteredEvent(User user) {
        log.info("RegisteredEventDTO");

        return eventRepository
                .findEventsByVolunteerAndStatus(user.getId(), Event.EventStatus.ONGOING)
                .stream()
                .map(event -> {
                    RegisteredEventDTO dto = eventMapper.toRegisteredEventDTO(event);
                    // Calculate real-time count from registrations
                    int approvedCount = registrationRepository.countByEventIdAndStatus(event.getId(), Registration.RegistrationStatus.APPROVED);
                    int pendingCount = registrationRepository.countByEventIdAndStatus(event.getId(), Registration.RegistrationStatus.PENDING);
                    dto.setCurrentParticipants(approvedCount + pendingCount);
                    return dto;
                })
                .toList();
    }

    private List<UpcomingEventDTO> getUpcomingEvent(Long userId) {
        log.info("UpcomingEventDTO");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysLater = LocalDateTime.now().plusDays(7);
        return eventRepository.findUpcomingEvents(now, sevenDaysLater, userId)
                .stream()
                .map(event -> {
                    UpcomingEventDTO dto = eventMapper.toUpComingEventDTO(event);
                    // Calculate real-time count from registrations
                    int approvedCount = registrationRepository.countByEventIdAndStatus(event.getId(), Registration.RegistrationStatus.APPROVED);
                    int pendingCount = registrationRepository.countByEventIdAndStatus(event.getId(), Registration.RegistrationStatus.PENDING);
                    dto.setCurrentParticipants(approvedCount + pendingCount);
                    return dto;
                })
                .toList();
    }

    private List<RecentActivityDTO> getRecentActivities(User user) {

        List<RecentActivityDTO> activities = new ArrayList<>();

        log.info("posts");

        List<Post> posts = postRepository.findPostsByUserId(user.getId(), PageRequest.of(0, 20));
        for (Post post : posts) {

            RecentActivityDTO activityDTO = new RecentActivityDTO()
                    .setActivityType(RecentActivityDTO.ActivityType.POST)
                    .setCreatedAt(post.getCreatedAt())
                    .setEventId(post.getEvent().getId())
                    .setEventTitle(post.getEvent().getTitle())
                    .setActivityDescription("new post");
            activities.add(activityDTO);
        }

        System.out.println(activities);

        log.info("comments");

        List<Comment> comments = commentRepository.findCommentsByUserId(user.getId(), PageRequest.of(0, 20));
        for (Comment comment : comments) {

            RecentActivityDTO activityDTO = new RecentActivityDTO()
                    .setActivityType(RecentActivityDTO.ActivityType.COMMENT)
                    .setCreatedAt(comment.getCreatedAt())
                    .setEventId(comment.getPost().getEvent().getId())
                    .setEventTitle(comment.getPost().getEvent().getTitle())
                    .setActivityDescription("new comment");
            activities.add(activityDTO);
        }

        log.info("likesPost");

        List<Like> likesPost = likeRepository.findLikesPostByUserId(user.getId(), PageRequest.of(0, 20));
        for (Like like : likesPost) {

            RecentActivityDTO activityDTO = new RecentActivityDTO()
                    .setActivityType(RecentActivityDTO.ActivityType.LIKE_POST)
                    .setCreatedAt(like.getCreatedAt())
                    .setEventId(like.getPost().getEvent().getId())
                    .setEventTitle(like.getPost().getEvent().getTitle())
                    .setActivityDescription("new like on post");
            activities.add(activityDTO);
        }

        log.info("likesComment");

        List<Like> likesComment = likeRepository.findLikesCommentByUserId(user.getId(), PageRequest.of(0, 20));
        for (Like like : likesComment) {

            RecentActivityDTO activityDTO = new RecentActivityDTO()
                    .setActivityType(RecentActivityDTO.ActivityType.LIKE_POST)
                    .setCreatedAt(like.getCreatedAt())
                    .setEventId(like.getComment().getPost().getEvent().getId())
                    .setEventTitle(like.getComment().getPost().getEvent().getTitle())
                    .setActivityDescription("new like on comment");
            activities.add(activityDTO);
        }

        log.info("pendingRegistrations");

        List<Registration> pendingRegistrations = registrationRepository.findRecentRegistrations(
                user.getId(), Registration.RegistrationStatus.PENDING, PageRequest.of(0, 20));
        for (Registration registration : pendingRegistrations) {

            RecentActivityDTO activityDTO = new RecentActivityDTO()
                    .setActivityType(RecentActivityDTO.ActivityType.PENDING_REGISTRATION)
                    .setCreatedAt(registration.getRegisteredAt())
                    .setEventId(registration.getEvent().getId())
                    .setEventTitle(registration.getEvent().getTitle())
                    .setActivityDescription("new pending registration");
            activities.add(activityDTO);
        }

        log.info("approvedRegistrations");

        List<Registration> approvedRegistrations = registrationRepository.findRecentRegistrations(
                user.getId(), Registration.RegistrationStatus.APPROVED, PageRequest.of(0, 20));
        for (Registration registration : approvedRegistrations) {

            RecentActivityDTO activityDTO = new RecentActivityDTO()
                    .setActivityType(RecentActivityDTO.ActivityType.APPROVED_REGISTRATION)
                    .setCreatedAt(registration.getRegisteredAt())
                    .setEventId(registration.getEvent().getId())
                    .setEventTitle(registration.getEvent().getTitle())
                    .setActivityDescription("new approved registration");
            activities.add(activityDTO);
        }
        return activities
                .stream()
                .sorted((a1, a2) -> a2.getCreatedAt().compareTo(a1.getCreatedAt()))
                .limit(6)
                .toList();
    }

    public User getCurrentUser() {
        return userService.getCurrentUser();
    }

}
