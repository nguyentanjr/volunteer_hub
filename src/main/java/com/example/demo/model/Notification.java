package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ToString(exclude = {"user"})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String message;
    private RelatedType relatedType;
    private Long relatedId;
    private LocalDateTime createdAt = LocalDateTime.now();
    private boolean readStatus = false;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    public enum RelatedType {
        EVENT,
        COMMENT,
        POST,
        LIKE,
        REGISTRATION,
        GENERAL
    }
}
