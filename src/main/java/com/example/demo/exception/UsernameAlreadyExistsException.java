package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class UsernameAlreadyExistsException extends BaseException{

    public UsernameAlreadyExistsException(String message) {
        super(message,HttpStatus.CONFLICT, "USER_ALREADY_EXISTS");
    }
}
