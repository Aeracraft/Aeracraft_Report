package com.aeracraft.report.listener;

import com.aeracraft.report.AeracraftReport;
import com.aeracraft.report.model.Report;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PunishmentListener implements Listener {

    private final AeracraftReport plugin;
    private static final Pattern REPORT_ID_PATTERN = Pattern.compile("举报#([a-f0-9\\-]{36})");

    public PunishmentListener(AeracraftReport plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
    }

    public void onPlayerBan(Player player, String reason, Object banInfo) {
        Matcher matcher = REPORT_ID_PATTERN.matcher(reason);
        if (matcher.find()) {
            String reportIdStr = matcher.group(1);
            try {
                UUID reportId = UUID.fromString(reportIdStr);
                String banId = extractBanId(banInfo);

                if (banId != null) {
                    plugin.getReportService().linkBanToReport(reportId, banId, "SYSTEM", null);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void onPlayerMute(Player player, String reason, Object muteInfo) {
    }

    private String extractBanId(Object banInfo) {
        if (banInfo == null) {
            return null;
        }
        if (banInfo instanceof String) {
            return (String) banInfo;
        }
        return banInfo.toString();
    }

    public void notifyReportStatusChange(UUID reportId, Report.ReportStatus newStatus, String handledBy) {
        String permission = "aeracraft.report.notify";

        Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.hasPermission(permission))
                .forEach(player -> {
                    String message = plugin.getLanguageManager().getMessage(player, "report.status-change", Map.of(
                            "id", reportId.toString(),
                            "status", newStatus.name(),
                            "handler", handledBy != null ? handledBy : "SYSTEM"
                    ));
                    player.sendMessage(message);
                });
    }

    public void notifyReportCompletion(UUID reportId, String targetName, String resolutionNote) {
        plugin.getReportRepository().findById(reportId)
                .thenAccept(optionalReport -> {
                    if (optionalReport.isEmpty()) {
                        return;
                    }

                    Report report = optionalReport.get();
                    Player reporter = Bukkit.getPlayer(report.getReporter());

                    if (reporter != null && reporter.isOnline()) {
                        String message = plugin.getLanguageManager().getMessage(reporter, "report.completed-notify", Map.of(
                                "id", reportId.toString(),
                                "target", targetName,
                                "note", resolutionNote != null ? resolutionNote : ""
                        ));
                        reporter.sendMessage(message);
                    }
                });
    }
}
