package com.fintech.fintech_gateway.persistence.repository;

import com.fintech.fintech_gateway.persistence.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository
        extends JpaRepository<AuditLogEntity, Long> {

    List<AuditLogEntity> findByIp(String ip);
    List<AuditLogEntity> findByDecision(String decision);
    List<AuditLogEntity> findByAttackType(String attackType);
}