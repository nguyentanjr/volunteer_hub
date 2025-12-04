package com.example.demo.service.Impl;

import com.example.demo.dto.comment.*;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UnauthorizedException;
import com.example.demo.mapper.CommentMapper;
import com.example.demo.model.Comment;
import com.example.demo.model.FileRecord;
import com.example.demo.model.Post;
import com.example.demo.model.User;
import com.example.demo.repository.*;
import com.example.demo.service.CloudinaryService;
import com.example.demo.service.CommentService;
import com.example.demo.service.NotificationService;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final PostRepository postRepository;
    private final UserService userService;
    private final CloudinaryService cloudinaryService;
    private final FileRepository fileRepository;
    private final LikeRepository likeRepository;
    private final NotificationService notificationService;

    @Override
    @CacheEvict(value = "dashboard", key = "'volunteer:' + #root.target.getCurrentUser().id")
    public CommentDTO createComment(CreateCommentDTO createCommentDTO) {
        log.info("Creating comment for post: {}", createCommentDTO.getPostId());

        User user = userService.getCurrentUser();
        Post post = postRepository.findById(createCommentDTO.getPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        Comment comment = new Comment()
                .setPost(post)
                .setUser(user)
                .setContent(createCommentDTO.getContent());

        if(createCommentDTO.getParentCommentId() != null) {
            Comment parentcomment = commentRepository.findById(createCommentDTO.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
            comment.setParentComment(parentcomment);
            notificationService.notifyUserOnNewChildComment(parentcomment.getUser(), comment);
        }

        Comment savedComment = commentRepository.save(comment);
        notificationService.notifyUserOnNewComment(post.getPostCreator(), comment);

        log.info("Success create comment in post: {}", post.getId());
        return toCommentDTOWithCounts(savedComment);
    }

    public int countCommentsByPostId(Long postId) {
        return commentRepository.countCommentsByPostId(postId);
    }

    @Override
    public CommentDTO updateComment(Long commentId, UpdateCommentDTO updateCommentDTO) {
        log.info("Updating comment: {}", commentId);

        User currentUser = userService.getCurrentUser();

        Comment comment = commentRepository.findByIdWithDetails(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if(!comment.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to update this comment");
        }

        comment.setContent(updateCommentDTO.getContent());
        comment.setUpdatedAt(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);

        return toCommentDTOWithCounts(comment);

    }

    @Override
    public void deleteComment(Long commentId) {

        log.info("Deleting comment: {}", commentId);

        User currentUser = userService.getCurrentUser();

        Comment comment = commentRepository.findByIdWithDetails(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if(!comment.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to delete this comment");
        }

        commentRepository.delete(comment);
        log.info("Comment deleted successfully: {}", commentId);
    }

    @Override
    public CommentCursorPageResponse getCommentsByPostId(Long postId, String cursor, int limit, CommentSortType commentSortType) {
        if(limit <= 0 || limit >= 100) {
            limit = 20;
        }

        List<Comment> comments;
        if(commentSortType.equals(CommentSortType.LATEST)) {
            comments = getCommentsByLatest(postId, cursor, limit);
        }
        else {
            comments = getCommentsByTopLiked(postId, cursor, limit);
        }

        boolean hasNext = comments.size() > limit;
        if(hasNext) {
            comments = comments.subList(0, limit);
        }

        List<CommentDTO> commentDTOS = comments.stream().map(this::toCommentDTOWithCounts)
                .toList();

        Comment nextCursor = null;
        if (hasNext && !comments.isEmpty() && !commentDTOS.isEmpty()) {
            nextCursor = comments.get(commentDTOS.size() - 1);
        }

        String encodedCursor = nextCursor != null 
                ? encodeCursor(nextCursor, commentSortType) 
                : null;

        return CommentCursorPageResponse.of(commentDTOS, encodedCursor, hasNext);

    }

    public CommentCursorPageResponse getRepliesByCommentId(Long commentId, String cursor, int limit) {
        log.info("Getting replies for comment: {} with cursor: {} and limit: {}", commentId, cursor, limit);

        if(limit <= 0 || limit >= 100) {
            limit = 20;
        }

        List<Comment> replyComments = getRepliesByLatest(commentId, cursor, limit);

        boolean hasNext = replyComments.size() > limit;
        if(hasNext) {
            replyComments = replyComments.subList(0, limit);
        }

        List<CommentDTO> commentDTOs = replyComments.stream().map(this::toCommentDTOWithCounts).toList();

        String nextCursor = null;
        if (!replyComments.isEmpty() && hasNext) {
            Comment lastReplyComment = replyComments.get(replyComments.size() - 1);
            nextCursor = encodeCursor(lastReplyComment, CommentSortType.LATEST);
        }
        
        return CommentCursorPageResponse.of(commentDTOs, nextCursor, hasNext);

    }

    private List<Comment> getRepliesByLatest(Long commentId, String cursor, int limit) {
        if(cursor == null || cursor.isEmpty()) {
            return commentRepository.findRepliesByParentCommentIdOrderByLatestWithLimit(commentId, limit + 1);
        }
        else {
            return commentRepository.findRepliesOrderByLatestWithCursorAndLimit(commentId, decodeDateCursor(cursor), limit + 1);
        }
    }

    private List<Comment> getCommentsByLatest(Long postId, String cursor, int limit) {
        if(cursor == null || cursor.isEmpty()) {
            return commentRepository.findByPostIdOrderByLatestWithLimit(postId, limit + 1);
        }
        else {
            return commentRepository.findByPostIdOrderByLatestWithCursorAndLimit(postId, decodeDateCursor(cursor), limit + 1);
        }
    }

    private List<Comment> getCommentsByTopLiked(Long postId, String cursor, int limit) {
        if(cursor == null || cursor.isEmpty()) {
            return commentRepository.findByPostIdOrderByTopLikedWithLimit(postId, limit + 1);
        }
        else {
            String[] part = decodeTopLikedCursor(cursor);
            return commentRepository.findByPostIdOrderByTopLikedWithCursorAndLimit(postId, Long.parseLong(part[0]),
                    Long.parseLong(part[1]), limit + 1);
        }
    }

    private CommentDTO toCommentDTOWithCounts(Comment comment) {
        CommentDTO commentDTO = commentMapper.toCommentDTO(comment);
        commentDTO.setLikeCount(likeRepository.countLikesByCommentId(comment.getId()));
        commentDTO.setReplyCount(commentRepository.countRepliesByCommentId(comment.getId()));

        try {
            User currentUser = userService.getCurrentUser();
            if (currentUser != null) {
                boolean liked = likeRepository.findByCommentIdAndUserId(comment.getId(), currentUser.getId()) != null;
                commentDTO.setIsLikedByCurrentUser(liked);
            } else {
                commentDTO.setIsLikedByCurrentUser(false);
            }
        } catch (Exception e) {
            // In case there is no authenticated user (e.g. public access), default to false
            commentDTO.setIsLikedByCurrentUser(false);
        }
        return commentDTO;
    }

    private String encodeCursor(Comment comment, CommentSortType sortType) {
        if (comment == null) {
            return null;
        }
        
        String cursorData;

        if(sortType.equals(CommentSortType.LATEST)) {
            // TOP LATEST
            cursorData = comment.getCreatedAt().toString();
        }
        else {
            Integer likeCount = likeRepository.countLikesByCommentId(comment.getId());
            cursorData = likeCount + ":" + comment.getId();
        }
        return Base64.getEncoder().encodeToString(cursorData.getBytes());
    }

    private LocalDateTime decodeDateCursor(String cursor) {
        byte[] decodeBytes = Base64.getDecoder().decode(cursor);
        String decodeString = new String(decodeBytes);
        return LocalDateTime.parse(decodeString);
    }

    private String[] decodeTopLikedCursor(String cursor) {
        byte[] decodeBytes = Base64.getDecoder().decode(cursor);
        String decodeString = new String(decodeBytes);
        return decodeString.split(":");
    }

    public User getCurrentUser() {
        return userService.getCurrentUser();
    }
//    public Page<CommentDTO> fetchCommentsByPostId(Long postId, Pageable pageable) {
//        log.info("Fetch comment in post : {}", post.getId());
//        return commentRepository.findByPostAndParentCommentIsNull(post, pageable).map(commentMapper::toCommentDTO);
//    }
}
