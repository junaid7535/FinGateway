package com.fintech.fintech_gateway.attack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AttackDetectionServiceTest {

    private AttackDetectionService service;

    @BeforeEach
    void setUp() {
        service = new AttackDetectionService();
    }

    @Test
    void cleanPayload_shouldNotDetectThreat() {
        var result = service.inspect(
                "{\"amount\":5000,\"currency\":\"INR\"}", "/api/payments");
        assertFalse(result.isThreatDetected());
    }

    @Test
    void sqlInjectionPayload_shouldDetectThreat() {
        var result = service.inspect(
                "{\"accountId\":\"ACC123' OR '1'='1\"}", "/api/payments");
        assertTrue(result.isThreatDetected());
        assertEquals("SQL_INJECTION", result.getThreatType());
    }

    @Test
    void xssPayload_shouldDetectThreat() {
        var result = service.inspect(
                "{\"name\":\"<script>alert('xss')</script>\"}", "/api/account");
        assertTrue(result.isThreatDetected());
        assertEquals("XSS", result.getThreatType());
    }

    @Test
    void pathTraversal_shouldDetectThreat() {
        var result = service.inspect("", "/api/../../../etc/passwd");
        assertTrue(result.isThreatDetected());
        assertEquals("PATH_TRAVERSAL", result.getThreatType());
    }

    @Test
    void emptyPayload_shouldNotDetectThreat() {
        var result = service.inspect("", "/api/account/balance");
        assertFalse(result.isThreatDetected());
    }
}