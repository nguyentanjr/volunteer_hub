package com.example.demo.controller;

import com.example.demo.dto.common.ApiResponse;
import com.example.demo.dto.post.CreatePostDTO;
import com.example.demo.dto.post_content.PostDTO;
import com.example.demo.model.Post;
import com.example.demo.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping(value = "/events/{eventId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostDTO>> createPostWithFiles(
            @PathVariable Long eventId,
            @RequestParam("content") String content,
            @RequestParam(value = "files", required = false) List<MultipartFile> fileList) throws IOException {
        CreatePostDTO createPostDTO = new CreatePostDTO();
        createPostDTO.setContent(content);
        return ResponseEntity.ok(ApiResponse.created(postService.createPost(eventId, fileList, createPostDTO)));
    }

    @PostMapping(value = "/events/{eventId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PostDTO>> createPostJson(
            @PathVariable Long eventId,
            @RequestBody CreatePostDTO createPostDTO) throws IOException {
        return ResponseEntity.ok(ApiResponse.created(postService.createPost(eventId, null, createPostDTO)));
    }

    @GetMapping("/events/{eventId}")
    public ResponseEntity<ApiResponse<Page<PostDTO>>> viewAllPosts(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(ApiResponse.success(postService.getAllPosts(eventId, pageable)));

    }

//    @GetMapping("/{postId}")
//    public ResponseEntity<ApiResponse<PostDTO>> getPostById(@PathVariable Long postId) {
//        return ResponseEntity.ok(ApiResponse.success(postService.getPostById(postId)));
//    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long postId) {
        postService.deletePost(postId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDTO>> updatePost(
            @PathVariable Long postId,
            @RequestBody CreatePostDTO updatePostDTO) {
        return ResponseEntity.ok(ApiResponse.success(postService.updatePost(postId, updatePostDTO)));
    }

    @PutMapping(value = "/{postId}/with-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostDTO>> updatePostWithFiles(
            @PathVariable Long postId,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "removeFileIds", required = false) List<Long> removeFileIds) throws IOException {
        return ResponseEntity.ok(ApiResponse.success(postService.updatePostWithFiles(postId, content, files, removeFileIds)));
    }
}
