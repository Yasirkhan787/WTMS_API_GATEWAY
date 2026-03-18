package com.yasirkhan.gateway.filters;

import com.yasirkhan.gateway.exceptions.TokenNotFoundException;
import com.yasirkhan.gateway.exceptions.UnauthorizedException;
import com.yasirkhan.gateway.utils.ExcludedPath;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private final ExcludedPath excludedPath;
    private final WebClient.Builder webClientBuilder;

    @Value("${app.security.internal-secret}")
    private String internalSecret;

    public AuthFilter(ExcludedPath excludedPath, WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.excludedPath = excludedPath;
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (excludedPath.predicate.test(request)) {
                String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    return Mono.error(new TokenNotFoundException("Missing Access Token"));
                }

                return webClientBuilder.build()
                        .get()
                        .uri("http://AUTH-SERVICE/auth/ping")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, clientResponse ->
                                clientResponse.bodyToMono(Map.class).flatMap(errorBody -> {
                                    String message =
                                            errorBody.getOrDefault(
                                                    "message", "Authentication Failed").toString();
                                    return Mono.error(new UnauthorizedException(message));
                                })
                        )
                        .bodyToMono(Map.class)
                        .flatMap(response -> {
                            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                    .header("X-User-Id", response.get("userId").toString())
                                    .header("X-Username", response.get("username").toString())
                                    .header("X-User-Role", response.get("role").toString())
                                    .header("X-Gateway-Secret", internalSecret)
                                    .build();
                            return chain.filter(exchange.mutate().request(mutatedRequest).build());
                        });
            }

            ServerHttpRequest publicRequest = request.mutate()
                    .header("X-Gateway-Secret", internalSecret)
                    .build();

            return chain.filter(exchange.mutate().request(publicRequest).build());
        };
    }

    public static class Config {}
}