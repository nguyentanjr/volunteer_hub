package com.example.demo.dto.post_content;

import com.example.demo.dto.file.FileRecordDTO;
import com.example.demo.model.PostContent;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

// PostDTO.java - Cải thiện
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostDTO {
    private Long id;
    private String content;

    // User info
    private Long userId;

    // Counts
    private Integer likeCount;
    private Integer commentCount;

    // Status
    private Boolean isPinned;  // Thêm
    private Boolean isLikedByCurrentUser;  // Thêm (optional)

    // Files
    private List<FileRecordDTO> files;  // Thêm

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}