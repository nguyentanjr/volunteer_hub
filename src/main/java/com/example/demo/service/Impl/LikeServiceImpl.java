package com.example.demo.service.Impl;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Comment;
import com.example.demo.model.Like;
import com.example.demo.model.Post;
import com.example.demo.model.User;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.LikeRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.service.LikeService;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final UserService userService;

    @Transactional
    @CacheEvict(value = "dashboard", key = "'volunteer:' + #root.target.getCurrentUser().id")
    public void likePost(Long postId) {
        log.info("Toggling like for post: {}", postId);

        User user = userService.getCurrentUser();
        Like existingLike = likeRepository.findByPostIdAndUserId(postId, user.getId());

        if (existingLike != null) {
            likeRepository.delete(existingLike);
            log.info("User {} unliked post {}", user.getId(), postId);
            return;
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        Like newLike = new Like()
                .setPost(post)
                .setUser(user);

        likeRepository.save(newLike);
        log.info("User {} liked post {}", user.getId(), postId);
    }


    @Transactional
    @CacheEvict(value = "dashboard", key = "'volunteer:' + #root.target.getCurrentUser().id")
    public void likeComment(Long commentId) {
        User user = userService.getCurrentUser();

        Like existingLike = likeRepository.findByCommentIdAndUserId(commentId, user.getId());

        if (existingLike != null) {
            likeRepository.delete(existingLike);
            log.info("User {} unliked comment {}", user.getId(), commentId);
        } else {
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

            Like like = new Like()
                    .setComment(comment)
                    .setUser(user);

            likeRepository.save(like);
            log.info("User {} liked comment {}", user.getId(), commentId);
        }
    }

    public void unLikeComment(Long commentId) {
        log.info("Unlike comment: {}", commentId);

        Like like = likeRepository.findByCommentIdAndUserId(commentId, userService.getCurrentUser().getId());
        likeRepository.delete(like);
    }

    public int countLikesByPostId(Long postId) {
        return likeRepository.countLikesByPostId(postId);
    }

    public int countLikesByCommentId(Long commentId) {
        return likeRepository.countLikesByCommentId(commentId);
    }

    public User getCurrentUser() {
        return userService.getCurrentUser();
    }
}
