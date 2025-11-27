package com.example.demo.repository;

import com.example.demo.model.Comment;

import java.time.LocalDateTime;
import java.util.List;

public interface CommentRepositoryCustom {


    List<Comment> findByPostIdOrderByLatestWithLimit(Long postId, int limit);

    List<Comment> findByPostIdOrderByLatestWithCursorAndLimit(Long postId, LocalDateTime cursorDate, int limit);

    List<Comment> findByPostIdOrderByTopLikedWithLimit(Long postId, int limit);

    List<Comment> findByPostIdOrderByTopLikedWithCursorAndLimit(Long postId, Long cursorLikeCount, Long cursorId, int limit);

    List<Comment> findRepliesByParentCommentIdOrderByLatestWithLimit(Long parentCommentId, int limit);

    List<Comment> findRepliesOrderByLatestWithCursorAndLimit(Long parentCommentId, LocalDateTime cursorDate, int limit);
}