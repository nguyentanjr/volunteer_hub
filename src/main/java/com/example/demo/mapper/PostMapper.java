package com.example.demo.mapper;

import com.example.demo.dto.post.CreatePostDTO;
import com.example.demo.dto.post_content.PostDTO;
import com.example.demo.model.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface PostMapper {

    @Mapping(target = "userId", source = "postCreator.id")
    @Mapping(target = "username", source = "postCreator.username")
    @Mapping(target = "userAvatarUrl", source = "postCreator.imageUrl")
    @Mapping(target = "userFullName", expression = "java(fullName(post))")
    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "commentCount", ignore = true)
    @Mapping(target = "isLikedByCurrentUser", ignore = true)
    @Mapping(target = "files", source = "fileRecords")
    @Mapping(target = "isPinned", source = "pinned")
    PostDTO toPostDTO(Post post);

    default String fullName(Post post) {
        if (post == null || post.getPostCreator() == null) return null;
        var u = post.getPostCreator();
        String first = u.getFirstName();
        String last = u.getLastName();
        String full = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
        if (!full.isBlank()) return full;
        return u.getUsername();
    }
}
