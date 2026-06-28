package com.fintech.fintech_gateway.persistence;

import com.fintech.fintech_gateway.persistence.entity.AuditLogEntity;
import com.fintech.fintech_gateway.persistence.entity.TrustHistoryEntity;
import com.fintech.fintech_gateway.persistence.repository.AuditLogRepository;
import com.fintech.fintech_gateway.persistence.repository.TrustHistoryRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class PersistenceService {

    private final AuditLogRepository auditLogRepository;
    private final TrustHistoryRepository trustHistoryRepository;

    public PersistenceService(AuditLogRepository auditLogRepository,
                              TrustHistoryRepository trustHistoryRepository) {
        this.auditLogRepository = auditLogRepository;
        this.trustHistoryRepository = trustHistoryRepository;
    }

    @Async
    public void saveAuditLog(Instant timestamp, String ip, String method,
                             String uri, int riskScore, String decision,
                             String attackType, int statusCode) {
        try {
            AuditLogEntity entity = new AuditLogEntity(
                    timestamp, ip, method, uri,
                    riskScore, decision, attackType, statusCode
            );
            auditLogRepository.save(entity);
        } catch (Exception e) {
            System.err.println("Failed to save audit log: " + e.getMessage());
        }
    }

    @Async
    public void saveTrustHistory(String ip, String previousTier,
                                 String newTier, String reason) {
        try {
            TrustHistoryEntity entity = new TrustHistoryEntity(
                    ip, previousTier, newTier, reason
            );
            trustHistoryRepository.save(entity);
        } catch (Exception e) {
            System.err.println("Failed to save trust history: " + e.getMessage());
        }
    }
}