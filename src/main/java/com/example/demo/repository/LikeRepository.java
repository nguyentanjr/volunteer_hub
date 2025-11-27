package com.example.demo.repository;

import com.example.demo.model.Like;
import com.example.demo.model.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LikeRepository extends JpaRepository<Like, Long> {

    @Query("SELECT l FROM Like l WHERE l.post.id = :postId AND l.user.id = :userId")
    Like findByPostIdAndUserId(Long postId, Long userId);

    @Query("SELECT l FROM Like l WHERE l.comment.id = :commentId AND l.user.id = :userId")
    Like findByCommentIdAndUserId(Long commentId, Long userId);

    @Query("SELECT COUNT(l) FROM Like l WHERE l.post.id = :postId")
    Integer countLikesByPostId(Long postId);

    @Query("SELECT COUNT(l) FROM Like l WHERE l.comment.id = :commentId")
    Integer countLikesByCommentId(@Param("commentId") Long commentId);

    @Query("SELECT COUNT(l) FROM Like l WHERE l.post.event.id = :eventId")
    Integer countAllLikePostsByEventId(Long eventId);

    @Query("SELECT COUNT(l) FROM Like l WHERE l.comment.post.event.id = :eventId")
    Integer countAllLikeCommentsByEventId(Long eventId);

    @Query("SELECT l FROM Like l WHERE l.user.id = :userId AND l.post.id IS NULL ORDER BY l.createdAt DESC")
    List<Like> findLikesCommentByUserId(Long userId, Pageable pageable);

    @Query("SELECT l FROM Like l WHERE l.user.id = :userId AND l.comment.id IS NULL ORDER BY l.createdAt DESC")
    List<Like> findLikesPostByUserId(Long userId, Pageable pageable);


}
