package com.example.demo.service;

import com.example.demo.dto.comment.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface CommentService {

    CommentDTO createComment(CreateCommentDTO createCommentDTO) throws IOException;

    CommentDTO createCommentWithFiles(CreateCommentDTO createCommentDTO, List<MultipartFile> files) throws IOException;

    CommentDTO updateComment(Long commentId, UpdateCommentDTO updateCommentDTO);

    void deleteComment(Long commentId);

    CommentDTO updateCommentWithFiles(Long commentId, String content, List<MultipartFile> files) throws IOException;

    CommentCursorPageResponse getCommentsByPostId(Long postId, String cursor, int limit, CommentSortType commentSortType);

    CommentCursorPageResponse getRepliesByCommentId(Long commentId, String cursor, int limit);
    
    // Get all replies flattened (all nested replies at same level)
    CommentCursorPageResponse getAllRepliesFlattened(Long commentId, String cursor, int limit);

}
