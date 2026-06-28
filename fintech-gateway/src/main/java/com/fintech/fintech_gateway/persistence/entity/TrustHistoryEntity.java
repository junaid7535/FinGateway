package com.fintech.fintech_gateway.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "trust_history")
public class TrustHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ip;

    @Column(nullable = false)
    private String previousTier;

    @Column(nullable = false)
    private String newTier;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private Instant timestamp;

    public TrustHistoryEntity() {}

    public TrustHistoryEntity(String ip, String previousTier,
                              String newTier, String reason) {
        this.ip = ip;
        this.previousTier = previousTier;
        this.newTier = newTier;
        this.reason = reason;
        this.timestamp = Instant.now();
    }

    // Getters
    public Long getId() { return id; }
    public String getIp() { return ip; }
    public String getPreviousTier() { return previousTier; }
    public String getNewTier() { return newTier; }
    public String getReason() { return reason; }
    public Instant getTimestamp() { return timestamp; }
}