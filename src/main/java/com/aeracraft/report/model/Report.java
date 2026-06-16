package com.aeracraft.report.model;

import java.time.Instant;
import java.util.UUID;

public class Report {

    private final UUID id;
    private final String reporter;
    private final String target;
    private final String type;
    private ReportStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private String evidenceHash;
    private String linkedBanId;
    private String serverName;
    private String handledBy;
    private String resolutionNote;

    public Report(UUID id, String reporter, String target, String type, ReportStatus status,
                  Instant createdAt, Instant updatedAt, String evidenceHash, String linkedBanId,
                  String serverName, String handledBy, String resolutionNote) {
        this.id = id;
        this.reporter = reporter;
        this.target = target;
        this.type = type;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.evidenceHash = evidenceHash;
        this.linkedBanId = linkedBanId;
        this.serverName = serverName;
        this.handledBy = handledBy;
        this.resolutionNote = resolutionNote;
    }

    public UUID getId() {
        return id;
    }

    public String getReporter() {
        return reporter;
    }

    public String getTarget() {
        return target;
    }

    public String getType() {
        return type;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getEvidenceHash() {
        return evidenceHash;
    }

    public void setEvidenceHash(String evidenceHash) {
        this.evidenceHash = evidenceHash;
    }

    public String getLinkedBanId() {
        return linkedBanId;
    }

    public void setLinkedBanId(String linkedBanId) {
        this.linkedBanId = linkedBanId;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getHandledBy() {
        return handledBy;
    }

    public void setHandledBy(String handledBy) {
        this.handledBy = handledBy;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public enum ReportStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        REJECTED
    }
}
