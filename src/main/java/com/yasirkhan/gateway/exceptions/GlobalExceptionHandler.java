package com.yasirkhan.gateway.exceptions;

import com.yasirkhan.gateway.responses.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException ex, ServerHttpRequest request){
        ErrorResponse response =
                ErrorResponse
                        .builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .timestamp(LocalDateTime.now())
                        .error("UNAUTHORIZED")
                        .path(request.getURI().getPath())
                        .build();

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(TokenNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTokenNotFoundException(TokenNotFoundException ex, ServerHttpRequest request){
        ErrorResponse response =
                ErrorResponse
                        .builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.NOT_FOUND.value())
                        .timestamp(LocalDateTime.now())
                        .error("NOT FOUND")
                        .path(request.getURI().getPath())
                        .build();

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
}
