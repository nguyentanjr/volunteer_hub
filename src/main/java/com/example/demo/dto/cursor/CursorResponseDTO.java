package com.example.demo.dto.cursor;

import com.example.demo.dto.comment.CommentDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CursorResponseDTO {
    private List<CommentDTO> data;
    private String nextCursor; //null if no more page
    private boolean hasNext;
}
