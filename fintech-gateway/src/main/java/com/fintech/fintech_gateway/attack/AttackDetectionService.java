package com.fintech.fintech_gateway.attack;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class AttackDetectionService {

    // SQL Injection patterns
    private static final List<Pattern> SQL_INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i).*(--|;|'|\\bOR\\b|\\bAND\\b|\\bDROP\\b|\\bSELECT\\b|\\bINSERT\\b|\\bDELETE\\b|\\bUPDATE\\b|\\bUNION\\b).*"),
            Pattern.compile("(?i).*('\\s*(or|and)\\s*'?\\d).*"),
            Pattern.compile("(?i).*(\\bEXEC\\b|\\bEXECUTE\\b|\\bxp_|\\bsp_).*")
    );

    // XSS patterns
    private static final List<Pattern> XSS_PATTERNS = List.of(
            Pattern.compile("(?i).*(<script|</script|javascript:|onerror=|onload=|<iframe|<img).*"),
            Pattern.compile("(?i).*(alert\\(|confirm\\(|prompt\\().*")
    );

    // Path traversal patterns
    private static final List<Pattern> PATH_TRAVERSAL_PATTERNS = List.of(
            Pattern.compile(".*\\.\\..*"),
            Pattern.compile("(?i).*(etc/passwd|win/system32|windows/system32).*")
    );

    public AttackDetectionResult inspect(String payload, String uri) {

        // Path traversal checks URI — always run regardless of payload
        for (Pattern pattern : PATH_TRAVERSAL_PATTERNS) {
            if (pattern.matcher(uri).matches()) {
                return AttackDetectionResult.threat("PATH_TRAVERSAL", uri);
            }
        }

        // Payload checks only run if there is a payload
        if (payload == null || payload.isEmpty()) {
            return AttackDetectionResult.clean();
        }

        // XSS first
        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(payload).matches()) {
                return AttackDetectionResult.threat("XSS", payload);
            }
        }

        // Then SQL injection
        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            if (pattern.matcher(payload).matches()) {
                return AttackDetectionResult.threat("SQL_INJECTION", payload);
            }
        }

        return AttackDetectionResult.clean();
    }

    // Result object
    public static class AttackDetectionResult {
        private final boolean threatDetected;
        private final String threatType;
        private final String evidence;

        private AttackDetectionResult(boolean threatDetected, String threatType, String evidence) {
            this.threatDetected = threatDetected;
            this.threatType = threatType;
            this.evidence = evidence;
        }

        public static AttackDetectionResult clean() {
            return new AttackDetectionResult(false, null, null);
        }

        public static AttackDetectionResult threat(String type, String evidence) {
            return new AttackDetectionResult(true, type, evidence);
        }

        public boolean isThreatDetected() { return threatDetected; }
        public String getThreatType() { return threatType; }
        public String getEvidence() { return evidence; }
    }
}