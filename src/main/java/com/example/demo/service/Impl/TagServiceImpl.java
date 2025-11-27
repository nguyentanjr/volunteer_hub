package com.example.demo.service.Impl;

import com.example.demo.dto.user.UserTagDTO;
import com.example.demo.model.Event;
import com.example.demo.model.Registration;
import com.example.demo.model.Tag;
import com.example.demo.model.User;
import com.example.demo.repository.*;
import com.example.demo.service.TagService;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final EventRepository eventRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;

    @Override
    @Cacheable(value = "tags", key = "'allTags'")
    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    @Override
    public double calculateTagScore(User user, Event event) {
        Set<Tag> userTags = user.getTags();
        Set<Tag> eventTags = event.getTags();

        if (userTags == null || eventTags == null || userTags.isEmpty()) {
            return 0.0;
        }

        Set<Tag> intersection = new HashSet<>(eventTags);
        intersection.retainAll(userTags);

        return (double) intersection.size() / eventTags.size();
    }


}

