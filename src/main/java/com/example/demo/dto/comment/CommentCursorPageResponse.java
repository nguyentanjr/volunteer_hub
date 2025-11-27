package com.example.demo.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentCursorPageResponse {
    private List<CommentDTO> comments;
    private String nextCursor; // Cursor for the next page (null if no more pages)
    private boolean hasNext;
    private int size;

    public static CommentCursorPageResponse of(List<CommentDTO> comments, String nextCursor, boolean hasNext) {
        return new CommentCursorPageResponse(
                comments,
                nextCursor,
                hasNext,
                comments.size()
        );
    }
}
