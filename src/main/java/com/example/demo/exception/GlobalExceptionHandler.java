package com.example.demo.exception;

import com.example.demo.dto.error.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler{

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                e.getHttpStatus().value(),
                e.getErrorCode(),
                e.getMessage(),
                request.getDescription(false).replace("uri=","")
        );
        return new ResponseEntity<>(errorResponse, e.getHttpStatus());
    }


}
