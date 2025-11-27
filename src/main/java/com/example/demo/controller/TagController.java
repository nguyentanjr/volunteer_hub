package com.example.demo.controller;

import com.example.demo.dto.common.ApiResponse;
import com.example.demo.model.Tag;
import com.example.demo.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {
    private final TagService tagService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Tag>>> getAllTags() {
        return ResponseEntity.ok(ApiResponse.success(tagService.getAllTags()));
    }
}

