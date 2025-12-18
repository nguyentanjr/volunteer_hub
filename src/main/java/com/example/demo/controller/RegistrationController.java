package com.example.demo.controller;

import com.cloudinary.Api;
import com.example.demo.dto.common.ApiResponse;
import com.example.demo.dto.event.EventDTO;
import com.example.demo.dto.registration.RegistrationDTO;
import com.example.demo.service.RegistrationService;
import com.example.demo.service.UserService;
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
    private final UserService userService;

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

    /**
     * Get current user's registrations with full details (including status)
     * Used by frontend to show registration status badges
     */
    @GetMapping("/my-registrations")
    public ResponseEntity<ApiResponse<Page<RegistrationDTO>>> getMyRegistrations(
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "100") int pageSize) {
        Long currentUserId = userService.getCurrentUser().getId();
        Sort sort = Sort.by("registeredAt").descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        
        Page<RegistrationDTO> registrations = registrationService.getMyRegistrations(currentUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success(registrations));
    }

    /**
     * Get registration status for current user in a specific event.
     * Used by frontend to decide whether to show Register button or not.
     */
    @GetMapping("/events/{eventId}/status")
    public ResponseEntity<ApiResponse<RegistrationDTO>> getMyRegistrationStatus(@PathVariable Long eventId) {
        // Get current authenticated user
        Long currentUserId = userService.getCurrentUser().getId();

        try {
            RegistrationDTO registrationDTO = registrationService.findRegistrationByUserIdAndEventId(currentUserId, eventId);
            return ResponseEntity.ok(ApiResponse.success(registrationDTO));
        } catch (Exception ex) {
            // If no registration found, return success with null data so frontend knows user is not registered
            return ResponseEntity.ok(ApiResponse.success(null, "No registration found for current user and event"));
        }
    }

    /**
     * Delete/remove a registration by manager (admin can remove any user from event)
     */
    @DeleteMapping("/{registrationId}")
    public ResponseEntity<ApiResponse<Void>> deleteRegistration(@PathVariable Long registrationId) {
        registrationService.deleteRegistrationById(registrationId);
        return ResponseEntity.ok(ApiResponse.success(null, "Registration deleted successfully"));
    }

}
