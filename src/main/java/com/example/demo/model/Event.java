package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ToString(exclude = {"creator", "registrations", "posts"})

public class Event implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(nullable = false)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    private int maxParticipants;

    private int currentRegistrationCount;


    private String rejectReason;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    private Double totalScore;

    private String qrCodeUrl;

    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    @JsonIgnore
    private User creator;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Registration> registrations;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Post> posts;

    @ManyToMany
    @JoinTable(
            name = "event_tags",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    public enum EventStatus {
        PLANNED,
        ONGOING,
        COMPLETED,
        CANCELLED
    }

}