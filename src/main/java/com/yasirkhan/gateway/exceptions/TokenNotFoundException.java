package com.yasirkhan.gateway.exceptions;

public class TokenNotFoundException extends RuntimeException{

    private String message;

    public TokenNotFoundException(String message){
        super(message);
        this.message = message;
    }
}