package com.fintech.fintech_gateway.audit;

import com.fintech.fintech_gateway.persistence.PersistenceService;
import com.fintech.fintech_gateway.risk.RiskDecisionEngine.Decision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
public class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger("AUDIT");
    private final PersistenceService persistenceService;

    public AuditLogger(PersistenceService persistenceService){
        this.persistenceService = persistenceService;
    }

    public void log(AuditEvent event) {
        log.info("AUDIT | timestamp={} | ip={} | method={} | uri={} | riskScore={} | decision={} | attackType={} | statusCode={}",
                event.getTimestamp(),
                event.getIp(),
                event.getMethod(),
                event.getUri(),
                event.getRiskScore(),
                event.getDecision(),
                event.getAttackType() != null ? event.getAttackType() : "NONE",
                event.getStatusCode()
        );

        // Database save
        persistenceService.saveAuditLog(
                event.getTimestamp(),
                event.getIp(),
                event.getMethod(),
                event.getUri(),
                event.getRiskScore(),
                event.getDecision().name(),
                event.getAttackType(),
                event.getStatusCode()
        );
    }

    // Event object
    public static class AuditEvent {
        private final Instant timestamp;
        private final String ip;
        private final String method;
        private final String uri;
        private final int riskScore;
        private final Decision decision;
        private final String attackType;
        private final int statusCode;

        private AuditEvent(Builder builder) {
            this.timestamp = Instant.now();
            this.ip = builder.ip;
            this.method = builder.method;
            this.uri = builder.uri;
            this.riskScore = builder.riskScore;
            this.decision = builder.decision;
            this.attackType = builder.attackType;
            this.statusCode = builder.statusCode;
        }

        // Builder pattern
        public static class Builder {
            private String ip;
            private String method;
            private String uri;
            private int riskScore;
            private Decision decision;
            private String attackType;
            private int statusCode;

            public Builder ip(String ip) { this.ip = ip; return this; }
            public Builder method(String method) { this.method = method; return this; }
            public Builder uri(String uri) { this.uri = uri; return this; }
            public Builder riskScore(int riskScore) { this.riskScore = riskScore; return this; }
            public Builder decision(Decision decision) { this.decision = decision; return this; }
            public Builder attackType(String attackType) { this.attackType = attackType; return this; }
            public Builder statusCode(int statusCode) { this.statusCode = statusCode; return this; }
            public AuditEvent build() { return new AuditEvent(this); }
        }

        public Instant getTimestamp() { return timestamp; }
        public String getIp() { return ip; }
        public String getMethod() { return method; }
        public String getUri() { return uri; }
        public int getRiskScore() { return riskScore; }
        public Decision getDecision() { return decision; }
        public String getAttackType() { return attackType; }
        public int getStatusCode() { return statusCode; }
    }
}