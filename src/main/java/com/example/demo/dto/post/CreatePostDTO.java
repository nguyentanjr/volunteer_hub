package com.example.demo.dto.post;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePostDTO {

    @Size(max = 5000, message = "Content must not exceed 5000 characters")
    private String content;

}
