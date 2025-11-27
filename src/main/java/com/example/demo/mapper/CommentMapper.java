package com.example.demo.mapper;

import com.example.demo.dto.comment.CommentDTO;
import com.example.demo.model.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface CommentMapper {
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "postId", source = "post.id")
    @Mapping(target = "parentCommentId", source = "parentComment.id")
    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "replyCount", ignore = true)
    CommentDTO toCommentDTO(Comment comment);
}
