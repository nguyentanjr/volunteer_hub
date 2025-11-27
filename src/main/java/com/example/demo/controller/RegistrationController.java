package com.example.demo.controller;

import com.cloudinary.Api;
import com.example.demo.dto.common.ApiResponse;
import com.example.demo.dto.event.EventDTO;
import com.example.demo.dto.registration.RegistrationDTO;
import com.example.demo.service.RegistrationService;
import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

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
            @RequestParam(defaultValue = "1 0") int pageSize,
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


}
