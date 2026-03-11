package com.yasirkhan.gateway.utils;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.function.Predicate;

@Component
public class ExcludedPath {

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public static final List<String> endPoints = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/refreshToken",
            "/api/v1/user/add"
    );

    public Predicate<ServerHttpRequest> predicate = request -> {

        String requestPath = request.getURI().getPath();

        return endPoints
                .stream()
                .noneMatch(uri -> antPathMatcher.match(uri, requestPath));
    };
}