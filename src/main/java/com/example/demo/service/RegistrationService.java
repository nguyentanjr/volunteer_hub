package com.example.demo.service;

import com.example.demo.dto.event.EventDTO;
import com.example.demo.dto.registration.RegistrationDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Registration;
import com.example.demo.model.User;
import com.google.firebase.messaging.FirebaseMessagingException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RegistrationService {
    RegistrationDTO registerEvent(Long eventId);

    Page<RegistrationDTO> getAllRegistration(Pageable pageable);

    int countParticipantsOfAnEvent(Long eventId, Registration.RegistrationStatus registrationStatus);

    Page<EventDTO> getRegisteredEvents(Pageable pageable);

    Page<RegistrationDTO> getMyRegistrations(Long userId, Pageable pageable);

    void approvedRegistration(Long registrationId) throws FirebaseMessagingException;

    void rejectedRegistration(Long registrationId) throws FirebaseMessagingException;

    void cancelRegistration(Long eventId);

    RegistrationDTO findRegistrationByUserIdAndEventId(Long userId, Long eventId);

    RegistrationDTO findRegistrationById(Long registrationId);

    User getEventCreatorFromRegistration(Long registrationId);

    boolean promoteWaitingRegistration(Long eventId);

    void deleteRegistrationById(Long registrationId);
}
