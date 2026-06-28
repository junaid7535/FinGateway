package com.fintech.fintech_gateway.risk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RiskDecisionEngineTest {

    private RiskDecisionEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RiskDecisionEngine();
    }

    @Test
    void score25_shouldAllow() {
        assertEquals(RiskDecisionEngine.Decision.ALLOW,
                engine.decide(25));
    }

    @Test
    void score50_shouldThrottle() {
        assertEquals(RiskDecisionEngine.Decision.THROTTLE,
                engine.decide(50));
    }

    @Test
    void score75_shouldBlock() {
        assertEquals(RiskDecisionEngine.Decision.BLOCK,
                engine.decide(75));
    }

    @Test
    void scoreAtExactBlockThreshold_shouldBlock() {
        assertEquals(RiskDecisionEngine.Decision.BLOCK,
                engine.decide(71));
    }

    @Test
    void scoreAtExactThrottleThreshold_shouldThrottle() {
        assertEquals(RiskDecisionEngine.Decision.THROTTLE,
                engine.decide(31));
    }

    @Test
    void score0_shouldAllow() {
        assertEquals(RiskDecisionEngine.Decision.ALLOW,
                engine.decide(0));
    }

    @Test
    void score100_shouldBlock() {
        assertEquals(RiskDecisionEngine.Decision.BLOCK,
                engine.decide(100));
    }
}