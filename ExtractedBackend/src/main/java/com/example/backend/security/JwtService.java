package com.example.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        try {
            String username = extractClaim(token, Claims::getSubject);
            log.debug("JWT_SERVICE - Extracted username: {}", username);
            return username;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JWT_SERVICE - Failed to extract username from token: {}", ex.getMessage());
            return null;
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }


    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return buildToken(extraClaims, userDetails.getUsername(), expiration);
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        String token = Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
        log.debug("JWT_SERVICE - Token generated for subject: {} with expiration: {} ms", subject, expiration);
        return token;
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            boolean valid = username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token);
            if (valid) {
                log.debug("JWT_SERVICE - Token is valid for user: {}", username);
            } else {
                log.warn("JWT_SERVICE - Token validation failed. Username matches: {}, Not expired: {}", 
                        username != null && username.equals(userDetails.getUsername()), 
                        !isTokenExpired(token));
            }
            return valid;
        } catch (JwtException | IllegalArgumentException ex) {
            log.error("JWT_SERVICE - Token validation exception: {}", ex.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes;
        try {
            keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("jwt.secret must be a valid Base64-encoded value", ex);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("jwt.secret must decode to at least 32 bytes for HS256");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
