package com.example.demo.controller;

import com.example.demo.dto.common.ApiResponse;
import com.example.demo.model.Like;
import com.example.demo.service.LikeService;
import com.example.demo.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/likes")
public class LikeController {

    private final LikeService likeService;
    private final PostService postService;

    @PostMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<?>> likeComment(@PathVariable Long commentId) {
        likeService.likeComment(commentId);
        return ResponseEntity.ok(ApiResponse.success(null,"Like success comment: " + commentId));
    }

    @GetMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Integer>> countLikesByCommentId(@PathVariable Long commentId) {
        return ResponseEntity.ok(ApiResponse.success(likeService.countLikesByCommentId(commentId)));
    }

    @PostMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<?>> likePost(@PathVariable Long postId) {
        likeService.likePost(postId);
        return ResponseEntity.ok(ApiResponse.success(null, "Like success post: " + postId));
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Integer>> countLikesByPostId(@PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.success(likeService.countLikesByPostId(postId)));
    }


}
