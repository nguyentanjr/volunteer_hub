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
    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "commentCount", ignore = true)
    @Mapping(target = "isLikedByCurrentUser", ignore = true)
    @Mapping(target = "files", source = "fileRecords")
    @Mapping(target = "isPinned", source = "pinned")
    PostDTO toPostDTO(Post post);


}
