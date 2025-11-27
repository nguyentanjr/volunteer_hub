package com.example.demo.dto.event;

import com.example.demo.model.Event;
import com.example.demo.model.User;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventUpdateDTO {

    private Long id;

    @NotBlank
    @Size(min = 10, max = 50)
    private String title;

    @NotBlank
    @Size(min = 10, max = 2000)
    private String description;

    @NotNull(message = "Date cannot be null")
    private LocalDateTime date;

    @NotBlank
    private String location;

    @NotNull(message = "Max participants is required")
    @Min(value = 1, message = "Must have at least 1 participant")
    private Integer maxParticipants;
}
