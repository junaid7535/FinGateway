package com.fintech.fintech_gateway.risk;

import com.fintech.fintech_gateway.trust.TrustTierService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.LocalTime;

@Component
public class RiskScoringEngine {

    private final RedisTemplate<String, String> redisTemplate;
    private final TrustTierService trustTierService;

    public RiskScoringEngine(RedisTemplate<String, String> redisTemplate,
                             TrustTierService trustTierService) {
        this.redisTemplate = redisTemplate;
        this.trustTierService = trustTierService;
    }

    public int calculateScore(HttpServletRequest request) {
        int score = 0;
        String ip = request.getRemoteAddr();

        score += scoreEndpointSensitivity(request.getRequestURI());
        score += scoreRequestFrequency(ip);
        score += scoreTimeOfRequest();
        score += scorePayloadSize(request);

        TrustTierService.TrustTier tier = trustTierService.getTrustTier(ip);
        score += trustTierService.getRiskAdjustment(tier);

        return Math.min(Math.max(score, 0), 100);
    }

    private int scoreEndpointSensitivity(String uri) {
        if (uri.contains("/payments")) return 30;
        if (uri.contains("/account/balance")) return 10;
        if (uri.contains("/account/profile")) return 5;
        return 5;
    }

    private int scoreRequestFrequency(String ip) {
        try {
            String key = "rate:" + ip;
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, Duration.ofMinutes(1));
            }
            if (count == null) return 0;
            if (count > 45) return 40;
            if (count > 30) return 20;
            if (count > 10) return 10;
            return 0;
        } catch (Exception e) {
            System.err.println("Redis down — skipping frequency score");
            return 0;
        }
    }

    private int scoreTimeOfRequest() {
        int hour = LocalTime.now().getHour();
        if (hour >= 23 || hour <= 5) return 15;
        return 0;
    }

    private int scorePayloadSize(HttpServletRequest request) {
        int contentLength = request.getContentLength();
        if (contentLength > 10000) return 20;
        if (contentLength > 5000) return 10;
        return 0;
    }
}