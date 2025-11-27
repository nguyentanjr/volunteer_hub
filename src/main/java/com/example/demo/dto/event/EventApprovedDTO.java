package com.example.demo.dto.event;

import com.example.demo.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventApprovedDTO {
    private Long id;
    private User creator;
}
