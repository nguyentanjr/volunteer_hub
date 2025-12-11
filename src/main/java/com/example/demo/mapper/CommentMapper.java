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
    @Mapping(target = "userFullName", expression = "java(comment.getUser() != null ? comment.getUser().getFirstName() + \" \" + comment.getUser().getLastName() : null)")
    @Mapping(target = "userAvatarUrl", source = "user.imageUrl")
    @Mapping(target = "postId", source = "post.id")
    @Mapping(target = "parentCommentId", source = "parentComment.id")
    @Mapping(target = "parentAuthorName", expression = "java(parentName(comment))")
    @Mapping(target = "parentUsername", expression = "java(comment.getParentComment() != null && comment.getParentComment().getUser() != null ? comment.getParentComment().getUser().getUsername() : null)")
    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "replyCount", ignore = true)
    @Mapping(target = "isLikedByCurrentUser", ignore = true)
    @Mapping(target = "fileRecords", ignore = true)
    CommentDTO toCommentDTO(Comment comment);

    // Helper to safely build parent author display name
    default String parentName(Comment comment) {
        if (comment == null || comment.getParentComment() == null || comment.getParentComment().getUser() == null) {
            return null;
        }
        var user = comment.getParentComment().getUser();
        String first = user.getFirstName();
        String last = user.getLastName();
        String username = user.getUsername();

        String full = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
        if (!full.isBlank()) return full;
        return username;
    }
}
