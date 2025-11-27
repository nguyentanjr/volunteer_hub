package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentType contentType;

    @Column(length = 2000)
    private String textContent;

    private String mediaUrl;

    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum ContentType {
        TEXT,
        IMAGE,
        VIDEO
    }
}