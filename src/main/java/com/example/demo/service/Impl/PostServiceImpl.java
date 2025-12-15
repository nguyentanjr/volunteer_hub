package com.example.demo.service.Impl;

import com.example.demo.dto.post.CreatePostDTO;
import com.example.demo.dto.post_content.PostDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.PostMapper;
import com.example.demo.model.*;
import com.example.demo.model.Role;
import com.example.demo.repository.*;
import com.example.demo.service.CloudinaryService;
import com.example.demo.service.NotificationService;
import com.example.demo.service.PostService;
import com.example.demo.service.UserService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@Getter
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CloudinaryService cloudinaryService;
    private final UserService userService;
    private final RegistrationRepository registrationRepository;
    private final CommentRepository commentRepository;
    private final PostMapper postMapper;
    private final LikeRepository likeRepository;
    private final FileRepository fileRepository;
    private final NotificationService notificationService;

    @Override
    public Page<PostDTO> getAllPosts(Long eventId, Pageable pageable) {
        log.info("Get all posts in event: {}", eventId);
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        return postRepository.getAllPostByEvent(eventId, pageable)
                .map(this::toDTOWithLikeCountAndCommentCount);
    }

    @Transactional
    @CacheEvict(value = "dashboard", key = "'volunteer:' + @userService.getCurrentUser().id")
    public PostDTO createPost(Long eventId, List<MultipartFile> multipartFiles, CreatePostDTO createPostDTO) throws IOException {
        log.info("Create new post with files in event: {}", eventId);

        // Validate input
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }

        if (multipartFiles == null) {
            multipartFiles = new ArrayList<>();
        }
        log.info("Incoming multipart files count: {}", multipartFiles.size());
        boolean hasFiles = multipartFiles.stream().anyMatch(f -> f != null && !f.isEmpty());
        boolean hasContent = createPostDTO.getContent() != null && !createPostDTO.getContent().trim().isEmpty();
        if (!hasFiles && !hasContent) {
            throw new IllegalArgumentException("Post must have either content or files.");
        }
        // Get current user
        User user = userService.getCurrentUser();
        log.info("User {} attempting to create post in event {}", user.getUsername(), eventId);

        // Get event first to check status and creator
        Event event = eventRepository.getEventById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        // Check if event is PLANNED - feed is not available for PLANNED events
        if (event.getStatus() == Event.EventStatus.PLANNED) {
            log.error("Event {} is PLANNED, feed is not available", eventId);
            throw new IllegalStateException("Event feed is not available for events with PLANNED status");
        }

        // Check if user is the event creator (manager)
        boolean isEventCreator = event.getCreator().getId().equals(user.getId());
        boolean isManager = user.hasRole(Role.RoleName.EVENT_MANAGER) || user.hasRole(Role.RoleName.ADMIN);

        // If user is manager but not the creator, they need to register
        if (isManager && !isEventCreator) {
            log.info("User {} is manager but not creator of event {}, checking registration", user.getId(), eventId);
            Registration registration = registrationRepository.findRegistrationByUserIdAndEventId(user.getId(), eventId)
                    .orElseThrow(() -> {
                        log.error("Registration not found for manager {} in event {}", user.getId(), eventId);
                        return new ResourceNotFoundException("You must register for this event before posting");
                    });

            log.info("Registration found with status: {}", registration.getStatus());
            if(!registration.getStatus().equals(Registration.RegistrationStatus.APPROVED)) {
                log.error("Manager {} registration status is {} (not APPROVED)", user.getId(), registration.getStatus());
                throw new IllegalStateException("Your registration must be approved before you can create posts");
            }
        } else if (!isEventCreator) {
            // Regular volunteers need registration
            Registration registration = registrationRepository.findRegistrationByUserIdAndEventId(user.getId(), eventId)
                    .orElseThrow(() -> {
                        log.error("Registration not found for user {} in event {}", user.getId(), eventId);
                        return new ResourceNotFoundException("You must register for this event before posting");
                    });

            log.info("Registration found with status: {}", registration.getStatus());
            if(!registration.getStatus().equals(Registration.RegistrationStatus.APPROVED)) {
                log.error("User {} registration status is {} (not APPROVED)", user.getId(), registration.getStatus());
                throw new IllegalStateException("Your registration must be approved before you can create posts");
            }
        } else {
            // Event creator (manager) can always post
            log.info("User {} is the creator of event {}, allowing post creation", user.getId(), eventId);
        }

        // Create and save post first (needed for FileRecord relationships)
        Post post = new Post()
                .setPostCreator(user)
                .setEvent(event)
                .setContent(createPostDTO.getContent());

        Post savedPost = postRepository.save(post);
        log.info("Post created with ID: {}", savedPost.getId());

        // Upload files and create FileRecords
        if (hasFiles) {
            List<FileRecord> fileRecords = new ArrayList<>();
            for (MultipartFile file : multipartFiles) {
                if (file != null && !file.isEmpty()) {
                    try {
                        FileRecord fileRecord = cloudinaryService.uploadFileForPostOrComment(file, savedPost);
                        fileRecords.add(fileRecord);
                        log.info("File uploaded: {}", file.getOriginalFilename());
                    } catch (IOException e) {
                        log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
                        throw new IOException("Failed to upload file: " + file.getOriginalFilename(), e);
                    }
                }
            }
            savedPost.setFileRecords(fileRecords);
            // Persist FileRecords and update post association
            fileRepository.saveAll(fileRecords);
            savedPost = postRepository.save(savedPost);
        }
        
        // Notify all approved members in the event about the new post
        notificationService.notifyAllMembersInEventOnNewPost(event, savedPost);
        
        return toDTOWithLikeCountAndCommentCount(savedPost);
    }

    private PostDTO toDTOWithLikeCountAndCommentCount(Post post) {
        PostDTO postDTO = postMapper.toPostDTO(post);
        postDTO.setCommentCount(commentRepository.countCommentsByPostId(postDTO.getId()));
        postDTO.setLikeCount(likeRepository.countLikesByPostId(postDTO.getId()));

        // Map file records to DTO (ensure FE receives media)
        if (post.getFileRecords() != null && !post.getFileRecords().isEmpty()) {
            List<com.example.demo.dto.file.FileRecordDTO> fileDTOs = post.getFileRecords().stream()
                    .map(fr -> new com.example.demo.dto.file.FileRecordDTO(fr.getId(), fr.getFileName(), fr.getUrl(), fr.getFileType()))
                    .toList();
            postDTO.setFiles(fileDTOs);
        }

        try {
            User currentUser = userService.getCurrentUser();
            if (currentUser != null) {
                boolean liked = likeRepository.findByPostIdAndUserId(post.getId(), currentUser.getId()) != null;
                postDTO.setIsLikedByCurrentUser(liked);
            } else {
                postDTO.setIsLikedByCurrentUser(false);
            }
        } catch (Exception e) {
            // Nếu không có user đăng nhập, mặc định chưa like
            postDTO.setIsLikedByCurrentUser(false);
        }

        return postDTO;
    }

    @Override
    @Transactional
    public void deletePost(Long postId) {
        log.info("Deleting post: {}", postId);

        User currentUser = userService.getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getPostCreator().getId().equals(currentUser.getId())) {
            throw new com.example.demo.exception.UnauthorizedException("You are not authorized to delete this post");
        }

        postRepository.delete(post);
        log.info("Post deleted successfully: {}", postId);
    }

    @Override
    @Transactional
    public PostDTO updatePost(Long postId, CreatePostDTO updatePostDTO) {
        log.info("Updating post: {}", postId);

        User currentUser = userService.getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getPostCreator().getId().equals(currentUser.getId())) {
            throw new com.example.demo.exception.UnauthorizedException("You are not authorized to update this post");
        }

        post.setContent(updatePostDTO.getContent());
        post.setUpdatedAt(java.time.LocalDateTime.now());

        Post savedPost = postRepository.save(post);
        return toDTOWithLikeCountAndCommentCount(savedPost);
    }

    @Override
    @Transactional
    public PostDTO updatePostWithFiles(Long postId, String content, List<MultipartFile> files, List<Long> removeFileIds) throws IOException {
        log.info("Updating post with files: {}", postId);

        User currentUser = userService.getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getPostCreator().getId().equals(currentUser.getId())) {
            throw new com.example.demo.exception.UnauthorizedException("You are not authorized to update this post");
        }

        if (content != null) {
            post.setContent(content);
        }

        // Remove files if requested
        if (removeFileIds != null && !removeFileIds.isEmpty()) {
            if (post.getFileRecords() != null) {
                post.getFileRecords().removeIf(fr -> removeFileIds.contains(fr.getId()));
            }
            fileRepository.deleteAllById(removeFileIds);
        }

        boolean hasFiles = files != null && !files.isEmpty();

        if (hasFiles) {
            List<FileRecord> fileRecords = post.getFileRecords() != null ? post.getFileRecords() : new ArrayList<>();
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    FileRecord fileRecord = cloudinaryService.uploadFileForPostOrComment(file, post);
                    fileRecords.add(fileRecord);
                    fileRepository.save(fileRecord);
                    log.info("File uploaded for post update: {}", file.getOriginalFilename());
                }
            }
            post.setFileRecords(fileRecords);
        }

        post.setUpdatedAt(java.time.LocalDateTime.now());
        Post savedPost = postRepository.save(post);
        return toDTOWithLikeCountAndCommentCount(savedPost);
    }
}
