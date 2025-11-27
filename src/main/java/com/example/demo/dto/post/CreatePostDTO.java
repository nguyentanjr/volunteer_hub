package com.example.demo.dto.post;

import com.example.demo.dto.file.FileRecordDTO;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePostDTO {

    @NotBlank(message = "Content cannot be empty")
    @Size(max = 5000, message = "Content must not exceed 5000 characters")
    private String content;

    private List<FileRecordDTO> files;  // Thêm

}
