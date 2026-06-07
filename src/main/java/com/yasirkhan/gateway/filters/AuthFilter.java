package com.yasirkhan.gateway.filters;

import com.yasirkhan.gateway.exceptions.SessionExpiredException;
import com.yasirkhan.gateway.exceptions.UnauthorizedException;
import com.yasirkhan.gateway.utils.ExcludedPath;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private final ExcludedPath excludedPath;
    private final ReactiveStringRedisTemplate redisTemplate;

    @Value("${jwt.public-key.path}")
    private Resource publicKeyResource;
    private PublicKey publicKey;

    public AuthFilter(ExcludedPath excludedPath, ReactiveStringRedisTemplate redisTemplate) {
        super(Config.class);
        this.excludedPath = excludedPath;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() throws Exception {
        byte[] pubKeyBytes = publicKeyResource.getInputStream().readAllBytes();
        String pubKeyString = new String(pubKeyBytes)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decodedPubKey = Base64.getDecoder().decode(pubKeyString);
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(decodedPubKey);
        this.publicKey = KeyFactory.getInstance("RSA").generatePublic(pubKeySpec);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (!excludedPath.predicate.test(request)) {
                return chain.filter(exchange);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Mono.error(new UnauthorizedException("Missing or Invalid Authorization Header"));
            }

            String token = authHeader.substring(7);

            try {

                Claims claims = Jwts.parser()
                        .verifyWith(publicKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String userId = String.valueOf(claims.get("userId"));
                Integer tokenVersion = claims.get("tokenVersion", Integer.class);

                return redisTemplate.opsForValue().get("user:token:" + userId)
                        .flatMap(cachedVersion -> {
                            if (cachedVersion == null || !cachedVersion.equals(String.valueOf(tokenVersion))) {
                                return Mono.error(new SessionExpiredException("Session Expired: Logged in from another device."));
                            }
                            // Token is valid and matches Redis version, forward to downstream service
                            return chain.filter(exchange);
                        })
                        .switchIfEmpty(Mono.error(new SessionExpiredException("Session Expired: No active session found.")));

            } catch (Exception e) {
                return Mono.error(new UnauthorizedException("Invalid or expired token"));
            }
        };
    }

    public static class Config {}
}