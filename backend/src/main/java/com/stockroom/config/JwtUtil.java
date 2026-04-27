package com.stockroom.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;

/**
 * JWT utility — generates and validates tokens.
 * The signing secret comes from JWT_SECRET env var (K8s Secret resource).
 * Token expiry comes from JWT_EXPIRY_HOURS env var (K8s ConfigMap).
 *
 * K8s teaching point:
 *   Rotate JWT_SECRET in the Secret → rolling restart → all existing tokens
 *   are instantly invalidated. Great demo of secret rotation impact.
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long      expiryMs;

    public JwtUtil(AppProperties props) {
        byte[] keyBytes = props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        // HS256 requires at least 32 bytes — pad if needed (for short dev secrets)
        byte[] paddedKey = Arrays.copyOf(keyBytes, Math.max(keyBytes.length, 32));
        this.key      = Keys.hmacShaKeyFor(paddedKey);
        this.expiryMs = props.getJwt().getExpiryHours() * 3600L * 1000L;
    }

    public String generateToken(String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiryMs))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
