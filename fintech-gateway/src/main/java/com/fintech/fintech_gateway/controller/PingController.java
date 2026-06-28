package com.fintech.fintech_gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/ping")
public class PingController {

    private static final long START_TIME = System.currentTimeMillis();

    @GetMapping
    public ResponseEntity<Map<String, Object>> ping() {
        long uptimeSeconds = (System.currentTimeMillis() - START_TIME) / 1000;
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "timestamp", Instant.now().toString(),
                "uptime_seconds", uptimeSeconds,
                "service", "fintech-gateway"
        ));
    }
}