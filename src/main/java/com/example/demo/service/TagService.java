package com.example.demo.service;

import com.example.demo.model.Event;
import com.example.demo.model.Tag;
import com.example.demo.model.User;

import java.util.List;

public interface TagService {
    List<Tag> getAllTags();

    double calculateTagScore(User user, Event event);
}
