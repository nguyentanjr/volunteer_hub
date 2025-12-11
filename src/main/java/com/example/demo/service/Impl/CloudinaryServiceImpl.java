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
import org.springframework.util.StringUtils;
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
        log.info("Cloudinary upload start: file={}, size={}", multipartFile.getOriginalFilename(), multipartFile.getSize());
        // Get current user from SecurityContext
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof User) {
            username = ((User) principal).getUsername();
        } else {
            username = principal.toString();
        }
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
        Map<String, Object> uploadResult = cloudinary.uploader().upload(
            multipartFile.getBytes(),
            ObjectUtils.asMap(
                "folder", username,
                "public_id", user.getId() + "/" + multipartFile.getOriginalFilename(),
                "resource_type", "auto"
            )
        );

        String secureUrl = uploadResult.get("secure_url") != null ? uploadResult.get("secure_url").toString() : null;
        String publicId = uploadResult.get("public_id") != null ? uploadResult.get("public_id").toString() : null;
        String resourceType = uploadResult.get("resource_type") != null ? uploadResult.get("resource_type").toString() : null; // image / video / raw
        String fileType = multipartFile.getContentType();

        log.info("Cloudinary upload result: public_id={}, resource_type={}, secure_url={}", publicId, resourceType, secureUrl);

        if (!StringUtils.hasText(secureUrl) || !StringUtils.hasText(publicId)) {
            log.error("Cloudinary upload failed or returned empty url/public_id. Response: {}", uploadResult);
            throw new IOException("Failed to upload file to Cloudinary");
        }

        // Create FileRecord
        FileRecord fileRecord = new FileRecord()
                .setUrl(secureUrl)
                .setPublicId(publicId)
                .setFileName(multipartFile.getOriginalFilename())
                .setFileType(fileType)
                .setUser(user);

        // Set relationship based on entity type
        log.info("Post or comment");
        if (relatedEntity instanceof Post) {
            fileRecord.setPost((Post) relatedEntity);
        } else {
            fileRecord.setComment((Comment) relatedEntity);
        }


       //log.info("File uploaded successfully: {}", url);
        return fileRecord;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Allow larger sizes for video (default 2MB was too small)
        long imageMax = 10 * 1024 * 1024; // 10MB
        long videoMax = 50 * 1024 * 1024; // 50MB

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("Invalid file type");
        }

        boolean isImage = contentType.startsWith("image/");
        boolean isVideo = contentType.startsWith("video/");
        if (!isImage && !isVideo) {
            throw new IllegalArgumentException("File must be an image or video");
        }

        if (isImage && file.getSize() > imageMax) {
            throw new IllegalArgumentException("Image size exceeds 10MB");
        }
        if (isVideo && file.getSize() > videoMax) {
            throw new IllegalArgumentException("Video size exceeds 50MB");
        }

    }
}
