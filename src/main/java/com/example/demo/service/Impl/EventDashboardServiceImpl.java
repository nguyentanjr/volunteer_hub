package com.example.demo.service.Impl;

import com.example.demo.dto.dashboard_manager.*;
import com.example.demo.model.Event;
import com.example.demo.model.Post;
import com.example.demo.model.Registration;
import com.example.demo.model.User;
import com.example.demo.repository.*;
import com.example.demo.service.EventDashboardService;
import com.example.demo.service.UserService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Cache;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Getter
@RequiredArgsConstructor
public class EventDashboardServiceImpl implements EventDashboardService {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserService userService;

    @Override
    @Cacheable(value = "dashboard", key = "'manager:' + #root.target.userService.getCurrentUser().id")
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
        //dashboard.setAttendanceReports(getAttendanceReports(managerId));
        
        log.info("Dashboard generated successfully for manager: {}", managerId);
        return dashboard;
    }

    private RegistrationStatisticsDTO getRegistrationStatistics(Long managerId) {

        Integer totalRegistrations = registrationRepository.countAllRegistrationsByManager(managerId);

        Integer pendingRegistrations = registrationRepository.countRegistrationsByManagerAndStatus(
                managerId, Registration.RegistrationStatus.PENDING);
        Integer approvedRegistrations = registrationRepository.countRegistrationsByManagerAndStatus(
                managerId, Registration.RegistrationStatus.APPROVED);
        Integer rejectedRegistrations = registrationRepository.countRegistrationsByManagerAndStatus(
                managerId, Registration.RegistrationStatus.REJECTED);
        Integer cancelledRegistrations = registrationRepository.countRegistrationsByManagerAndStatus(
                managerId, Registration.RegistrationStatus.CANCELLED);;

        Double approvalRate = (totalRegistrations > 0) ? (approvedRegistrations * 100.0 / totalRegistrations ) : 0.0;

        return new RegistrationStatisticsDTO(
                totalRegistrations, pendingRegistrations, approvedRegistrations,
                rejectedRegistrations, cancelledRegistrations, approvalRate);

    }

    private List<EventWithPendingRegistrationsDTO> getEventsNeedingApproval(Long managerId) {



        List<Event> events = eventRepository.findEventsWithPendingRegistrations(managerId);

        return events.stream().map(event -> {
            Long eventId = event.getId();
            String eventTitle = event.getTitle();
            Integer pendingCount = registrationRepository.countPendingByEventId(eventId);
            Integer approvedCount = registrationRepository.countByEventIdAndStatus(eventId,
                    Registration.RegistrationStatus.APPROVED);
            Integer maxParticipants = event.getMaxParticipants();
            LocalDateTime eventDate = event.getDate();
            Boolean isFull = approvedCount >= maxParticipants;

            return new EventWithPendingRegistrationsDTO(
                    eventId, eventTitle, pendingCount, approvedCount, maxParticipants, eventDate, isFull);
        }).toList();

    }

    private List<EventActivityDTO> getRecentActivities(Long managerId) {
        List<EventActivityDTO> activities = new ArrayList<>();

        List<Registration> registrations = registrationRepository.findRecentRegistrationsByManager(
                managerId, PageRequest.of(0, 20));

        for(Registration registration : registrations) {
            EventActivityDTO eventActivityDTO = new EventActivityDTO();
            eventActivityDTO.setEventId(registration.getEvent().getId());
            eventActivityDTO.setEventTitle(registration.getEvent().getTitle());
            eventActivityDTO.setActivityTime(registration.getRegisteredAt());
            if (registration.getStatus() == Registration.RegistrationStatus.CANCELLED) {
                eventActivityDTO.setActivityType(EventActivityDTO.ActivityType.REGISTRATION_CANCELLED);
                eventActivityDTO.setActivityDescription("cancelled registration");
            } else {
                eventActivityDTO.setActivityType(EventActivityDTO.ActivityType.NEW_REGISTRATION);
                eventActivityDTO.setActivityDescription("registered for event");
            }
            activities.add(eventActivityDTO);
        }

        List<Event> events = eventRepository.getMyEvents(managerId);

        for(Event event : events) {
            List<Post> posts = postRepository.getAllPostByEvent(event.getId())
                    .stream()
                    .sorted((p1,p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                    .limit(5)
                    .toList();

            for(Post post : posts) {
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

        return activities
                .stream()
                .sorted((a1, a2) -> a2.getActivityTime().compareTo(a1.getActivityTime()))
                .limit(20)
                .toList();
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
                            false
                    );
                })
                .collect(Collectors.toList());
    }

//    private List<EventAttendanceReportDTO> getAttendanceReports(Long managerId) {
//
//    }
}



