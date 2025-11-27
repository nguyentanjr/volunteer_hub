package com.example.demo.service.Impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Comment;
import com.example.demo.model.FileRecord;
import com.example.demo.model.Post;
import com.example.demo.model.User;
import com.example.demo.repository.FileRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;

    public FileRecord uploadFileForPostOrComment(MultipartFile multipartFile, Object relatedEntity) throws IOException {
        // Get current user from SecurityContext
        Principal principal = (Principal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = principal.getName();
        User user = userRepository.getUserByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate input
        validateFile(multipartFile);

        if (relatedEntity == null) {
            throw new IllegalArgumentException("Related entity cannot be null");
        }

        if (!(relatedEntity instanceof Post) && !(relatedEntity instanceof Comment)) {
            throw new IllegalArgumentException("Related entity must be either Post or Comment");
        }

        log.info("Uploading file: {} for user: {} related to: {}",
                multipartFile.getOriginalFilename(), username, relatedEntity.getClass().getSimpleName());


        // Upload to Cloudinary
        Map<String, Object> uploadResult = cloudinary.uploader().upload(multipartFile.getBytes(), ObjectUtils.asMap(
                "folder", username,
                "public_id", user.getId() + "/" + multipartFile.getOriginalFilename(),
                "resource_type", "auto"
        ));

        String url = uploadResult.get("url").toString();

        // Create FileRecord
        FileRecord fileRecord = new FileRecord()
                .setUrl(url)
                .setFileName(multipartFile.getOriginalFilename())
                .setUser(user);

        // Set relationship based on entity type
        log.info("Post or comment");
        if (relatedEntity instanceof Post) {
            fileRecord.setPost((Post) relatedEntity);
        } else {
            fileRecord.setComment((Comment) relatedEntity);
        }


        log.info("File uploaded successfully: {}", url);
        return fileRecord;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        long maxSize = 2 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds 2MB");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("Invalid file type");
        }

        if (!contentType.startsWith("image/") && !contentType.startsWith("video/")) {
            throw new IllegalArgumentException("File must be an image or video");
        }

    }
}
