package com.example.demo.service.Impl;

import com.example.demo.dto.dashboard_manager.*;
import com.example.demo.model.Event;
import com.example.demo.model.Post;
import com.example.demo.model.Registration;
import com.example.demo.model.User;
import com.example.demo.repository.*;
import com.example.demo.service.EventDashboardService;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class tan implements EventDashboardService {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserService userService;

    @Override
    public EventDashboardDTO getManagerDashboard() {
        log.info("Getting dashboard for event manager");

        User currentUser = userService.getCurrentUser();
        Long managerId = currentUser.getId();

        EventDashboardDTO dashboard = new EventDashboardDTO();

        // 1. Tổng quan sự kiện
        dashboard.setTotalManagedEvents(eventRepository.countEventsByManager(managerId));
        dashboard.setActiveEvents(eventRepository.countEventsByManagerAndStatus(managerId, Event.EventStatus.ONGOING));
        dashboard.setCompletedEvents(eventRepository.countEventsByManagerAndStatus(managerId, Event.EventStatus.COMPLETED));

        // Count upcoming events (PLANNED status)
        dashboard.setUpcomingEvents(eventRepository.countEventsByManagerAndStatus(managerId, Event.EventStatus.PLANNED));

        // 2. Thống kê đăng ký
        dashboard.setRegistrationStatistics(getRegistrationStatistics(managerId));

        // 3. Sự kiện cần duyệt đăng ký
        dashboard.setEventsNeedingApproval(getEventsNeedingApproval(managerId));

        // 4. Hoạt động gần đây
        dashboard.setRecentActivities(getRecentActivities(managerId));

        // 5. Sự kiện sắp tới cần chuẩn bị (7 ngày tới)
        dashboard.setUpcomingEventsNeedPreparation(getUpcomingEventsNeedPreparation(managerId));

        // 6. Báo cáo tham dự
        dashboard.setAttendanceReports(getAttendanceReports(managerId));

        log.info("Dashboard generated successfully for manager: {}", managerId);
        return dashboard;
    }

    private RegistrationStatisticsDTO getRegistrationStatistics(Long managerId) {
        Integer total = registrationRepository.countAllRegistrationsByManager(managerId);
        Integer pending = registrationRepository.countRegistrationsByManagerAndStatus(
                managerId, Registration.RegistrationStatus.PENDING);
        Integer approved = registrationRepository.countRegistrationsByManagerAndStatus(
                managerId, Registration.RegistrationStatus.APPROVED);
        Integer rejected = registrationRepository.countRegistrationsByManagerAndStatus(
                managerId, Registration.RegistrationStatus.REJECTED);
        Integer cancelled = registrationRepository.countRegistrationsByManagerAndStatus(
                managerId, Registration.RegistrationStatus.CANCELLED);

        Double approvalRate = (total > 0) ? (approved * 100.0 / total) : 0.0;

        return new RegistrationStatisticsDTO(
                total, pending, approved, rejected, cancelled, approvalRate
        );
    }

    private List<EventWithPendingRegistrationsDTO> getEventsNeedingApproval(Long managerId) {
        List<Event> events = eventRepository.findEventsWithPendingRegistrations(managerId);

        return events.stream()
                .map(event -> {
                    Integer pendingCount = registrationRepository.countPendingByEventId(event.getId());
                    Integer approvedCount = registrationRepository.countByEventIdAndStatus(
                            event.getId(), Registration.RegistrationStatus.APPROVED);
                    Boolean isFull = approvedCount >= event.getMaxParticipants();

                    return new EventWithPendingRegistrationsDTO(
                            event.getId(),
                            event.getTitle(),
                            pendingCount,
                            approvedCount,
                            event.getMaxParticipants(),
                            event.getDate(),
                            isFull
                    );
                })
                .collect(Collectors.toList());
    }

    private List<EventActivityDTO> getRecentActivities(Long managerId) {
        List<EventActivityDTO> activities = new ArrayList<>();

        // Get recent registrations (last 20)
        List<Registration> recentRegistrations = registrationRepository
                .findRecentRegistrationsByManager(managerId, PageRequest.of(0, 20));

        for (Registration reg : recentRegistrations) {
            EventActivityDTO activity = new EventActivityDTO();
            activity.setEventId(reg.getEvent().getId());
            activity.setEventTitle(reg.getEvent().getTitle());
            activity.setActivityTime(reg.getRegisteredAt());
            activity.setUserName(reg.getUser().getUsername());

            if (reg.getStatus() == Registration.RegistrationStatus.CANCELLED) {
                activity.setActivityType(EventActivityDTO.ActivityType.REGISTRATION_CANCELLED);
                activity.setActivityDescription("cancelled registration");
            } else {
                activity.setActivityType(EventActivityDTO.ActivityType.NEW_REGISTRATION);
                activity.setActivityDescription("registered for event");
            }

            activities.add(activity);
        }

        // Get recent posts in managed events
        List<Event> managedEvents = eventRepository.getMyEvents(managerId);
        for (Event event : managedEvents) {
            List<Post> recentPosts = postRepository.getAllPostByEvent(event.getId())
                    .stream()
                    .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                    .limit(5)
                    .collect(Collectors.toList());

            for (Post post : recentPosts) {
                EventActivityDTO activity = new EventActivityDTO();
                activity.setEventId(event.getId());
                activity.setEventTitle(event.getTitle());
                activity.setActivityType(EventActivityDTO.ActivityType.NEW_POST);
                activity.setActivityDescription("created a new post");
                activity.setActivityTime(post.getCreatedAt());
                activity.setUserName(post.getPostCreator().getUsername());
                activities.add(activity);
            }
        }

        // Sort by time and return top 20
        return activities.stream()
                .sorted((a1, a2) -> a2.getActivityTime().compareTo(a1.getActivityTime()))
                .limit(20)
                .collect(Collectors.toList());
    }

    private List<UpcomingEventDTO> getUpcomingEventsNeedPreparation(Long managerId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysLater = now.plusDays(7);

        List<Event> upcomingEvents = eventRepository.findUpcomingEventsByManager(
                managerId, now, sevenDaysLater);

        return upcomingEvents.stream()
                .map(event -> {
                    Integer approvedCount = registrationRepository.countByEventIdAndStatus(
                            event.getId(), Registration.RegistrationStatus.APPROVED);

                    long daysUntil = ChronoUnit.DAYS.between(now, event.getDate());

                    return new UpcomingEventDTO(
                            event.getId(),
                            event.getTitle(),
                            event.getDate(),
                            (int) daysUntil,
                            approvedCount,
                            event.getMaxParticipants(),
                            event.getLocation(),
                            event.getStatus(),
                            false  // TODO: Add logic to check preparations
                    );
                })
                .collect(Collectors.toList());
    }

    private List<EventAttendanceReportDTO> getAttendanceReports(Long managerId) {
        List<Event> completedEvents = eventRepository.getMyEvents(managerId).stream()
                .filter(e -> e.getStatus() == Event.EventStatus.COMPLETED)
                .collect(Collectors.toList());

        return completedEvents.stream()
                .map(event -> {
                    Integer totalRegistered = registrationRepository.countByEventId(event.getId());
                    Integer approved = registrationRepository.countByEventIdAndStatus(
                            event.getId(), Registration.RegistrationStatus.APPROVED);

                    // For now, assume all approved participants attended
                    Integer attended = approved;

                    Double attendanceRate = (approved > 0) ? (attended * 100.0 / approved) : 0.0;
                    Double fillRate = (event.getMaxParticipants() > 0) ?
                            (approved * 100.0 / event.getMaxParticipants()) : 0.0;

                    return new EventAttendanceReportDTO(
                            event.getId(),
                            event.getTitle(),
                            event.getDate(),
                            totalRegistered,
                            approved,
                            attended,
                            event.getMaxParticipants(),
                            attendanceRate,
                            fillRate
                    );
                })
                .collect(Collectors.toList());
    }
}

