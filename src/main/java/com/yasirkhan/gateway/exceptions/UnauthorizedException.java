package com.yasirkhan.gateway.exceptions;

public class UnauthorizedException extends RuntimeException{

    private String message;

    public UnauthorizedException(String message){
        super(message);
        this.message = message;
    }
}
