package com.fintech.fintech_gateway.idempotency;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class IdempotencyService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final Duration TTL = Duration.ofHours(24);

    public IdempotencyService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isProcessed(String key) {
        String value = redisTemplate.opsForValue().get("idempotency:" + key);
        return value != null && !value.equals(IN_PROGRESS);
    }

    public boolean isInProgress(String key) {
        String value = redisTemplate.opsForValue().get("idempotency:" + key);
        return IN_PROGRESS.equals(value);
    }

    public void markInProgress(String key) {
        redisTemplate.opsForValue().set("idempotency:" + key, IN_PROGRESS, TTL);
    }

    public void markCompleted(String key, String response) {
        redisTemplate.opsForValue().set("idempotency:" + key, response, TTL);
    }

    public String getStoredResponse(String key) {
        return redisTemplate.opsForValue().get("idempotency:" + key);
    }
}