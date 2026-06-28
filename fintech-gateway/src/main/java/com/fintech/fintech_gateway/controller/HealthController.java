package com.fintech.fintech_gateway.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    private final RedisTemplate<String, String> redisTemplate;
    private final DataSource dataSource;

    public HealthController(RedisTemplate<String, String> redisTemplate,
                            DataSource dataSource) {
        this.redisTemplate = redisTemplate;
        this.dataSource = dataSource;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();

        // Check Redis
        try {
            redisTemplate.opsForValue().get("health-check");
            status.put("redis", "UP");
        } catch (Exception e) {
            status.put("redis", "DOWN");
            status.put("redis_error", "Cannot connect to Redis");
        }

        // Check MySQL
        try (Connection conn = dataSource.getConnection()) {
            status.put("mysql", conn.isValid(2) ? "UP" : "DOWN");
        } catch (Exception e) {
            status.put("mysql", "DOWN");
            status.put("mysql_error", "Cannot connect to MySQL");
        }

        // Check Backend via actuator
        try {
            org.springframework.web.client.RestClient client =
                    org.springframework.web.client.RestClient.create();
            client.get()
                    .uri("http://localhost:8080/actuator/health")
                    .retrieve()
                    .toBodilessEntity();
            status.put("backend", "UP");
        } catch (Exception e) {
            status.put("backend", "DOWN");
            status.put("backend_error", "Cannot reach backend");
        }

        status.put("gateway", "UP");

        boolean degraded = status.values().stream()
                .anyMatch(v -> v.equals("DOWN"));

        status.put("overall", degraded ? "DEGRADED" : "HEALTHY");

        return degraded
                ? ResponseEntity.status(503).body(status)
                : ResponseEntity.ok(status);
    }
}