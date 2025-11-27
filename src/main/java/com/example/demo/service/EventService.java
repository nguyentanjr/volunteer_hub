package com.example.demo.service;

import com.example.demo.dto.event.CreateEventDTO;
import com.example.demo.dto.event.EventDTO;
import com.example.demo.dto.event.EventUpdateDTO;
import com.example.demo.model.Event;
import com.example.demo.model.Registration;
import com.example.demo.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public interface EventService {
    EventDTO createEvent(CreateEventDTO createEventDTO);

    Page<EventDTO> getAllEvents(Pageable pageable);

    Page<EventDTO> getMyEvents(Pageable pageable);

    EventDTO getEventDetails(Long eventId);

    List<EventDTO> getPendingEvents();

    Event save(Event event);

    Event updateEvent(EventUpdateDTO eventUpdateDTO);

    boolean isAvailableForRegistration(Long evenId);

    long getRegistrationCount(Long eventId);

    List<Event> recommendEvent();

    List<String> searchSuggestions(String eventTitle);
}
