package com.example.demo.controller;

import com.example.demo.dto.common.ApiResponse;
import com.example.demo.dto.event.CreateEventDTO;
import com.example.demo.dto.event.EventDTO;
import com.example.demo.model.Post;
import com.example.demo.model.Registration;
import com.example.demo.service.EventService;
import com.example.demo.service.PostService;
import com.example.demo.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final PostService postService;
    private final RegistrationService registrationService;

    @PostMapping
    public ResponseEntity<ApiResponse<EventDTO>> createEvent(@Valid @RequestBody CreateEventDTO createEventDTO) {
        EventDTO eventDTO =  eventService.createEvent(createEventDTO);
        return ResponseEntity.ok(ApiResponse.created(eventDTO));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EventDTO>>> getAllEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(ApiResponse.success(eventService.getAllEvents(pageable),"Retrieve all events successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<EventDTO>>> getMyEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(ApiResponse.success(eventService.getMyEvents(pageable),"Retrieve my events successfully"));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<EventDTO>> getEventDetail(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.success(eventService.getEventDetails(eventId)));
    }

    @GetMapping("/{eventId}/registrations/{status}/count")
    public ResponseEntity<ApiResponse<Integer>> countEventRegistration(
            @PathVariable Long eventId,
            @PathVariable Registration.RegistrationStatus status) {
        return ResponseEntity.ok(ApiResponse.success(registrationService.countParticipantsOfAnEvent(eventId, status)));
    }

    @GetMapping("/search")
    public ResponseEntity<List<String>> autocomplete(@RequestParam String keyword) {
        if (keyword.length() < 2) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(eventService.searchSuggestions(keyword));
    }
}