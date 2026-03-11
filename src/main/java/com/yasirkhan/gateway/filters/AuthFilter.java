package com.yasirkhan.gateway.filters;

import com.yasirkhan.gateway.exceptions.TokenNotFoundException;
import com.yasirkhan.gateway.exceptions.UnauthorizedException;
import com.yasirkhan.gateway.utils.ExcludedPath;
import com.yasirkhan.gateway.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private final ExcludedPath excludedPath;
    private final JwtUtils jwtUtils;

    public AuthFilter(ExcludedPath excludedPath, JwtUtils jwtUtils) {
        super(Config.class);
        this.excludedPath = excludedPath;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public GatewayFilter apply(Config config) {

        return (ServerWebExchange exchange, GatewayFilterChain chain) -> {

            String path = exchange.getRequest().getURI().getPath();
            System.out.println("DEBUG: Gateway received request for path: " + path);

            // Skip authentication for excluded paths
            if (!excludedPath.predicate.test(exchange.getRequest())) {
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


            try {
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
                                .build()
                );
            } catch (ExpiredJwtException e) {
                throw new UnauthorizedException(e.getMessage());
            }catch (MalformedJwtException e) {
                throw new UnauthorizedException(e.getMessage());
            }catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static class Config {
    }
}