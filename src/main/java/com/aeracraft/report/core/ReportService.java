package com.aeracraft.report.core;

import com.aeracraft.report.AeracraftReport;
import com.aeracraft.report.model.AuditLog;
import com.aeracraft.report.model.Evidence;
import com.aeracraft.report.model.Report;
import com.aeracraft.report.model.Report.ReportStatus;
import com.aeracraft.report.util.HashUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ReportService {

    private final AeracraftReport plugin;

    public ReportService(AeracraftReport plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Report> createReport(Player reporter, Player target, String type, String description) {
        return CompletableFuture.supplyAsync(() -> {
            UUID reportId = UUID.randomUUID();
            Instant now = Instant.now();

            Evidence evidence = plugin.getEvidenceCollector().collectEvidence(reporter, target, type, description);

            String evidenceJson = plugin.getGson().toJson(evidence);
            String evidenceHash = HashUtil.sha256(evidenceJson);

            Report report = new Report(
                    reportId,
                    reporter.getName(),
                    target.getName(),
                    type,
                    ReportStatus.PENDING,
                    now,
                    now,
                    evidenceHash,
                    null,
                    plugin.getConfigManager().getServerName(),
                    null,
                    null
            );

            return plugin.getReportRepository().createReport(report, evidence).join();
        }).thenCompose(savedReport -> {
            notifyAdminsNewReport(savedReport);
            broadcastNewReport(savedReport);

            plugin.getPointsManager().incrementReportCount(savedReport.getReporter());

            return CompletableFuture.completedFuture(savedReport);
        });
    }

    public CompletableFuture<Report> createReport(String reporterName, String targetName, String type, String description, String serverName) {
        return CompletableFuture.supplyAsync(() -> {
            UUID reportId = UUID.randomUUID();
            Instant now = Instant.now();

            Evidence evidence = plugin.getEvidenceCollector().collectOfflineEvidence(reporterName, targetName, type, description);

            String evidenceJson = plugin.getGson().toJson(evidence);
            String evidenceHash = HashUtil.sha256(evidenceJson);

            Report report = new Report(
                    reportId,
                    reporterName,
                    targetName,
                    type,
                    ReportStatus.PENDING,
                    now,
                    now,
                    evidenceHash,
                    null,
                    serverName,
                    null,
                    null
            );

            Evidence offlineEvidence = new Evidence();
            offlineEvidence.setReportId(reportId);
            offlineEvidence.setCollectedAt(now);

            return plugin.getReportRepository().createReport(report, offlineEvidence).join();
        }).thenCompose(savedReport -> {
            broadcastNewReport(savedReport);
            return CompletableFuture.completedFuture(savedReport);
        });
    }

    public CompletableFuture<Void> updateReportStatus(UUID reportId, ReportStatus newStatus, String operator, String note, String operatorIp) {
        return plugin.getReportRepository().findById(reportId)
                .thenCompose(optionalReport -> {
                    if (optionalReport.isEmpty()) {
                        return CompletableFuture.failedFuture(new IllegalArgumentException("Report not found: " + reportId));
                    }

                    Report report = optionalReport.get();
                    ReportStatus oldStatus = report.getStatus();

                    String beforeSnapshot = serializeReportSnapshot(report);

                    return plugin.getReportRepository().updateStatus(reportId, newStatus, operator, note)
                            .thenCompose(v -> {
                                if (newStatus == ReportStatus.COMPLETED) {
                                    plugin.getPointsManager().addPointsForValidReport(report.getReporter(), report.getType());
                                    plugin.getPointsManager().notifyThresholdReached(report.getTarget());
                                }

                                String afterSnapshot = serializeReportSnapshot(report);
                                createAuditLog(reportId, "STATUS_CHANGE", operator, operatorIp, beforeSnapshot, afterSnapshot);

                                broadcastStatusChange(reportId, newStatus);

                                return CompletableFuture.completedFuture(null);
                            });
                });
    }

    public CompletableFuture<Void> linkBanToReport(UUID reportId, String banId, String operator, String operatorIp) {
        return plugin.getReportRepository().linkBan(reportId, banId)
                .thenCompose(v -> {
                    plugin.getReportRepository().findById(reportId)
                            .thenAccept(optionalReport -> optionalReport.ifPresent(report -> {
                                String beforeSnapshot = serializeReportSnapshot(report);
                                String afterSnapshot = serializeReportSnapshot(report);
                                createAuditLog(reportId, "BAN_LINKED", operator, operatorIp, beforeSnapshot, afterSnapshot);
                            }));

                    return CompletableFuture.completedFuture(null);
                });
    }

    public CompletableFuture<List<Report>> getReportsByStatus(ReportStatus status, int page, int pageSize) {
        int offset = page * pageSize;
        return plugin.getReportRepository().findByStatus(status, pageSize, offset);
    }

    public CompletableFuture<List<Report>> getReportsByTarget(String target, int page, int pageSize) {
        int offset = page * pageSize;
        return plugin.getReportRepository().findByTarget(target, pageSize, offset);
    }

    public CompletableFuture<List<Report>> getReportsByReporter(String reporter, int page, int pageSize) {
        int offset = page * pageSize;
        return plugin.getReportRepository().findByReporter(reporter, pageSize, offset);
    }

    public CompletableFuture<List<Report>> getAllReports(int page, int pageSize) {
        int offset = page * pageSize;
        return plugin.getReportRepository().findAll(pageSize, offset);
    }

    public CompletableFuture<java.util.Optional<Report>> getReportById(UUID reportId) {
        return plugin.getReportRepository().findById(reportId);
    }

    public CompletableFuture<java.util.Optional<Evidence>> getEvidence(UUID reportId) {
        return plugin.getReportRepository().findEvidenceByReportId(reportId);
    }

    public CompletableFuture<Boolean> canCreateReport(Player player) {
        String permission = "aeracraft.report.bypass.limit";
        if (player.hasPermission(permission)) {
            return CompletableFuture.completedFuture(true);
        }

        int dailyLimit = plugin.getConfigManager().getDailyReportLimit();
        if (dailyLimit <= 0) {
            return CompletableFuture.completedFuture(true);
        }

        return plugin.getReportRepository().findByReporter(player.getName(), 0, Integer.MAX_VALUE)
                .thenApply(reports -> {
                    long todayCount = reports.stream()
                            .filter(r -> r.getCreatedAt().toEpochMilli() >= Instant.now().minusSeconds(86400).toEpochMilli())
                            .count();
                    return todayCount < dailyLimit;
                });
    }

    public CompletableFuture<Boolean> isOnCooldown(Player player, String targetName) {
        String permission = "aeracraft.report.bypass.cooldown";
        if (player.hasPermission(permission)) {
            return CompletableFuture.completedFuture(false);
        }

        int cooldownMinutes = plugin.getConfigManager().getReportCooldownMinutes();
        if (cooldownMinutes <= 0) {
            return CompletableFuture.completedFuture(false);
        }

        return plugin.getReportRepository().existsDuplicateReport(
                player.getName(),
                targetName,
                cooldownMinutes
        );
    }

    private void notifyAdminsNewReport(Report report) {
        String permission = "aeracraft.report.notify";
        Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.hasPermission(permission))
                .forEach(player -> {
                    String message = plugin.getLanguageManager().getMessage(player, "report.new-notify", Map.of(
                            "id", report.getId().toString(),
                            "reporter", report.getReporter(),
                            "target", report.getTarget(),
                            "type", report.getType()
                    ));
                    player.sendMessage(message);
                });
    }

    private void broadcastNewReport(Report report) {
        if (plugin.getConfigManager().isCrossServerEnabled()) {
            // Redis broadcast would go here
        }
    }

    private void broadcastStatusChange(UUID reportId, ReportStatus newStatus) {
        if (plugin.getConfigManager().isCrossServerEnabled()) {
            // Redis broadcast would go here
        }
    }

    private void createAuditLog(UUID reportId, String action, String operator, String operatorIp,
                                String beforeSnapshot, String afterSnapshot) {
        AuditLog auditLog = new AuditLog(
                UUID.randomUUID(),
                reportId,
                action,
                operator,
                operatorIp,
                beforeSnapshot,
                afterSnapshot,
                Instant.now()
        );

        plugin.getAuditRepository().createAuditLog(auditLog);
    }

    private String serializeReportSnapshot(Report report) {
        return plugin.getGson().toJson(report);
    }
}
