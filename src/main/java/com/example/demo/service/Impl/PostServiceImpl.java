package com.example.demo.service.Impl;

import com.example.demo.dto.post.CreatePostDTO;
import com.example.demo.dto.post_content.PostDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.PostMapper;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.CloudinaryService;
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

    @Override
    public Page<PostDTO> getAllPosts(Long eventId, Pageable pageable) {
        log.info("Get all posts in event: {}", eventId);
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        return postRepository.getAllPostByEvent(eventId, pageable)
                .map(postMapper::toPostDTO);
    }

    @Transactional
    @CacheEvict(value = "dashboard", key = "'volunteer:' + #root.target.userService.getCurrentUser().id")
    public PostDTO createPost(Long eventId, List<MultipartFile> multipartFiles, CreatePostDTO createPostDTO) throws IOException {
        log.info("Create new post with files in event: {}", eventId);

        // Validate input
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }

        if (multipartFiles == null) {
            multipartFiles = new ArrayList<>();
        }
        boolean hasFiles = multipartFiles.stream().anyMatch(f -> f != null && !f.isEmpty());
        boolean hasContent = createPostDTO.getContent() != null && !createPostDTO.getContent().trim().isEmpty();
        if (!hasFiles && !hasContent) {
            throw new IllegalArgumentException("Post must have either content or files.");
        }
        // Get current user
        User user = userService.getCurrentUser();

        Registration registration = registrationRepository.findRegistrationByUserIdAndEventId(user.getId(), eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found"));

        if(!registration.getStatus().equals(Registration.RegistrationStatus.APPROVED)) {
            throw new IllegalStateException("User must be approved to create post");
        }

        // Get event
        Event event = eventRepository.getEventById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

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
        }

        return toDTOWithLikeCountAndCommentCount(savedPost);
    }

    private PostDTO toDTOWithLikeCountAndCommentCount(Post post) {
        PostDTO postDTO = postMapper.toPostDTO(post);
        postDTO.setCommentCount(commentRepository.countCommentsByPostId(postDTO.getId()));
        postDTO.setLikeCount(likeRepository.countLikesByPostId(postDTO.getId()));
        return postDTO;
    }
}
