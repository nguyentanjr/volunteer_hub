package com.example.demo.controller;

import com.cloudinary.Api;
import com.example.demo.dto.common.ApiResponse;
import com.example.demo.dto.event.EventDTO;
import com.example.demo.dto.registration.RegistrationDTO;
import com.example.demo.service.ExportService;
import com.example.demo.service.RegistrationService;
import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/registrations")
@RequiredArgsConstructor
@Slf4j
public class RegistrationController {

    private final RegistrationService registrationService;
    private final ExportService exportService;

    @PostMapping("/events/{eventId}")
    public ResponseEntity<ApiResponse<RegistrationDTO>> registerEvent(@PathVariable Long eventId) {
        RegistrationDTO registrationDTO = registrationService.registerEvent(eventId);
        return ResponseEntity.ok(ApiResponse.success(registrationDTO));
    }

    @PostMapping("/events/{eventId}/{registrationId}/approved")
    public ResponseEntity<?> approvedRegistration(@PathVariable Long eventId, @PathVariable Long registrationId) throws FirebaseMessagingException {
        registrationService.approvedRegistration(registrationId);
        return ResponseEntity.ok("Success");
    }

    @PostMapping("/events/{eventId}/{registrationId}/rejected")
    public ResponseEntity<?> rejectedRegistration(@PathVariable Long eventId, @PathVariable Long registrationId) throws FirebaseMessagingException {
        registrationService.rejectedRegistration(registrationId);
        return ResponseEntity.ok("Success");
    }

    @GetMapping("/events/{eventId}")
    public ResponseEntity<ApiResponse<Page<RegistrationDTO>>> getAllRegistrationOfAnEvent(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        return ResponseEntity.ok(ApiResponse.success(registrationService.getAllRegistration(pageable)));
    }

    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<ApiResponse<Void>> cancelRegistration(@PathVariable Long eventId) {
        registrationService.cancelRegistration(eventId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<EventDTO>>> getRegisteredEvents(
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        return ResponseEntity.ok(ApiResponse.success(registrationService.getRegisteredEvents(pageable)));
    }

    @GetMapping("/events/{eventId}/status")
    public ResponseEntity<ApiResponse<RegistrationDTO>> getRegistrationStatus(@PathVariable Long eventId) {
        RegistrationDTO registrationDTO = registrationService.getCurrentUserRegistrationStatus(eventId);
        if (registrationDTO == null) {
            return ResponseEntity.ok(ApiResponse.success(null, "User has not registered for this event"));
        }
        return ResponseEntity.ok(ApiResponse.success(registrationDTO));
    }

    @GetMapping("/events/{eventId}/export")
    @PreAuthorize("hasRole('EVENT_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportEventRegistrations(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean completedOnly) throws IOException {
        
        log.info("Exporting registrations for event {} - format: {}, status: {}, completedOnly: {}", 
                eventId, format, status, completedOnly);
        
        byte[] data;
        String filename;
        MediaType mediaType;
        
        if ("json".equalsIgnoreCase(format)) {
            data = exportService.exportRegistrationsToJSON(eventId, status, completedOnly);
            filename = "registrations_event_" + eventId + "_" + 
                      LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".json";
            mediaType = MediaType.APPLICATION_JSON;
        } else {
            data = exportService.exportRegistrationsToCSV(eventId, status, completedOnly);
            filename = "registrations_event_" + eventId + "_" + 
                      LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
            mediaType = MediaType.parseMediaType("text/csv");
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }

}
