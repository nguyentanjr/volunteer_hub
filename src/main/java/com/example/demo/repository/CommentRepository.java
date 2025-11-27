package com.example.demo.repository;

import com.example.demo.model.Comment;
import com.example.demo.model.Event;
import com.example.demo.model.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>,CommentRepositoryCustom {

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.parentComment.id = :commentId")
    Integer countRepliesByCommentId(@Param("commentId") Long commentId);

    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.user LEFT JOIN FETCH c.post WHERE c.id = :commentId")
    Optional<Comment> findByIdWithDetails(@Param("commentId") Long commentId);

    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId")
    List<Comment> findAllByPostId(@Param("postId") Long postId);

    Long post(Post post);

    Comment getCommentById(Long commentId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId")
    Integer countCommentsByPostId(Long postId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.event.id = :eventId")
    Integer countAllCommentByEventId(Long eventId);

    @Query("SELECT e FROM Event e " +
            "WHERE e.creator.id = :managerId")
    List<Event> findEventsWithPendingRegistrations(@Param("managerId") Long managerId);

    @Query("SELECT c FROM Comment c WHERE c.user.id = :userId ORDER BY c.createdAt DESC")
    List<Comment> findCommentsByUserId(Long userId, Pageable pageable);

}
