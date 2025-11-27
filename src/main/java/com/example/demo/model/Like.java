package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "post_likes")
@ToString(exclude = {"user", "post", "comment"})
@Accessors(chain = true)
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "post_id")
    @JsonIgnore
    private Post post;

    @ManyToOne
    @JoinColumn(name = "comment_id")
    @JsonIgnore
    private Comment comment;

    private LocalDateTime createdAt = LocalDateTime.now();
}
