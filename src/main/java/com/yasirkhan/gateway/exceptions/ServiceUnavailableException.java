package com.yasirkhan.gateway.exceptions;

public class ServiceUnavailableException extends RuntimeException{

    private String message;

    public ServiceUnavailableException(String message){
        super(message);
        this.message = message;
    }

}