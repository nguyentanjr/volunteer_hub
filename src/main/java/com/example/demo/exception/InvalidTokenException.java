package com.example.demo.exception;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends BaseException{
    public InvalidTokenException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "TOKEN_NOT_FOUND");
    }
}
