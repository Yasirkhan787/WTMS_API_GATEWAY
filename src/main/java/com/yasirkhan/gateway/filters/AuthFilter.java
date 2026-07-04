package com.yasirkhan.gateway.filters;

import com.yasirkhan.gateway.exceptions.UnauthorizedException;
import com.yasirkhan.gateway.utils.ExcludedPath;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private final ExcludedPath excludedPath;

    public AuthFilter(ExcludedPath excludedPath) {
        super(Config.class);
        this.excludedPath = excludedPath;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Check if the route requires authentication
            if (excludedPath.predicate.test(request)) {
                String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

                // NEW: Extract the query parameter for WebSocket handshakes
                String tokenParam = request.getQueryParams().getFirst("token");

                boolean hasValidHeader = authHeader != null && authHeader.startsWith("Bearer ");
                boolean hasValidParam = tokenParam != null && !tokenParam.isEmpty();

                // Check if EITHER the header or the query parameter is present
                if (!hasValidHeader && !hasValidParam) {
                    return Mono.error(new UnauthorizedException("Missing Authorization Header or Token Parameter"));
                }
            }

            return chain.filter(exchange);
        };
    }

    public static class Config {}
}