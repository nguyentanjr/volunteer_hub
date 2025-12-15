package com.example.demo.service.Impl;

import com.example.demo.model.Event;
import com.example.demo.model.Registration;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.RegistrationRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ExportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {
    
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    
    @Override
    public byte[] exportEventsToCSV() throws IOException {
        log.info("Exporting all events to CSV");
        List<Event> events = eventRepository.findAll();
        return generateEventsCSV(events);
    }
    
    @Override
    public byte[] exportEventsToJSON() throws IOException {
        log.info("Exporting all events to JSON");
        List<Event> events = eventRepository.findAll();
        return generateEventsJSON(events);
    }
    
    @Override
    public byte[] exportUsersToCSV() throws IOException {
        log.info("Exporting all users to CSV");
        List<User> users = userRepository.findAll();
        return generateUsersCSV(users);
    }
    
    @Override
    public byte[] exportUsersToJSON() throws IOException {
        log.info("Exporting all users to JSON");
        List<User> users = userRepository.findAll();
        return generateUsersJSON(users);
    }
    
    @Override
    public byte[] exportEventsToCSV(String status, String startDate, String endDate) throws IOException {
        log.info("Exporting filtered events to CSV - status: {}, startDate: {}, endDate: {}", 
                status, startDate, endDate);
        
        List<Event> events = eventRepository.findAll();
        
        if (status != null && !status.isEmpty()) {
            Event.EventStatus eventStatus = Event.EventStatus.valueOf(status.toUpperCase());
            events = events.stream()
                    .filter(e -> e.getStatus() == eventStatus)
                    .collect(Collectors.toList());
        }
        
        if (startDate != null && !startDate.isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
            events = events.stream()
                    .filter(e -> e.getDate().isAfter(start))
                    .collect(Collectors.toList());
        }
        
        if (endDate != null && !endDate.isEmpty()) {
            LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");
            events = events.stream()
                    .filter(e -> e.getDate().isBefore(end))
                    .collect(Collectors.toList());
        }
        
        return generateEventsCSV(events);
    }
    
    @Override
    public byte[] exportUsersToCSV(String role, Boolean enabled) throws IOException {
        log.info("Exporting filtered users to CSV - role: {}, enabled: {}", role, enabled);
        
        List<User> users = userRepository.findAll();
        
        // Apply filters
        if (role != null && !role.isEmpty()) {
            Role.RoleName userRole = Role.RoleName.valueOf(role.toUpperCase());
            users = users.stream()
                    .filter(u -> u.hasRole(userRole))
                    .collect(Collectors.toList());
        }
        
        if (enabled != null) {
            users = users.stream()
                    .filter(u -> u.isEnabled() == enabled)
                    .collect(Collectors.toList());
        }
        
        return generateUsersCSV(users);
    }
    
    
    private byte[] generateEventsCSV(List<Event> events) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);
        
        // CSV Header
        writer.println("ID,Title,Description,Date,Location,Status,Max Participants,Current Registrations,Creator,Created At");
        
        // CSV Data
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (Event event : events) {
            int registrationCount = registrationRepository.countByEventId(event.getId());
            
            writer.printf("%d,\"%s\",\"%s\",%s,\"%s\",%s,%d,%d,\"%s\",%s%n",
                    event.getId(),
                    escapeCsv(event.getTitle()),
                    escapeCsv(event.getDescription()),
                    event.getDate().format(formatter),
                    escapeCsv(event.getLocation()),
                    event.getStatus(),
                    event.getMaxParticipants(),
                    registrationCount,
                    event.getCreator().getUsername(),
                    event.getCreatedAt() != null ? event.getCreatedAt().format(formatter) : ""
            );
        }
        
        writer.flush();
        writer.close();
        
        return outputStream.toByteArray();
    }
    
    private byte[] generateUsersCSV(List<User> users) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);
        
        // CSV Header
        writer.println("ID,Username,Email,First Name,Last Name,Phone,Role,Enabled,Auth Provider,Events Created,Registrations,Created At");
        
        // CSV Data
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (User user : users) {
            int eventsCreated = user.getEvents() != null ? user.getEvents().size() : 0;
            int registrationsCount = registrationRepository.countByUserId(user.getId());
            
            writer.printf("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%s,%s,%s,%d,%d,%s%n",
                    user.getId(),
                    escapeCsv(user.getUsername()),
                    escapeCsv(user.getEmail()),
                    escapeCsv(user.getFirstName()),
                    escapeCsv(user.getLastName()),
                    escapeCsv(user.getPhoneNumber() != null ? user.getPhoneNumber() : ""),
                    user.getRole(),
                    user.isEnabled(),
                    user.getAuthProvider(),
                    eventsCreated,
                    registrationsCount,
                    user.getCreatedAt() != null ? user.getCreatedAt().format(formatter) : ""
            );
        }
        
        writer.flush();
        writer.close();
        
        return outputStream.toByteArray();
    }
    
    private byte[] generateEventsJSON(List<Event> events) throws IOException {
        List<Map<String, Object>> eventList = events.stream()
                .map(event -> {
                    Map<String, Object> eventMap = new HashMap<>();
                    eventMap.put("id", event.getId());
                    eventMap.put("title", event.getTitle());
                    eventMap.put("description", event.getDescription());
                    eventMap.put("date", event.getDate());
                    eventMap.put("location", event.getLocation());
                    eventMap.put("status", event.getStatus());
                    eventMap.put("maxParticipants", event.getMaxParticipants());
                    eventMap.put("currentRegistrations", registrationRepository.countByEventId(event.getId()));
                    eventMap.put("creatorUsername", event.getCreator().getUsername());
                    eventMap.put("creatorEmail", event.getCreator().getEmail());
                    eventMap.put("createdAt", event.getCreatedAt());
                    eventMap.put("updatedAt", event.getUpdatedAt());
                    return eventMap;
                })
                .collect(Collectors.toList());
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        return mapper.writeValueAsBytes(eventList);
    }
    
    private byte[] generateUsersJSON(List<User> users) throws IOException {
        List<Map<String, Object>> userList = users.stream()
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("username", user.getUsername());
                    userMap.put("email", user.getEmail());
                    userMap.put("firstName", user.getFirstName());
                    userMap.put("lastName", user.getLastName());
                    userMap.put("phoneNumber", user.getPhoneNumber());
                    userMap.put("address", user.getAddress());
                    userMap.put("role", user.getRole());
                    userMap.put("enabled", user.isEnabled());
                    userMap.put("authProvider", user.getAuthProvider());
                    userMap.put("eventsCreated", user.getEvents() != null ? user.getEvents().size() : 0);
                    userMap.put("registrationsCount", registrationRepository.countByUserId(user.getId()));
                    userMap.put("createdAt", user.getCreatedAt());
                    userMap.put("updatedAt", user.getUpdatedAt());
                    return userMap;
                })
                .collect(Collectors.toList());
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        return mapper.writeValueAsBytes(userList);
    }
    
    @Override
    public byte[] exportRegistrationsToCSV(Long eventId, String status, Boolean completedOnly) throws IOException {
        log.info("Exporting registrations for event {} to CSV - status: {}, completedOnly: {}", eventId, status, completedOnly);
        
        Registration.RegistrationStatus registrationStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                registrationStatus = Registration.RegistrationStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status: {}", status);
            }
        }
        
        List<Registration> registrations = registrationRepository.findRegistrationsForExport(
                eventId, registrationStatus, completedOnly);
        
        return generateRegistrationsCSV(registrations);
    }
    
    @Override
    public byte[] exportRegistrationsToJSON(Long eventId, String status, Boolean completedOnly) throws IOException {
        log.info("Exporting registrations for event {} to JSON - status: {}, completedOnly: {}", eventId, status, completedOnly);
        
        Registration.RegistrationStatus registrationStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                registrationStatus = Registration.RegistrationStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status: {}", status);
            }
        }
        
        List<Registration> registrations = registrationRepository.findRegistrationsForExport(
                eventId, registrationStatus, completedOnly);
        
        return generateRegistrationsJSON(registrations);
    }
    
    private byte[] generateRegistrationsCSV(List<Registration> registrations) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);
        
        // CSV Header
        writer.println("ID,User ID,Username,Email,First Name,Last Name,Event ID,Event Title,Status,Registered At,Completed At,Event Completed");
        
        // CSV Data
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (Registration reg : registrations) {
            User user = reg.getUser();
            Event event = reg.getEvent();
            
            writer.printf("%d,%d,\"%s\",\"%s\",\"%s\",\"%s\",%d,\"%s\",%s,%s,%s,%s%n",
                    reg.getId(),
                    user.getId(),
                    escapeCsv(user.getUsername()),
                    escapeCsv(user.getEmail()),
                    escapeCsv(user.getFirstName()),
                    escapeCsv(user.getLastName()),
                    event.getId(),
                    escapeCsv(event.getTitle()),
                    reg.getStatus(),
                    reg.getRegisteredAt() != null ? reg.getRegisteredAt().format(formatter) : "",
                    reg.getCompletedAt() != null ? reg.getCompletedAt().format(formatter) : "",
                    reg.getEventCompleted() != null ? reg.getEventCompleted() : false
            );
        }
        
        writer.flush();
        writer.close();
        
        return outputStream.toByteArray();
    }
    
    private byte[] generateRegistrationsJSON(List<Registration> registrations) throws IOException {
        List<Map<String, Object>> registrationList = registrations.stream()
                .map(reg -> {
                    Map<String, Object> regMap = new HashMap<>();
                    User user = reg.getUser();
                    Event event = reg.getEvent();
                    
                    regMap.put("id", reg.getId());
                    regMap.put("userId", user.getId());
                    regMap.put("username", user.getUsername());
                    regMap.put("email", user.getEmail());
                    regMap.put("firstName", user.getFirstName());
                    regMap.put("lastName", user.getLastName());
                    regMap.put("eventId", event.getId());
                    regMap.put("eventTitle", event.getTitle());
                    regMap.put("status", reg.getStatus());
                    regMap.put("registeredAt", reg.getRegisteredAt());
                    regMap.put("completedAt", reg.getCompletedAt());
                    regMap.put("eventCompleted", reg.getEventCompleted());
                    return regMap;
                })
                .collect(Collectors.toList());
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        return mapper.writeValueAsBytes(registrationList);
    }
    
    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}

