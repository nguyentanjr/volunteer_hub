package com.example.demo.exception;

import org.springframework.http.HttpStatus;

public class TokenExpiredException extends BaseException{
    public TokenExpiredException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "TOKEN_OVERDUE");
    }
}
