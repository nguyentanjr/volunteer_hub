package com.example.demo.exception;

import org.springframework.http.HttpStatus;

public class EventDuplicateException extends BaseException{

    public EventDuplicateException(String message) {
        super(message, HttpStatus.BAD_REQUEST,"DUPLICATE_EVENT");
    }
}
