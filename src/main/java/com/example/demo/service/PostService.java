package com.example.demo.service;

import com.example.demo.dto.post.CreatePostDTO;
import com.example.demo.dto.post_content.PostDTO;
import com.example.demo.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Repository
public interface PostService {
    Page<PostDTO> getAllPosts(Long eventId, Pageable pageable);

    PostDTO createPost(Long eventId, List<MultipartFile> multipartFiles, CreatePostDTO createPostDTO) throws IOException;

    void deletePost(Long postId);

    PostDTO updatePost(Long postId, CreatePostDTO updatePostDTO);

    PostDTO updatePostWithFiles(Long postId, String content, List<MultipartFile> files, List<Long> removeFileIds) throws IOException;
}
