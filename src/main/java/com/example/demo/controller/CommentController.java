package com.example.demo.controller;

import com.example.demo.dto.comment.CommentCursorPageResponse;
import com.example.demo.dto.comment.CommentDTO;
import com.example.demo.dto.comment.CommentSortType;
import com.example.demo.dto.comment.CreateCommentDTO;
import com.example.demo.dto.comment.UpdateCommentDTO;
import com.example.demo.dto.common.ApiResponse;
import com.example.demo.model.Comment;
import com.example.demo.repository.CommentRepository;
import com.example.demo.service.CommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
private final CommentRepository commentRepository;

    @PostMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<CommentDTO>> createComment(
            @PathVariable Long postId,
            @RequestBody CreateCommentDTO createCommentDTO) throws IOException {
        return ResponseEntity.ok(ApiResponse.created(commentService.createComment(createCommentDTO)));
    }

    @PostMapping(value = "/posts/{postId}/with-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CommentDTO>> createCommentWithFiles(
            @PathVariable Long postId,
            @RequestParam("createCommentDTO") String createCommentDTOJson,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) throws IOException {
        
        ObjectMapper objectMapper = new ObjectMapper();
        CreateCommentDTO createCommentDTO = objectMapper.readValue(createCommentDTOJson, CreateCommentDTO.class);
        
        return ResponseEntity.ok(ApiResponse.created(commentService.createCommentWithFiles(createCommentDTO, files)));
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<ApiResponse<CommentDTO>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentDTO updateCommentDTO) {
        return ResponseEntity.ok(ApiResponse.success(commentService.updateComment(commentId, updateCommentDTO)));
    }

    @PutMapping(value = "/{commentId}/with-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CommentDTO>> updateCommentWithFiles(
            @PathVariable Long commentId,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) throws IOException {
        return ResponseEntity.ok(ApiResponse.success(commentService.updateCommentWithFiles(commentId, content, files)));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/posts/{postId}/latest")
    public ResponseEntity<ApiResponse<CommentCursorPageResponse>> getLatestComments(
            @PathVariable Long postId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(
                ApiResponse.success(commentService.getCommentsByPostId(postId, cursor, limit, CommentSortType.LATEST)));
    }

    @GetMapping("/{commentId}/replies")
    public ResponseEntity<ApiResponse<CommentCursorPageResponse>> getCommentReplies(
            @PathVariable Long commentId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(
                ApiResponse.success(commentService.getRepliesByCommentId(commentId, cursor, limit)));
    }

    @GetMapping("/{commentId}/replies/flattened")
    public ResponseEntity<ApiResponse<CommentCursorPageResponse>> getAllRepliesFlattened(
            @PathVariable Long commentId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(
                ApiResponse.success(commentService.getAllRepliesFlattened(commentId, cursor, limit)));
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Comment>> getCommentById(@PathVariable Long commentId) {
        return ResponseEntity.ok(ApiResponse.success(commentRepository.getCommentById(commentId)));
    }

}

