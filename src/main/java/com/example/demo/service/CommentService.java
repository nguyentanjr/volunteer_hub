package com.example.demo.service;

import com.example.demo.dto.comment.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface CommentService {

    CommentDTO createComment(CreateCommentDTO createCommentDTO);

    CommentDTO updateComment(Long commentId, UpdateCommentDTO updateCommentDTO);

    void deleteComment(Long commentId);

    CommentCursorPageResponse getCommentsByPostId(Long postId, String cursor, int limit, CommentSortType commentSortType);

    CommentCursorPageResponse getRepliesByCommentId(Long commentId, String cursor, int limit);

}
