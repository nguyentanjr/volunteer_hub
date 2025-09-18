package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class EmailAlreadyExistsException extends BaseException{

    public EmailAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS");
    }
}
