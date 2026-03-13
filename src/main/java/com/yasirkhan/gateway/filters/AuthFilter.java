package com.yasirkhan.gateway.filters;

import com.yasirkhan.gateway.exceptions.TokenNotFoundException;
import com.yasirkhan.gateway.exceptions.UnauthorizedException;
import com.yasirkhan.gateway.utils.ExcludedPath;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
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
            String path = exchange.getRequest().getURI().getPath();

            System.out.println(path);
            // If the path is NOT in the excluded list, proceed with security
            if (excludedPath.predicate.test(exchange.getRequest())) {

                String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    throw new TokenNotFoundException("Missing Access Token");
                }

                return webClientBuilder.build()
                        .get()
                        .uri("http://AUTH-SERVICE/auth/ping")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .flatMap(response -> {
                            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                    .header("X-User-Id", response.get("userId").toString())
                                    .header("X-Username", response.get("username").toString())
                                    .header("X-User-Role", response.get("role").toString())
                                    .header("X-Gateway-Secret", internalSecret)
                                    .build();
                            return chain.filter(exchange.mutate().request(mutatedRequest).build());
                        })
                        .onErrorResume(e -> Mono.error(new UnauthorizedException("Invalid Token")));
            }

            // If path is EXCLUDED, still add the secret so service doesn't block it
            ServerHttpRequest publicRequest = exchange.getRequest().mutate()
                    .header("X-Gateway-Secret", internalSecret)
                    .build();

            return chain.filter(exchange.mutate().request(publicRequest).build());
        };
    }

    public static class Config {}
}