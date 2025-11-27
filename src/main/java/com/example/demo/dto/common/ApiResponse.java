package com.example.demo.dto.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter

public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final String timestamp;
    private final Integer status;

    private ApiResponse(boolean success, String message, T data, Integer status) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now().toString();
        this.status = status;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data, HttpStatus.OK.value());
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, HttpStatus.OK.value());
    }

    public static <T> ApiResponse<T> error(String message, HttpStatus status) {
        return new ApiResponse<>(false, message, null, status.value());
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, "Resource created successfully", data,
                HttpStatus.CREATED.value());
    }

    /** Create successful creation response with custom message */
    public static <T> ApiResponse<T> created(T data, String message) {
        return new ApiResponse<>(true, message, data, HttpStatus.CREATED.value());
    }

    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(false, message, null, HttpStatus.BAD_REQUEST.value());
    }

    /** Create not found error */
    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(false, message, null, HttpStatus.NOT_FOUND.value());
    }

    /** Create unauthorized error */
    public static <T> ApiResponse<T> unauthorized(String message) {
        return new ApiResponse<>(false, message, null, HttpStatus.UNAUTHORIZED.value());
    }

    /** Create forbidden error */
    public static <T> ApiResponse<T> forbidden(String message) {
        return new ApiResponse<>(false, message, null, HttpStatus.FORBIDDEN.value());
    }
}
