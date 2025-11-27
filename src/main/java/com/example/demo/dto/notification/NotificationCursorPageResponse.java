package com.example.demo.dto.notification;

import com.example.demo.dto.comment.CommentCursorPageResponse;
import com.example.demo.dto.comment.CommentDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationCursorPageResponse {
    List<NotificationDTO> notificationDTOS;
    String nextCursor;
    boolean hasNext;
    private int size;

    public static NotificationCursorPageResponse of(List<NotificationDTO> notificationDTOS, String nextCursor, boolean hasNext) {
        return new NotificationCursorPageResponse(
                notificationDTOS,
                nextCursor,
                hasNext,
                notificationDTOS.size()
        );
    }

}
