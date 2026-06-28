package com.fintech.fintech_gateway.persistence.repository;

import com.fintech.fintech_gateway.persistence.entity.TrustHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TrustHistoryRepository
        extends JpaRepository<TrustHistoryEntity, Long> {

    List<TrustHistoryEntity> findByIp(String ip);
}