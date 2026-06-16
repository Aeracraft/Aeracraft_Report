package com.aeracraft.report.model;

import java.time.Instant;
import java.util.UUID;

public class AuditLog {

    private UUID id;
    private UUID reportId;
    private String action;
    private String operator;
    private String operatorIp;
    private String beforeSnapshot;
    private String afterSnapshot;
    private Instant createdAt;

    public AuditLog() {
    }

    public AuditLog(UUID id, UUID reportId, String action, String operator, String operatorIp,
                    String beforeSnapshot, String afterSnapshot, Instant createdAt) {
        this.id = id;
        this.reportId = reportId;
        this.action = action;
        this.operator = operator;
        this.operatorIp = operatorIp;
        this.beforeSnapshot = beforeSnapshot;
        this.afterSnapshot = afterSnapshot;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getReportId() {
        return reportId;
    }

    public void setReportId(UUID reportId) {
        this.reportId = reportId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getOperatorIp() {
        return operatorIp;
    }

    public void setOperatorIp(String operatorIp) {
        this.operatorIp = operatorIp;
    }

    public String getBeforeSnapshot() {
        return beforeSnapshot;
    }

    public void setBeforeSnapshot(String beforeSnapshot) {
        this.beforeSnapshot = beforeSnapshot;
    }

    public String getAfterSnapshot() {
        return afterSnapshot;
    }

    public void setAfterSnapshot(String afterSnapshot) {
        this.afterSnapshot = afterSnapshot;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
