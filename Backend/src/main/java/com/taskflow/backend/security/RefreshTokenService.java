package com.taskflow.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;
    private final long refreshTokenExpiryDays;

    public RefreshTokenService(StringRedisTemplate redisTemplate,
                               @Value("${jwt.refresh-token-expiry-days}") long refreshTokenExpiryDays) {
        this.redisTemplate = redisTemplate;
        this.refreshTokenExpiryDays = refreshTokenExpiryDays;
    }

    /**
     * Issues a new opaque refresh token (not a JWT — just a random string) and
     * stores it in Redis keyed by userId, overwriting any previous one.
     * Opaque (not a signed JWT) is deliberate: a refresh token only ever needs
     * one lookup, server-side, against the value WE stored — no need for
     * self-contained claims, and it can't be forged even if someone knew the
     * userId, since it must match exactly what's in Redis.
     */
    public String issueToken(Long userId) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(KEY_PREFIX + userId, token, Duration.ofDays(refreshTokenExpiryDays));
        return token;
    }

    /**
     * Validates the presented refresh token against what's stored for this user.
     * Redis TTL alone handles expiry (the key simply won't exist past 7 days) —
     * no manual expiry-timestamp comparison needed.
     */
    public boolean isValid(Long userId, String presentedToken) {
        String stored = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        return stored != null && stored.equals(presentedToken);
    }

    /**
     * Rotation: called after a successful refresh, replaces the old token with
     * a new one. Narrows the window an intercepted refresh token stays useful —
     * standard practice, not overkill for a practice project given how little
     * it costs to implement.
     */
    public String rotate(Long userId) {
        return issueToken(userId); // overwrite is implicit in issueToken
    }

    /** Called on logout — immediate revocation, one Redis DEL. */
    public void revoke(Long userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }
}