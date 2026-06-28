package com.fintech.fintech_gateway.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private String ip;

    @Column(nullable = false)
    private String method;

    @Column(nullable = false)
    private String uri;

    @Column(nullable = false)
    private int riskScore;

    @Column(nullable = false)
    private String decision;

    @Column
    private String attackType;

    @Column(nullable = false)
    private int statusCode;

    // Constructor
    public AuditLogEntity() {}

    public AuditLogEntity(Instant timestamp, String ip, String method,
                          String uri, int riskScore, String decision,
                          String attackType, int statusCode) {
        this.timestamp = timestamp;
        this.ip = ip;
        this.method = method;
        this.uri = uri;
        this.riskScore = riskScore;
        this.decision = decision;
        this.attackType = attackType;
        this.statusCode = statusCode;
    }

    // Getters
    public Long getId() { return id; }
    public Instant getTimestamp() { return timestamp; }
    public String getIp() { return ip; }
    public String getMethod() { return method; }
    public String getUri() { return uri; }
    public int getRiskScore() { return riskScore; }
    public String getDecision() { return decision; }
    public String getAttackType() { return attackType; }
    public int getStatusCode() { return statusCode; }
}