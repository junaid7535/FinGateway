package com.fintech.fintech_gateway.filter;

import com.fintech.fintech_gateway.attack.AttackDetectionService;
import com.fintech.fintech_gateway.audit.AuditLogger;
import com.fintech.fintech_gateway.idempotency.IdempotencyService;
import com.fintech.fintech_gateway.risk.RiskDecisionEngine;
import com.fintech.fintech_gateway.risk.RiskDecisionEngine.Decision;
import com.fintech.fintech_gateway.risk.RiskScoringEngine;
import com.fintech.fintech_gateway.trust.TrustTierService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final RiskScoringEngine riskScoringEngine;
    private final RiskDecisionEngine riskDecisionEngine;
    private final IdempotencyService idempotencyService;
    private final AttackDetectionService attackDetectionService;
    private final AuditLogger auditLogger;
    private final TrustTierService trustTierService;

    public RequestLoggingFilter(RiskScoringEngine riskScoringEngine,
                                RiskDecisionEngine riskDecisionEngine,
                                IdempotencyService idempotencyService,
                                AttackDetectionService attackDetectionService,
                                AuditLogger auditLogger,
                                TrustTierService trustTierService) {
        this.riskScoringEngine = riskScoringEngine;
        this.riskDecisionEngine = riskDecisionEngine;
        this.idempotencyService = idempotencyService;
        this.attackDetectionService = attackDetectionService;
        this.auditLogger = auditLogger;
        this.trustTierService = trustTierService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String ip = request.getRemoteAddr();
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Skip internal requests completely
        if (uri.contains("/favicon.ico") || uri.contains("/swagger-ui")
                || uri.contains("/v3/api-docs") || uri.startsWith("/feedback")
                || uri.startsWith("/health") || uri.startsWith("/ping")
                || uri.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Blacklist check
            if (trustTierService.isBlacklisted(ip)) {
                response.setStatus(403);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"Access permanently denied\"," +
                                "\"ip\":\"" + ip + "\"}"
                );
                auditLogger.log(new AuditLogger.AuditEvent.Builder()
                        .ip(ip).method(method).uri(uri)
                        .riskScore(100).decision(Decision.BLOCK)
                        .attackType("BLACKLISTED").statusCode(403).build());
                return;
            }

            // Risk scoring
            int score = riskScoringEngine.calculateScore(request);
            Decision decision = riskDecisionEngine.decide(score);

            System.out.println("=== GATEWAY INTERCEPTED ===");
            System.out.println("Method   : " + method);
            System.out.println("URI      : " + uri);
            System.out.println("IP       : " + ip);
            System.out.println("Risk Score: " + score);
            System.out.println("Decision : " + decision);
            System.out.println("===========================");

            // Block
            if (decision == Decision.BLOCK) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"Request blocked\"," +
                                "\"reason\":\"Risk score too high\"," +
                                "\"score\":" + score + "}"
                );
                auditLogger.log(new AuditLogger.AuditEvent.Builder()
                        .ip(ip).method(method).uri(uri)
                        .riskScore(score).decision(decision)
                        .attackType(null).statusCode(429).build());
                trustTierService.recordBlock(ip);
                return;
            }

            // Throttle
            if (decision == Decision.THROTTLE) {
                int throttleCount = trustTierService.recordThrottle(ip);
                long delayMs = switch (throttleCount) {
                    case 1, 2 -> 2000L;
                    case 3, 4 -> 5000L;
                    case 5, 6 -> 10000L;
                    default -> -1L;
                };
                if (delayMs == -1) {
                    response.setStatus(429);
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"error\":\"Request blocked\"," +
                                    "\"reason\":\"Repeated throttle violations\"," +
                                    "\"strikes\":" + throttleCount + "}"
                    );
                    trustTierService.recordBlock(ip);
                    auditLogger.log(new AuditLogger.AuditEvent.Builder()
                            .ip(ip).method(method).uri(uri)
                            .riskScore(score).decision(Decision.BLOCK)
                            .attackType("THROTTLE_ABUSE").statusCode(429).build());
                    return;
                }
                try { Thread.sleep(delayMs); }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Read body for attack detection
            String body = "";
            if ("POST".equalsIgnoreCase(method) ||
                    "PUT".equalsIgnoreCase(method)) {
                body = request.getReader().lines()
                        .collect(java.util.stream.Collectors.joining());
                request = new CachedBodyRequestWrapper(request, body);
            }

            // Attack detection
            AttackDetectionService.AttackDetectionResult attackResult =
                    attackDetectionService.inspect(body, uri);

            if (attackResult.isThreatDetected()) {
                response.setStatus(400);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"Malicious request detected\"," +
                                "\"threatType\":\"" + attackResult.getThreatType() + "\"}"
                );
                auditLogger.log(new AuditLogger.AuditEvent.Builder()
                        .ip(ip).method(method).uri(uri)
                        .riskScore(score).decision(Decision.BLOCK)
                        .attackType(attackResult.getThreatType())
                        .statusCode(400).build());
                System.out.println("ATTACK DETECTED: "
                        + attackResult.getThreatType());
                return;
            }

            // Idempotency check
            String idempotencyKey = request.getHeader("Idempotency-Key");
            if ("POST".equalsIgnoreCase(method) && idempotencyKey != null) {
                if (idempotencyService.isProcessed(idempotencyKey)) {
                    response.setStatus(200);
                    response.setContentType("application/json");
                    response.getWriter().write(
                            idempotencyService.getStoredResponse(idempotencyKey));
                    System.out.println("IDEMPOTENT: Returning cached response for key: "
                            + idempotencyKey);
                    auditLogger.log(new AuditLogger.AuditEvent.Builder()
                            .ip(ip).method(method).uri(uri)
                            .riskScore(score).decision(decision)
                            .attackType("DUPLICATE").statusCode(200).build());
                    return;
                }
                if (idempotencyService.isInProgress(idempotencyKey)) {
                    response.setStatus(409);
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"error\":\"Request in progress\"," +
                                    "\"key\":\"" + idempotencyKey + "\"}"
                    );
                    return;
                }
                idempotencyService.markInProgress(idempotencyKey);
            }

            // Allow — forward to backend
            if ("POST".equalsIgnoreCase(method) && idempotencyKey != null) {
                CachedResponseWrapper wrappedResponse = new CachedResponseWrapper(response);
                response.setContentType("application/json");  // ADD THIS LINE
                filterChain.doFilter(request, wrappedResponse);
                String capturedBody = wrappedResponse.getCapturedBody();
                idempotencyService.markCompleted(idempotencyKey, capturedBody);
                wrappedResponse.copyBodyToResponse();
            } else {
                filterChain.doFilter(request, response);
            }

            // Record good behaviour and audit
            trustTierService.recordGoodBehaviour(ip);
            trustTierService.resetThrottleCount(ip);
            auditLogger.log(new AuditLogger.AuditEvent.Builder()
                    .ip(ip).method(method).uri(uri)
                    .riskScore(score).decision(decision)
                    .attackType(null).statusCode(200).build());

        } catch (Exception e) {
            System.err.println("Gateway filter error — failing open: "
                    + e.getMessage());
            // Never return 500 — forward request even if gateway logic fails
            if (!response.isCommitted()) {
                filterChain.doFilter(request, response);
            }
        }
    }
}