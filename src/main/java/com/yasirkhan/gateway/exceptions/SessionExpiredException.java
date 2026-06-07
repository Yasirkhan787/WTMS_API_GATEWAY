package com.yasirkhan.gateway.exceptions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

public class SessionExpiredException extends RuntimeException{

    private String message;

    public SessionExpiredException(String message){
        super(message);
        this.message = message;
    }

}