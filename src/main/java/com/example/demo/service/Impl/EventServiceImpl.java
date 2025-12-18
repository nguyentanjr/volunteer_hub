package com.example.demo.service.Impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.demo.dto.event.CreateEventDTO;
import com.example.demo.dto.event.EventDTO;
import com.example.demo.dto.event.EventUpdateDTO;
import com.example.demo.exception.BaseException;
import com.example.demo.exception.EventDuplicateException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UnauthorizedException;
import com.example.demo.mapper.EventMapper;
import com.example.demo.model.Event;
import com.example.demo.model.Registration;
import com.example.demo.model.Tag;
import com.example.demo.model.User;
import com.example.demo.repository.*;
import com.example.demo.service.*;
import com.example.demo.utils.QRCodeGenerator;
import com.google.zxing.WriterException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class EventServiceImpl implements EventService {

    private final EventMapper eventMapper;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final TagService tagService;
    private final Cloudinary cloudinary;

    @Override
    @Caching(evict = {
            @CacheEvict(value = "dashboard", allEntries = true),
            @CacheEvict(value = "events", allEntries = true),
    })
    public EventDTO createEvent(CreateEventDTO createEventDTO) {
        log.info("Creating event: title={}", createEventDTO.getTitle());

        validateEventCreation(createEventDTO);
       // checkEventDuplication(createEventDTO);


        User user = getCurrentUser();
        Event event = buildEventFromDTO(createEventDTO, user);
        Event savedEvent = eventRepository.save(event);
        EventDTO eventDTO = eventMapper.toEventDTO(savedEvent);
        // New event has 0 participants
        eventDTO.setCurrentParticipants(0);
        handleEventCreationSideEffects(eventDTO);

        log.info("Event created successfully: id={}, title={}", savedEvent.getId(), savedEvent.getTitle());
        return eventDTO;
    }

    private void checkEventDuplication(CreateEventDTO createEventDTO) {
        boolean eventExists = eventRepository.findByTitleAndDescriptionAndDateAndLocationAndMaxParticipants(
                createEventDTO.getTitle(),
                createEventDTO.getDescription(),
                createEventDTO.getDate(),
                createEventDTO.getLocation(),
                createEventDTO.getMaxParticipants()).isPresent();

        if (eventExists) {
            throw new EventDuplicateException("Event with same details already exists");
        }
    }

    private Event buildEventFromDTO(CreateEventDTO createEventDTO, User creator) {
        Event event = eventMapper.toEvent(createEventDTO);
        Set<Tag> tags = createEventDTO.getTags().stream()
                        .map(tag -> tagRepository
                                        .findByName(tag)
                                        .orElseThrow(() -> new ResourceNotFoundException("Tag not found")))
                                .collect(Collectors.toSet());
        event.setStatus(Event.EventStatus.PLANNED);
        event.setCreator(creator);
        event.setTags(tags);

        return event;
    }

    public boolean isAvailableForRegistration(Long eventId) {
        log.info("Check if event is available for registering");
        Event event = eventRepository.getEventById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        long numberOfParticipants = registrationRepository.countByEventIdAndStatus(eventId, Registration.RegistrationStatus.APPROVED);
        return (!event.getStatus().equals(Event.EventStatus.ONGOING));
    }

    @Override
    public Page<EventDTO> getAllEvents(Pageable pageable) {
        log.info("Get event titles");
        return eventRepository.findAllEvent(pageable).map(event -> {
            EventDTO dto = eventMapper.toEventDTO(event);
            // Calculate real-time count from registrations
            int actualCount = calculateParticipantCount(event.getId());
            dto.setCurrentParticipants(actualCount);
            return dto;
        });
    }

    @Override
    public Page<EventDTO> getMyEvents(Pageable pageable) {
        log.info("Get my events");
        User user = getCurrentUser();
        return eventRepository.getMyEvents(user.getId(), pageable)
                .map(event -> {
                    EventDTO dto = eventMapper.toEventDTO(event);
                    // Calculate real-time count from registrations
                    int actualCount = calculateParticipantCount(event.getId());
                    dto.setCurrentParticipants(actualCount);
                    return dto;
                });
    }

    @Override
    // Disable cache to ensure participant count is always real-time
    // Cache is evicted when registrations change, but to be safe, we calculate count fresh each time
    public EventDTO getEventDetails(Long eventId) {
        log.info("Get event details with ID : {}", eventId);
        Event event = eventRepository.getEventById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        EventDTO dto = eventMapper.toEventDTO(event);
        // Calculate real-time count from registrations (always fresh, not cached)
        int actualCount = calculateParticipantCount(eventId);
        dto.setCurrentParticipants(actualCount);
        return dto;
    }

    /**
     * Calculate actual participant count from registrations (APPROVED + PENDING)
     * This ensures count is always accurate, regardless of DB field sync
     */
    private int calculateParticipantCount(Long eventId) {
        int approvedCount = registrationRepository.countByEventIdAndStatus(eventId, Registration.RegistrationStatus.APPROVED);
        int pendingCount = registrationRepository.countByEventIdAndStatus(eventId, Registration.RegistrationStatus.PENDING);
        return approvedCount + pendingCount;
    }

    @Override
    @Cacheable(value = "events", key = "'pending'")
    public List<EventDTO> getPendingEvents() {
        log.info("Get pending events");
        return eventRepository.getPendingEvents().stream().map(event -> {
            EventDTO dto = eventMapper.toEventDTO(event);
            // Calculate real-time count from registrations
            int actualCount = calculateParticipantCount(event.getId());
            dto.setCurrentParticipants(actualCount);
            return dto;
        }).toList();
    }


    @Override
    public Event save(Event event) {
        log.info("Save event");
        return eventRepository.save(event);
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = "eventDetails", key = "#eventUpdateDTO.id"),
                    @CacheEvict(value = "events", allEntries = true),
                    @CacheEvict(value = "recommendations", allEntries = true)
            }
    )
    public Event updateEvent(EventUpdateDTO eventUpdateDTO) {
        log.info("Update event: {}", eventUpdateDTO.getTitle());
        Event event = new Event()
                .setDate(eventUpdateDTO.getDate())
                .setDescription(eventUpdateDTO.getDescription())
                .setLocation(eventUpdateDTO.getLocation())
                .setTitle(eventUpdateDTO.getTitle())
                .setMaxParticipants(eventUpdateDTO.getMaxParticipants());
        notificationService.notifyVolunteerOnEventUpdated(event);
        return eventRepository.save(event);
    }



    @Override
    public long getRegistrationCount(Long eventId) {
        return registrationRepository.countByEventId(eventId);
    }


    private void validateEventCreation(CreateEventDTO createEventDTO) {
        log.debug("Validating event creation data");

        if (createEventDTO.getDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Event date cannot be in the past");
        }

        if (createEventDTO.getMaxParticipants() <= 0) {
            throw new IllegalArgumentException("Maximum participants must be greater than 0");
        }
    }

    private void handleEventCreationSideEffects(EventDTO eventDTO) {
        log.info("Send notification to admin");
        notificationService.notifyAdminsOfNewEvent(eventDTO);
    }

    public User getCurrentUser() {
        String username;
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            username =  ((UserDetails) principal).getUsername();
        }
        else {
            username = principal.toString();
        }
        return userRepository.getUserByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    @Transactional(readOnly = true)
    // DON'T cache entities! They have lazy-loaded relationships
    // Cache at the DTO level instead (in VolunteerDashboardService)
    public List<Event> recommendEvent() {
        List<Event> candidates = eventRepository.findCandidateEvents(PageRequest.of(0,200));
        return candidates
                .stream()
                .sorted(Comparator.comparingDouble(this::calculateEventScore).reversed())
                .limit(5)
                .toList();
    }

    /*
    Event's Dashboard helper method
     */

    private double calculateEventScore(Event event) {
        User user = getCurrentUser();
        double tagScore = tagService.calculateTagScore(user, event);
        double capacityScore = calculateCapacityScore(event);
        double popularityScore = calculatePopularityScore(event);
        double organizerScore = calculateOrganizerScore(event.getCreator());
        double timeScore = calculateTimeScore(event);

        return 0.4 * tagScore
                + 0.1 * capacityScore
                + 0.2 * popularityScore
                + 0.1 * organizerScore
                + 0.2 * timeScore;
    }

    private double calculateCapacityScore(Event event) {
        if (event.getMaxParticipants() == 0) return 0.0;
        int approvedCount = registrationRepository
                .countRegistrationsByEventIdAndStatus(event.getId(), Registration.RegistrationStatus.APPROVED);
        return 1 - Math.min(1.0, (double) approvedCount / event.getMaxParticipants());
    }

    private double calculatePopularityScore(Event event) {
        int likes = likeRepository.countAllLikeCommentsByEventId(event.getId()) +
                likeRepository.countAllLikePostsByEventId(event.getId());
        int comments = commentRepository.countAllCommentByEventId(event.getId());
        int participants = event.getMaxParticipants();

        double score = likes * 0.4 + comments * 0.3 + participants * 0.3;
        return Math.min(score / 100.0, 1.0);
    }

    private double calculateOrganizerScore(User organizer) {
        int totalEvents = organizer.getEvents().size();
        if (totalEvents == 0) return 0.0;

        long successful = organizer.getEvents().stream()
                .filter(event -> event.getStatus() == Event.EventStatus.COMPLETED)
                .count();

        return (double) successful / totalEvents;
    }

    private double calculateTimeScore(Event event) {
        LocalDateTime now = LocalDateTime.now();
        long daysDiff = ChronoUnit.DAYS.between(now, event.getDate());
        if (daysDiff <= 0 || daysDiff >= 30) return 0.0;
        return 1 - (daysDiff / 30.0);
    }

    /*
    ====================================================================
     */


    public String createEventQRCode(Long eventId) throws WriterException, IOException {
        String qrText = "http://localhost/events/qrcode/" + eventId;
        ByteArrayOutputStream baos = QRCodeGenerator.generateQRCodeByteArray(qrText, 300, 300);

        Map uploadResult = cloudinary.uploader().upload(baos.toByteArray(),
                ObjectUtils.asMap("public_id", "qr_event_" + eventId));

        return uploadResult.get("secure_url").toString();
    }

    @Override
    public List<String> searchSuggestions(String eventTitle) {
        return eventRepository.findTop5ByTitleContainingIgnoreCase(eventTitle)
                .stream()
                .map(Event::getTitle)
                .toList();
    }

    @Override
    @Transactional
    public void syncEventParticipantCount(Long eventId) {
        log.info("Syncing participant count for event: {}", eventId);
        
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        
        // Count APPROVED + PENDING registrations (those who occupy slots)
        int approvedCount = registrationRepository.countByEventIdAndStatus(eventId, Registration.RegistrationStatus.APPROVED);
        int pendingCount = registrationRepository.countByEventIdAndStatus(eventId, Registration.RegistrationStatus.PENDING);
        int actualCount = approvedCount + pendingCount;
        
        log.info("Event {}: Current count in DB = {}, Actual count (APPROVED+PENDING) = {}", 
                 eventId, event.getCurrentRegistrationCount(), actualCount);
        
        if (event.getCurrentRegistrationCount() != actualCount) {
            event.setCurrentRegistrationCount(actualCount);
            eventRepository.save(event);
            log.info("Synced event {} participant count from {} to {}", 
                     eventId, event.getCurrentRegistrationCount(), actualCount);
        }
    }
}
