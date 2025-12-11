package com.example.demo.service.Impl;

import com.example.demo.dto.event.EventDTO;
import com.example.demo.dto.registration.RegistrationDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.EventMapper;
import com.example.demo.mapper.RegistrationMapper;
import com.example.demo.model.Event;
import com.example.demo.model.Registration;
import com.example.demo.model.User;
import com.example.demo.model.UserFcmToken;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.RegistrationRepository;
import com.example.demo.repository.UserFcmTokenRepository;
import com.example.demo.service.FirebaseService;
import com.example.demo.service.NotificationService;
import com.example.demo.service.RegistrationService;
import com.example.demo.service.UserService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final UserService userService;
    private final RegistrationMapper registrationMapper;
    private final EventMapper eventMapper;
    private final NotificationService notificationService;
    private final EventRepository eventRepository;
    private final FirebaseService firebaseService;
    private final UserFcmTokenRepository userFcmTokenRepository;

    @Override
    public Page<RegistrationDTO> getMyRegistrations(Registration.RegistrationStatus status, int page, int size) {
        User currentUser = userService.getCurrentUser();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<Registration> regs = (status == null)
                ? registrationRepository.findByUserId(currentUser.getId(), pageable)
                : registrationRepository.findByUserIdAndStatus(currentUser.getId(), status, pageable);
        return regs.map(registrationMapper::toRegistrationDTO);
    }

    @Caching(evict = {
            @CacheEvict(value = "dashboard", allEntries = true),
    })
    @Transactional
    public RegistrationDTO registerEvent(Long eventId) {
        log.info("Registering event with ID: {} (clearing dashboard cache)", eventId);

        User user = userService.getCurrentUser();

        if (registrationRepository.existsByUserIdAndEventId(user.getId(), eventId)) {
            throw new IllegalStateException("User has already registered for this event.");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        if (event.getStatus() == Event.EventStatus.PLANNED) {
            throw new IllegalStateException("Event is not open for registration.");
        }

        long approvedCount = countParticipantsOfAnEvent(eventId, Registration.RegistrationStatus.APPROVED);

        Registration registration = new Registration()
                .setEvent(event)
                .setUser(user);

        if (approvedCount < event.getMaxParticipants()) {
            registration.setStatus(Registration.RegistrationStatus.PENDING);
            event.setCurrentRegistrationCount(event.getCurrentRegistrationCount() + 1);
        } else {
            registration.setStatus(Registration.RegistrationStatus.WAITING);
        }

        registrationRepository.save(registration);
        eventRepository.save(event);

        // Notify manager about new registration via WebSocket
        notificationService.notifyManagerOnNewRegistration(registration.getId());

        return registrationMapper.toRegistrationDTO(registration);
    }

    public Page<RegistrationDTO> getAllRegistration(Pageable pageable) {
        log.info("Get all registration");
        return registrationRepository.findAll(pageable)
                .map(registrationMapper::toRegistrationDTO);
    }

    public Page<RegistrationDTO> getAllRegistrationByEventId(Long eventId, Pageable pageable) {
        log.info("Get all registrations for event ID: {}", eventId);

        // Check if event exists
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        // Check if current user is the event creator (manager) or admin
        User currentUser = userService.getCurrentUser();
        if (!event.getCreator().getId().equals(currentUser.getId()) &&
                !currentUser.hasRole(com.example.demo.model.Role.RoleName.ADMIN)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You don't have permission to view registrations for this event");
        }

        return registrationRepository.findByEventId(eventId, pageable)
                .map(registrationMapper::toRegistrationDTO);
    }

    public Page<EventDTO> getRegisteredEvents(Pageable pageable) {
        log.info("Get registered events");
        User user = userService.getCurrentUser();
        return registrationRepository.findEventRegistered(user.getId(), pageable)
                .map(eventMapper::toEventDTO);
    }

    public Page<com.example.demo.dto.event.RegisteredEventDTO> getRegisteredEventsWithStatus(Pageable pageable) {
        log.info("Get registered events with status");
        User user = userService.getCurrentUser();
        return registrationRepository.findRegistrationsByUserId(user.getId(), pageable)
                .map(registration -> {
                    Event event = registration.getEvent();
                    com.example.demo.dto.event.RegisteredEventDTO dto = new com.example.demo.dto.event.RegisteredEventDTO();
                    dto.setEventId(event.getId());
                    dto.setTitle(event.getTitle());
                    dto.setDescription(event.getDescription());
                    dto.setDate(event.getDate());
                    dto.setLocation(event.getLocation());
                    dto.setStatus(event.getStatus());
                    dto.setMaxParticipants(event.getMaxParticipants());
                    dto.setCreatedAt(event.getCreatedAt());
                    dto.setCreatorUsername(event.getCreator().getUsername());
                    dto.setRegistrationStatus(registration.getStatus());
                    return dto;
                });
    }

    @CacheEvict(value = "dashboard", key = "'manager:' + @userService.getCurrentUser().id")
    public void approvedRegistration(Long registrationId) {
        Registration registration = registrationRepository.findRegistrationById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found"));

        Event event = registration.getEvent();
        User volunteer = registration.getUser();

        registration.setStatus(Registration.RegistrationStatus.APPROVED);
        registrationRepository.save(registration);

        // Send WebSocket notification to volunteer
        notificationService.notifyVolunteerOnRegistrationApproved(volunteer, event, registrationId);

        // Send FCM notification (if token exists)
        List<UserFcmToken> tokens = userFcmTokenRepository.findAllByUser(volunteer);
        if (!tokens.isEmpty()) {
            Map<String, String> data = new HashMap<>();
            data.put("type", "REGISTRATION_APPROVED");
            data.put("eventId", event.getId().toString());
            data.put("eventTitle", event.getTitle());
            data.put("registrationId", registrationId.toString());
            data.put("click_action", "FLUTTER_NOTIFICATION_CLICK");

            Notification notification = Notification.builder()
                    .setTitle("Registration Approved")
                    .setBody("Your registration for event '" + event.getTitle() + "' has been approved!")
                    .build();

            for (UserFcmToken userFcmToken : tokens) {
                try {
                    Message message = Message.builder()
                            .setToken(userFcmToken.getToken())
                            .setNotification(notification)
                            .putAllData(data)
                            .build();

                    String response = FirebaseMessaging.getInstance().send(message);

                    log.info("FCM success for volunteer {} token {}: {}",
                            volunteer.getId(), userFcmToken.getToken(), response);

                } catch (FirebaseMessagingException e) {
                    log.error("FCM FAILED for volunteer {} token {}: code={}, msg={}",
                            volunteer.getId(),
                            userFcmToken.getToken(),
                            e.getMessagingErrorCode(),
                            e.getMessage(),
                            e);
                }
            }
        }

    }

    @CacheEvict(value = "dashboard", key = "'manager:' + @userService.getCurrentUser().id")
    public void rejectedRegistration(Long registrationId) throws FirebaseMessagingException {
        Registration registration = registrationRepository.findRegistrationById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found"));

        Event event = registration.getEvent();
        User volunteer = registration.getUser();

        // Update registration status
        registration.setStatus(Registration.RegistrationStatus.REJECTED);
        registrationRepository.save(registration);

        // Send WebSocket notification to volunteer
        notificationService.notifyVolunteerOnRegistrationRejected(volunteer, event, registrationId);

        // Send FCM notification (if token exists)
        List<UserFcmToken> tokens = userFcmTokenRepository.findAllByUser(volunteer);
        if (!tokens.isEmpty()) {
            Map<String, String> data = new HashMap<>();
            data.put("type", "REGISTRATION_REJECTED");
            data.put("eventId", event.getId().toString());
            data.put("eventTitle", event.getTitle());
            data.put("registrationId", registrationId.toString());
            data.put("click_action", "FLUTTER_NOTIFICATION_CLICK");

            Notification notification = Notification.builder()
                    .setTitle("Registration Rejected")
                    .setBody("Your registration for event '" + event.getTitle() + "' has been rejected!")
                    .build();

            for (UserFcmToken userFcmToken : tokens) {
                try {
                    Message message = Message.builder()
                            .setToken(userFcmToken.getToken())
                            .setNotification(notification)
                            .putAllData(data)
                            .build();

                    String response = FirebaseMessaging.getInstance().send(message);

                    log.info("FCM success for volunteer {} token {}: {}",
                            volunteer.getId(), userFcmToken.getToken(), response);

                } catch (FirebaseMessagingException e) {
                    log.error("FCM FAILED for volunteer {} token {}: code={}, msg={}",
                            volunteer.getId(),
                            userFcmToken.getToken(),
                            e.getMessagingErrorCode(),
                            e.getMessage(),
                            e);
                }
            }
        }
    }

    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true)
    public void cancelRegistration(Long eventId) {
        log.info("Cancel registration for event: {} (clearing dashboard cache)", eventId);

        Event event = eventRepository.getEventById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        // Nếu cần check thời gian event:
        // if (event.getDate().isBefore(LocalDateTime.now())) {
        //     throw new IllegalStateException("You cannot cancel the registration because it's happening");
        // }

        User user = userService.getCurrentUser();

        if (!registrationRepository.existsByUserIdAndEventId(user.getId(), eventId)) {
            throw new IllegalStateException("You haven't registered yet!");
        }

        Registration registration = registrationRepository
                .findRegistrationByUserIdAndEventId(user.getId(), eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found"));

        registrationRepository.delete(registration);

        int currentCount = event.getCurrentRegistrationCount();
        event.setCurrentRegistrationCount(currentCount - 1);

        log.info("User {} cancelled registration for event {}", user.getId(), eventId);

        boolean promoted = waitingRegistrationRegister(eventId);

        if (promoted) {
            event.setCurrentRegistrationCount(event.getCurrentRegistrationCount() + 1);
        }

        eventRepository.save(event);
    }

    @Override
    public boolean waitingRegistrationRegister(Long eventId) {
        return registrationRepository.findEarliestWaitingRegistration(eventId)
                .stream()
                .findFirst()
                .map(registration -> {
                    registration.setStatus(Registration.RegistrationStatus.PENDING);
                    registrationRepository.save(registration);
                    return true;
                })
                .orElse(false);
    }

    public RegistrationDTO findRegistrationByUserIdAndEventId(Long userId, Long eventId) {
        Registration registration = registrationRepository.findRegistrationByUserIdAndEventId(userId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found by userId and eventId"));
        return registrationMapper.toRegistrationDTO(registration);
    }

    public RegistrationDTO getCurrentUserRegistrationForEvent(Long eventId) {
        User currentUser = userService.getCurrentUser();
        Optional<Registration> registration = registrationRepository.findRegistrationByUserIdAndEventId(
                currentUser.getId(), eventId);

        if (registration.isPresent()) {
            return registrationMapper.toRegistrationDTO(registration.get());
        }

        // Return null DTO if not registered
        return null;
    }

    public RegistrationDTO findRegistrationById(Long registrationId) {
        Registration registration = registrationRepository.findRegistrationById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found by Id"));
        return registrationMapper.toRegistrationDTO(registration);
    }

    @Override
    public User getEventCreatorFromRegistration(Long registrationId) {
        log.info("Get event creator from registration: {}", registrationId);
        RegistrationDTO registrationDTO = findRegistrationById(registrationId);
        Event event = eventRepository.getEventById(registrationDTO.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        return event.getCreator();
    }

    public int countParticipantsOfAnEvent(Long eventId, Registration.RegistrationStatus registrationStatus) {
        return registrationRepository.countByEventIdAndStatus(eventId, registrationStatus);
    }
}
