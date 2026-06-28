package com.fintech.fintech_gateway.feedback;

import com.fintech.fintech_gateway.persistence.PersistenceService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class FeedbackService {

    private final RedisTemplate<String, String> redisTemplate;
    private final PersistenceService persistenceService;

    private static final String TRUST_KEY = "trust:";
    private static final String BLOCK_COUNT_KEY = "blockcount:";
    private static final String BLACKLIST_KEY = "blacklist:";

    public FeedbackService(RedisTemplate<String, String> redisTemplate,
                           PersistenceService persistenceService) {
        this.redisTemplate = redisTemplate;
        this.persistenceService = persistenceService;
    }

    public void handleFalsePositive(String ip) {
        // Step 1 — Remove from blacklist in Redis
        redisTemplate.delete(BLACKLIST_KEY + ip);

        // Step 2 — Reset block count in Redis
        redisTemplate.delete(BLOCK_COUNT_KEY + ip);

        // Step 3 — Reset trust tier to MEDIUM in Redis
        redisTemplate.delete(TRUST_KEY + ip);

        // Step 4 — Save to MySQL for audit trail
        persistenceService.saveTrustHistory(
                ip,
                "BLACKLISTED",
                "MEDIUM",
                "False positive reported by ops team - trust reset"
        );

        System.out.println("FALSE POSITIVE: IP " + ip +
                " removed from blacklist and reset to MEDIUM trust");
    }
}