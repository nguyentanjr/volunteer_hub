package com.example.demo.dto.error;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ErrorResponse {

    private LocalDateTime localDateTime;
    private String error;
    private String message;
    private String path;

    public ErrorResponse() {
        this.localDateTime = LocalDateTime.now();
    }
    public ErrorResponse(int status, String error, String message, String path) {
        this();
        this.error = error;
        this.message = message;
        this.path = path;
    }
}
