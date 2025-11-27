package com.example.demo.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateEventDTO {

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

    private List<String> tags;

}


