package com.fintech.fintech_gateway.trust;

import com.fintech.fintech_gateway.persistence.PersistenceService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class TrustTierService {

    private final RedisTemplate<String, String> redisTemplate;
    private final PersistenceService persistenceService;

    private static final String TRUST_KEY = "trust:";
    private static final String BLOCK_COUNT_KEY = "blockcount:";
    private static final String BLACKLIST_KEY = "blacklist:";
    private static final String THROTTLE_COUNT_KEY = "throttlecount:";

    public enum TrustTier {
        HIGH, MEDIUM, LOW, BLACKLISTED
    }

    public TrustTierService(RedisTemplate<String, String> redisTemplate,
                            PersistenceService persistenceService) {
        this.redisTemplate = redisTemplate;
        this.persistenceService = persistenceService;
    }

    public TrustTier getTrustTier(String ip) {
        try {
            if (isBlacklisted(ip)) return TrustTier.BLACKLISTED;
            String tier = redisTemplate.opsForValue().get(TRUST_KEY + ip);
            if (tier == null) return TrustTier.MEDIUM;
            return TrustTier.valueOf(tier);
        } catch (Exception e) {
            System.err.println("Redis down — defaulting to MEDIUM trust");
            return TrustTier.MEDIUM;
        }
    }

    public void recordBlock(String ip) {
        try {
            Long blockCount = redisTemplate.opsForValue()
                    .increment(BLOCK_COUNT_KEY + ip);
            if (blockCount == null) return;
            System.out.println("Block count for " + ip + ": " + blockCount);
            if (blockCount >= 4) {
                blacklist(ip);
                persistenceService.saveTrustHistory(
                        ip, "LOW", "BLACKLISTED",
                        "Blocked 4+ times - permanent ban");
            } else {
                Duration cooldown = switch ((int) blockCount.longValue()) {
                    case 1 -> Duration.ofMinutes(1);
                    case 2 -> Duration.ofMinutes(10);
                    case 3 -> Duration.ofHours(1);
                    default -> Duration.ofHours(24);
                };
                redisTemplate.opsForValue().set(
                        TRUST_KEY + ip, TrustTier.LOW.name(), cooldown);
                persistenceService.saveTrustHistory(
                        ip, "MEDIUM", "LOW",
                        "Blocked " + blockCount + " times");
                System.out.println("IP " + ip +
                        " set to LOW trust for " + cooldown);
            }
        } catch (Exception e) {
            System.err.println("Redis down — cannot record block: "
                    + e.getMessage());
        }
    }

    public void recordGoodBehaviour(String ip) {
        try {
            TrustTier current = getTrustTier(ip);
            if (current == TrustTier.MEDIUM) {
                String key = "goodcount:" + ip;
                Long goodCount = redisTemplate.opsForValue().increment(key);
                redisTemplate.expire(key, Duration.ofHours(24));
                if (goodCount != null && goodCount >= 100) {
                    redisTemplate.opsForValue().set(
                            TRUST_KEY + ip, TrustTier.HIGH.name());
                    redisTemplate.delete(key);
                    System.out.println("IP " + ip + " upgraded to HIGH trust");
                }
            }
        } catch (Exception e) {
            System.err.println("Redis down — cannot record good behaviour");
        }
    }

    public boolean isBlacklisted(String ip) {
        try {
            return Boolean.TRUE.equals(
                    redisTemplate.hasKey(BLACKLIST_KEY + ip));
        } catch (Exception e) {
            System.err.println("Redis down — skipping blacklist check: "
                    + e.getMessage());
            return false;
        }
    }

    public int recordThrottle(String ip) {
        try {
            String key = THROTTLE_COUNT_KEY + ip;
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, Duration.ofMinutes(10));
            }
            System.out.println("Throttle count for " + ip + ": " + count);
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            System.err.println("Redis down — cannot record throttle");
            return 0;
        }
    }

    public void resetThrottleCount(String ip) {
        try {
            redisTemplate.delete(THROTTLE_COUNT_KEY + ip);
        } catch (Exception e) {
            System.err.println("Redis down — cannot reset throttle count");
        }
    }

    private void blacklist(String ip) {
        try {
            redisTemplate.opsForValue().set(BLACKLIST_KEY + ip, "PERMANENT");
            System.out.println("IP " + ip + " PERMANENTLY BLACKLISTED");
        } catch (Exception e) {
            System.err.println("Redis down — cannot blacklist IP");
        }
    }

    public int getRiskAdjustment(TrustTier tier) {
        return switch (tier) {
            case HIGH -> -10;
            case MEDIUM -> 0;
            case LOW -> +20;
            case BLACKLISTED -> 100;
        };
    }
}