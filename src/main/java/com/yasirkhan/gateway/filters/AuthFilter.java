package com.yasirkhan.gateway.filters;

import com.yasirkhan.gateway.exceptions.TokenNotFoundException;
import com.yasirkhan.gateway.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthFilter implements GlobalFilter, Ordered {

    private final JwtUtils jwtUtils;

    public AuthFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        if (path.contains("/api/auth/login") ||
                path.contains("/api/auth/refreshToken")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange
                .getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new TokenNotFoundException("Missing Access Token");
        }

        String token = authHeader.substring(7);

        Claims claims = jwtUtils.validateToken(token);

        String role = claims.get("role", String.class);
        String userId = claims.get("userId").toString();

        ServerHttpRequest request =
                exchange.getRequest()
                        .mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Role", role)
                        .build();

        return chain.filter(
                exchange.mutate()
                        .request(request)
                        .build());
    }

    @Override
    public int getOrder() {
        return -1;
    }
}