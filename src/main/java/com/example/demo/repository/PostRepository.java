package com.example.demo.repository;

import com.example.demo.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p WHERE p.event.id = :eventId")
    List<Post> getAllPostByEvent(Long eventId);

    @Query("SELECT p FROM Post p WHERE p.event.id = :eventId")
    Page<Post> getAllPostByEvent(Long eventId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.postCreator.id = :userId ORDER BY p.createdAt DESC")
    List<Post> findPostsByUserId(Long userId, Pageable pageable);
}
