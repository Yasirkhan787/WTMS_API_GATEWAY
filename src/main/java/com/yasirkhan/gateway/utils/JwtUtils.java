package com.yasirkhan.gateway.utils;

import com.yasirkhan.gateway.exceptions.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtils {

    private final String SECRET;

    public JwtUtils(@Value("${jwt.secret}") String SECRET) {
        this.SECRET = SECRET;
    }

    public Claims validateToken(String token){

        if (isTokenExpired(token)){
            throw  new UnauthorizedException("Access Token Expired");
        }

        return extractAllClaims(token);
    }

    public Claims extractAllClaims(String token){
        return Jwts
                .parser()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver){
        final Claims claims = validateToken(token);
        return claimResolver.apply(claims);
    }

    private Boolean isTokenExpired(String token){
        Date expirationDate =
                extractClaim(token, Claims::getExpiration);
        return (expirationDate.before(new Date()));
    }
    private Key getSignKey(){
        byte[] key = SECRET.getBytes();

        return Keys.hmacShaKeyFor(key);
    }
}
