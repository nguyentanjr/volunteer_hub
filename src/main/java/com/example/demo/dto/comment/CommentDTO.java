package com.example.demo.dto.comment;

import com.example.demo.dto.file.FileRecordDTO;
import com.example.demo.dto.user.UserCommentDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentDTO {
    private Long id;
    private String content;
    private Long userId;
    private String username;
    private String userFullName;
    private String userAvatarUrl;
    private Long postId;
    private Long parentCommentId;
    private String parentAuthorName;
    private String parentUsername;
    private Integer likeCount;
    private Integer replyCount;
    private Boolean isLikedByCurrentUser;
    private List<FileRecordDTO> fileRecords;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}