package com.example.demo.exception;

import org.springframework.http.HttpStatus;

public class UnsupportedOperationException extends BaseException{
    public UnsupportedOperationException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "UNSUPPORTED_ACCOUNT");
    }
}
