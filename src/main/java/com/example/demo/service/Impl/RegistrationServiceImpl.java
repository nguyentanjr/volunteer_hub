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
import com.example.demo.service.*;
import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

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

        return registrationMapper.toRegistrationDTO(registration);
    }


    public Page<RegistrationDTO> getAllRegistration(Pageable pageable) {
        log.info("Get all registration");
        return registrationRepository.findAll(pageable)
                .map(registrationMapper::toRegistrationDTO);
    }

    public Page<EventDTO> getRegisteredEvents(Pageable pageable) {
        log.info("Get registered events");
        User user = userService.getCurrentUser();
        return registrationRepository.findEventRegistered(user.getId(), pageable)
                .map(eventMapper::toEventDTO);
    }

    @CacheEvict(value = "dashboard", key = "'manager:' + @userService.getCurrentUser().id")
    public void approvedRegistration(Long registrationId) throws FirebaseMessagingException {
        Registration registration = registrationRepository.findRegistrationById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found"));

        Event event = registration.getEvent();

        UserFcmToken user = userFcmTokenRepository.findByUser(registration.getUser())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        firebaseService.sendToToken(
                user.getToken(),
                "Registration successfully",
                "Your registration on event '"+ event.getTitle() + "' has been approved");

        registration.setStatus(Registration.RegistrationStatus.APPROVED);
        registrationRepository.save(registration);
    }

    @CacheEvict(value = "dashboard", key = "'manager:' + @userService.getCurrentUser().id")
    public void rejectedRegistration(Long registrationId) throws FirebaseMessagingException {
        Registration registration = registrationRepository.findRegistrationById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found"));

        Event event = registration.getEvent();

        UserFcmToken user = userFcmTokenRepository.findByUser(registration.getUser())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        firebaseService.sendToToken(
                user.getToken(),
                "Registration failed",
                "Your registration on event '"+ event.getTitle() + "' has been rejected");

        registration.setStatus(Registration.RegistrationStatus.REJECTED);
        registrationRepository.save(registration);
    }

    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true)
    public void cancelRegistration(Long eventId) {
        log.info("Cancel registration for event: {} (clearing dashboard cache)", eventId);
        Event event = eventRepository.getEventById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        if(event.getDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("You cannot cancel the registration because it's is happening");
        }

        User user = userService.getCurrentUser();
        if(!registrationRepository.existsByUserIdAndEventId(user.getId(), eventId)) {
            throw new IllegalStateException("You haven't registered yet!");
        }

        Registration registration = registrationRepository.findRegistrationByUserIdAndEventId(user.getId(), eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found"));

        registration.setStatus(Registration.RegistrationStatus.CANCELLED);
        registrationRepository.save(registration);

        event.setCurrentRegistrationCount(event.getCurrentRegistrationCount() - 1);
        eventRepository.save(event);

        notificationService.notifyManagerOnUserRegistrationCancelled(registration.getId());
        log.info("User {} cancelled registration for event {}", user.getId(), eventId);

        waitingRegistrationRegister(eventId);
        event.setCurrentRegistrationCount(event.getCurrentRegistrationCount() + 1);

    }

    @Override
    public void waitingRegistrationRegister(Long eventId) {
        Registration waitingRegistration = registrationRepository.findEarliestWaitingRegistration(eventId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No waiting registration found"));
        waitingRegistration.setStatus(Registration.RegistrationStatus.PENDING);
        registrationRepository.save(waitingRegistration);
    }

    public RegistrationDTO findRegistrationByUserIdAndEventId(Long userId, Long eventId) {
        Registration registration = registrationRepository.findRegistrationByUserIdAndEventId(userId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found by userId and eventId"));
        return registrationMapper.toRegistrationDTO(registration);
    }

    @Override
    public RegistrationDTO getCurrentUserRegistrationStatus(Long eventId) {
        User user = userService.getCurrentUser();
        return registrationRepository.findRegistrationByUserIdAndEventId(user.getId(), eventId)
                .map(registrationMapper::toRegistrationDTO)
                .orElse(null);
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
