package com.example.demo.service;

import com.example.demo.dto.event.EventDTO;
import com.example.demo.dto.registration.RegistrationDTO;
import com.example.demo.model.Registration;
import com.example.demo.model.User;
import com.google.firebase.messaging.FirebaseMessagingException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RegistrationService {
    RegistrationDTO registerEvent(Long eventId);

    Page<RegistrationDTO> getMyRegistrations(Registration.RegistrationStatus status, int page, int size);

    Page<RegistrationDTO> getAllRegistration(Pageable pageable);

    Page<RegistrationDTO> getAllRegistrationByEventId(Long eventId, Pageable pageable);

    int countParticipantsOfAnEvent(Long eventId, Registration.RegistrationStatus registrationStatus);

    Page<EventDTO> getRegisteredEvents(Pageable pageable);

    Page<com.example.demo.dto.event.RegisteredEventDTO> getRegisteredEventsWithStatus(Pageable pageable);

    void approvedRegistration(Long registrationId) throws FirebaseMessagingException;

    void rejectedRegistration(Long registrationId) throws FirebaseMessagingException;

    void cancelRegistration(Long eventId);

    RegistrationDTO findRegistrationByUserIdAndEventId(Long userId, Long eventId);

    RegistrationDTO getCurrentUserRegistrationForEvent(Long eventId);

    RegistrationDTO findRegistrationById(Long registrationId);

    User getEventCreatorFromRegistration(Long registrationId);

    boolean waitingRegistrationRegister(Long eventId);
}
