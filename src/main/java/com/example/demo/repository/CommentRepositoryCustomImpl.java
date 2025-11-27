package com.example.demo.repository;

import com.example.demo.model.Comment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
public class CommentRepositoryCustomImpl implements CommentRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Comment> findByPostIdOrderByLatestWithLimit(Long postId, int limit) {
        log.debug("Finding comments for post {} ordered by latest with limit {}", postId, limit);

        String jpql = "SELECT c FROM Comment c WHERE c.post.id = :postId AND c.parentComment IS NULL " +
                "ORDER BY c.createdAt DESC, c.id DESC";

        TypedQuery<Comment> query = entityManager.createQuery(jpql, Comment.class);
        query.setParameter("postId", postId);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    public List<Comment> findByPostIdOrderByLatestWithCursorAndLimit(Long postId, LocalDateTime cursorDate, int limit) {
        log.debug("Finding top-level comments for post {} ordered by latest with cursor {} and limit {}",
                postId, cursorDate, limit);

        String jpql = "SELECT c FROM Comment c WHERE c.post.id = :postId AND c.parentComment IS NULL " +
                "AND c.createdAt < :cursorDate " +
                "ORDER BY c.createdAt DESC, c.id DESC";

        TypedQuery<Comment> query = entityManager.createQuery(jpql, Comment.class);
        query.setParameter("postId", postId);
        query.setParameter("cursorDate", cursorDate);
        query.setMaxResults(limit);

        return query.getResultList();
    }

    @Override
    public List<Comment> findByPostIdOrderByTopLikedWithLimit(Long postId, int limit) {
        log.debug("Finding top-level comments for post {} ordered by top liked with limit {}", postId, limit);

        String jpql = "SELECT c FROM Comment c LEFT JOIN c.likes l " +
                "WHERE c.post.id = :postId AND c.parentComment IS NULL " +
                "GROUP BY c.id " +
                "ORDER BY COUNT(l) DESC, c.createdAt DESC";

        TypedQuery<Comment> query = entityManager.createQuery(jpql, Comment.class);
        query.setParameter("postId", postId);
        query.setMaxResults(limit);

        return query.getResultList();
    }

    @Override
    public List<Comment> findByPostIdOrderByTopLikedWithCursorAndLimit(Long postId, Long cursorLikeCount, Long cursorId, int limit) {
        log.debug("Finding top-level comments for post {} ordered by top liked with cursor (likeCount={}, id={}) and limit {}",
                postId, cursorLikeCount, cursorId, limit);

        // Using tuple comparison (like_count, id) for cursor pagination
        String jpql = "SELECT c FROM Comment c LEFT JOIN c.likes l " +
                "WHERE c.post.id = :postId AND c.parentComment IS NULL " +
                "AND ((SELECT COUNT(l2) FROM Like l2 WHERE l2.comment.id = c.id) < :cursorLikeCount " +
                "     OR ((SELECT COUNT(l2) FROM Like l2 WHERE l2.comment.id = c.id) = :cursorLikeCount AND c.id < :cursorId)) " +
                "GROUP BY c.id " +
                "ORDER BY COUNT(l) DESC, c.createdAt DESC";

        TypedQuery<Comment> query = entityManager.createQuery(jpql, Comment.class);
        query.setParameter("postId", postId);
        query.setParameter("cursorLikeCount", cursorLikeCount);
        query.setParameter("cursorId", cursorId);
        query.setMaxResults(limit);

        return query.getResultList();
    }

    @Override
    public List<Comment> findRepliesByParentCommentIdOrderByLatestWithLimit(Long parentCommentId, int limit) {
        log.debug("Finding replies for parent comment {} ordered by latest with limit {}", parentCommentId, limit);

        String jpql = "SELECT c FROM Comment c WHERE c.parentComment.id = :parentCommentId " +
                "ORDER BY c.createdAt DESC, c.id DESC";

        TypedQuery<Comment> query = entityManager.createQuery(jpql, Comment.class);
        query.setParameter("parentCommentId", parentCommentId);
        query.setMaxResults(limit);

        return query.getResultList();
    }

    @Override
    public List<Comment> findRepliesOrderByLatestWithCursorAndLimit(Long parentCommentId, LocalDateTime cursorDate, int limit) {
        log.debug("Finding replies for parent comment {} ordered by latest with cursor {} and limit {}",
                parentCommentId, cursorDate, limit);

        String jpql = "SELECT c FROM Comment c WHERE c.parentComment.id = :parentCommentId " +
                "AND c.createdAt < :cursorDate " +
                "ORDER BY c.createdAt DESC, c.id DESC";

        TypedQuery<Comment> query = entityManager.createQuery(jpql, Comment.class);
        query.setParameter("parentCommentId", parentCommentId);
        query.setParameter("cursorDate", cursorDate);
        query.setMaxResults(limit);

        return query.getResultList();
    }
}
