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
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @CacheEvict(value = "dashboard", key = "'volunteer:' + #root.target.getCurrentUser().id")
    public CommentDTO createComment(CreateCommentDTO createCommentDTO) throws IOException {
        return createCommentInternal(createCommentDTO, null);
    }

    @Override
    @CacheEvict(value = "dashboard", key = "'volunteer:' + #root.target.getCurrentUser().id")
    public CommentDTO createCommentWithFiles(CreateCommentDTO createCommentDTO, List<MultipartFile> files) throws IOException {
        return createCommentInternal(createCommentDTO, files);
    }

    private CommentDTO createCommentInternal(CreateCommentDTO createCommentDTO, List<MultipartFile> files) throws IOException {
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
            // Only notify if not replying to own comment
            if (!parentcomment.getUser().getId().equals(user.getId())) {
                notificationService.notifyUserOnNewChildComment(parentcomment.getUser(), comment);
            }
        }

        Comment savedComment = commentRepository.save(comment);

        // Upload files if provided
        if (files != null && !files.isEmpty()) {
            List<FileRecord> fileRecords = new ArrayList<>();
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    try {
                        FileRecord fileRecord = cloudinaryService.uploadFileForPostOrComment(file, savedComment);
                        fileRecords.add(fileRecord);
                        fileRepository.save(fileRecord);
                        log.info("File uploaded for comment: {}", file.getOriginalFilename());
                    } catch (IOException e) {
                        log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
                        throw new IOException("Failed to upload file: " + file.getOriginalFilename(), e);
                    }
                }
            }
            savedComment.setFileRecords(fileRecords);
        }
        
        // Convert to DTO for broadcasting
        CommentDTO commentDTO = toCommentDTOWithCounts(savedComment);
        
        // Broadcast new comment to all users viewing this post
        String topic = "/topic/posts/" + post.getId() + "/comments";
        try {
            messagingTemplate.convertAndSend(topic, commentDTO);
            log.info("Broadcasted new comment to topic: {} with comment ID: {}", topic, commentDTO.getId());
        } catch (Exception e) {
            log.error("Error broadcasting comment to topic: {}", topic, e);
        }
        
        // If this is a reply, also broadcast to the parent comment's replies topic
        if (savedComment.getParentComment() != null) {
            String repliesTopic = "/topic/comments/" + savedComment.getParentComment().getId() + "/replies";
            try {
                messagingTemplate.convertAndSend(repliesTopic, commentDTO);
                log.info("Broadcasted new reply to topic: {} with comment ID: {}", repliesTopic, commentDTO.getId());
            } catch (Exception e) {
                log.error("Error broadcasting reply to topic: {}", repliesTopic, e);
            }
        }
        
        // Only notify if not commenting on own post
        if (!post.getPostCreator().getId().equals(user.getId())) {
            notificationService.notifyUserOnNewComment(post.getPostCreator(), savedComment);
        }

        log.info("Success create comment in post: {}", post.getId());
        return commentDTO;
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
    @Transactional
    public CommentDTO updateCommentWithFiles(Long commentId, String content, List<MultipartFile> files) throws IOException {
        log.info("Updating comment with files: {}", commentId);
        User currentUser = userService.getCurrentUser();

        Comment comment = commentRepository.findByIdWithDetails(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if(!comment.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to update this comment");
        }

        if (content != null) {
            comment.setContent(content);
        }

        boolean hasFiles = files != null && !files.isEmpty();
        if (hasFiles) {
            List<FileRecord> fileRecords = comment.getFileRecords() != null ? comment.getFileRecords() : new ArrayList<>();
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    FileRecord fr = cloudinaryService.uploadFileForPostOrComment(file, comment);
                    fileRecords.add(fr);
                    fileRepository.save(fr);
                    log.info("File uploaded for comment update: {}", file.getOriginalFilename());
                }
            }
            comment.setFileRecords(fileRecords);
        }

        comment.setUpdatedAt(LocalDateTime.now());
        Comment saved = commentRepository.save(comment);
        return toCommentDTOWithCounts(saved);
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

    @Override
    public CommentCursorPageResponse getAllRepliesFlattened(Long commentId, String cursor, int limit) {
        log.info("Getting all replies (flattened) for comment: {} with cursor: {} and limit: {}", commentId, cursor, limit);

        if(limit <= 0 || limit >= 100) {
            limit = 20;
        }

        // Get all replies flattened (all nested replies at same level)
        List<Comment> allReplies = getAllRepliesRecursive(commentId);
        
        // Sort by createdAt DESC
        allReplies.sort((a, b) -> {
            int dateCompare = b.getCreatedAt().compareTo(a.getCreatedAt());
            if (dateCompare != 0) return dateCompare;
            return Long.compare(b.getId(), a.getId());
        });

        // Apply cursor pagination if provided
        if (cursor != null && !cursor.isEmpty()) {
            LocalDateTime cursorDate = decodeDateCursor(cursor);
            allReplies = allReplies.stream()
                .filter(c -> c.getCreatedAt().isBefore(cursorDate) || 
                            (c.getCreatedAt().equals(cursorDate) && c.getId() < Long.parseLong(cursor.split("_")[1])))
                .toList();
        }

        // Apply limit
        boolean hasNext = allReplies.size() > limit;
        if (hasNext) {
            allReplies = allReplies.subList(0, limit);
        }

        List<CommentDTO> commentDTOs = allReplies.stream()
            .map(this::toCommentDTOWithCounts)
            .toList();

        String nextCursor = null;
        if (!allReplies.isEmpty() && hasNext) {
            Comment lastReply = allReplies.get(allReplies.size() - 1);
            nextCursor = encodeCursor(lastReply, CommentSortType.LATEST);
        }
        
        return CommentCursorPageResponse.of(commentDTOs, nextCursor, hasNext);
    }

    /**
     * Recursively get all replies (flattened) for a comment
     * This includes all nested replies at any level
     */
    private List<Comment> getAllRepliesRecursive(Long commentId) {
        List<Comment> allReplies = new java.util.ArrayList<>();
        
        // Get direct replies
        List<Comment> directReplies = commentRepository.findRepliesByParentCommentIdOrderByLatestWithLimit(commentId, 1000);
        allReplies.addAll(directReplies);
        
        // Recursively get replies of each direct reply
        for (Comment reply : directReplies) {
            allReplies.addAll(getAllRepliesRecursive(reply.getId()));
        }
        
        return allReplies;
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
        // Use total replies (all nested) instead of only direct replies
        commentDTO.setReplyCount(getAllRepliesRecursive(comment.getId()).size());

        // Ensure parent author name is populated
        if (commentDTO.getParentAuthorName() == null && comment.getParentComment() != null && comment.getParentComment().getUser() != null) {
            var parentUser = comment.getParentComment().getUser();
            String first = parentUser.getFirstName();
            String last = parentUser.getLastName();
            String username = parentUser.getUsername();
            String full = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
            commentDTO.setParentAuthorName(!full.isBlank() ? full : username);
        }
        // Ensure parent username is populated
        if (commentDTO.getParentUsername() == null && comment.getParentComment() != null && comment.getParentComment().getUser() != null) {
            commentDTO.setParentUsername(comment.getParentComment().getUser().getUsername());
        }

        // Map file records
        if (comment.getFileRecords() != null && !comment.getFileRecords().isEmpty()) {
            List<com.example.demo.dto.file.FileRecordDTO> fileRecordDTOs = comment.getFileRecords().stream()
                .map(fr -> new com.example.demo.dto.file.FileRecordDTO(fr.getId(), fr.getFileName(), fr.getUrl(), fr.getFileType()))
                .toList();
            commentDTO.setFileRecords(fileRecordDTOs);
        }

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
