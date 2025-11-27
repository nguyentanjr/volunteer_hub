package com.example.demo.service;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Like;
import com.example.demo.model.Post;

public interface LikeService {
    void likePost(Long postId);

    void likeComment(Long commentId);

    int countLikesByPostId(Long postId);

    int countLikesByCommentId(Long postId);
}
