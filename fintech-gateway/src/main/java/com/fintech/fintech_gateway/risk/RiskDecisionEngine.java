package com.fintech.fintech_gateway.risk;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RiskDecisionEngine {

    @Value("${gateway.risk.block-threshold:71}")
    private int blockThreshold=71;

    @Value("${gateway.risk.throttle-threshold:31}")
    private int throttleThreshold=31;

    public Decision decide(int riskScore) {
        if (riskScore >= blockThreshold) return Decision.BLOCK;
        if (riskScore >= throttleThreshold) return Decision.THROTTLE;
        return Decision.ALLOW;
    }

    public enum Decision {
        ALLOW, THROTTLE, BLOCK
    }
}