package com.yasirkhan.gateway.filters;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class InternalSecurityFilter implements GlobalFilter, Ordered {

    @Value("${app.security.internal-secret}")
    private String GATEWAY_SECRET = "";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Add the secret header to the request being forwarded
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header("X-Gateway-Secret", GATEWAY_SECRET)
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return -1; // Run this early in the filter chain
    }
}