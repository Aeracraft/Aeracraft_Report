package com.aeracraft.report.listener;

import com.aeracraft.report.AeracraftReport;
import com.aeracraft.report.model.Report;
import com.aeracraft.report.model.Report.ReportStatus;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class RedisMessageListener implements Listener {

    private final AeracraftReport plugin;
    private final ExecutorService executor;
    private final Gson gson;

    public static final String CHANNEL_NEW_REPORT = "aeracraftreport:report:new";
    public static final String CHANNEL_STATUS_CHANGE = "aeracraftreport:report:status";
    public static final String CHANNEL_PUNISHMENT_EXECUTED = "aeracraftreport:punishment:executed";

    public RedisMessageListener(AeracraftReport plugin) {
        this.plugin = plugin;
        this.executor = Executors.newCachedThreadPool();
        this.gson = new Gson();
    }

    public void subscribeToChannels() {
        if (!plugin.getConfigManager().isCrossServerEnabled()) {
            return;
        }

        plugin.getLogger().info("开始订阅 Redis 频道...");
    }

    public void unsubscribeFromChannels() {
        plugin.getLogger().info("取消订阅 Redis 频道...");
    }

    public void handleNewReportMessage(String message) {
        executor.execute(() -> {
            try {
                JsonObject json = gson.fromJson(message, JsonObject.class);
                String reportId = json.get("reportId").getAsString();
                String reporter = json.get("reporter").getAsString();
                String target = json.get("target").getAsString();
                String type = json.get("type").getAsString();
                String serverName = json.get("serverName").getAsString();

                notifyAdminsNewReport(reportId, reporter, target, type, serverName);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "处理新举报消息失败", e);
            }
        });
    }

    public void handleStatusChangeMessage(String message) {
        executor.execute(() -> {
            try {
                JsonObject json = gson.fromJson(message, JsonObject.class);
                String reportId = json.get("reportId").getAsString();
                String newStatus = json.get("status").getAsString();
                String handledBy = json.get("handledBy").getAsString();

                notifyAdminsStatusChange(reportId, newStatus, handledBy);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "处理状态变更消息失败", e);
            }
        });
    }

    public void handlePunishmentExecutedMessage(String message) {
        executor.execute(() -> {
            try {
                JsonObject json = gson.fromJson(message, JsonObject.class);
                String reportId = json.get("reportId").getAsString();
                String banId = json.get("banId").getAsString();
                String target = json.get("target").getAsString();

                notifyReportCompletion(reportId, target);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "处理处罚执行消息失败", e);
            }
        });
    }

    public void broadcastNewReport(Report report) {
        if (!plugin.getConfigManager().isCrossServerEnabled()) {
            return;
        }

        JsonObject json = new JsonObject();
        json.addProperty("reportId", report.getId().toString());
        json.addProperty("reporter", report.getReporter());
        json.addProperty("target", report.getTarget());
        json.addProperty("type", report.getType());
        json.addProperty("serverName", report.getServerName());
        json.addProperty("timestamp", System.currentTimeMillis());

        publish(CHANNEL_NEW_REPORT, gson.toJson(json));
    }

    public void broadcastStatusChange(UUID reportId, ReportStatus newStatus, String handledBy) {
        if (!plugin.getConfigManager().isCrossServerEnabled()) {
            return;
        }

        JsonObject json = new JsonObject();
        json.addProperty("reportId", reportId.toString());
        json.addProperty("status", newStatus.name());
        json.addProperty("handledBy", handledBy);
        json.addProperty("timestamp", System.currentTimeMillis());

        publish(CHANNEL_STATUS_CHANGE, gson.toJson(json));
    }

    public void broadcastPunishmentExecuted(UUID reportId, String banId, String targetName) {
        if (!plugin.getConfigManager().isCrossServerEnabled()) {
            return;
        }

        JsonObject json = new JsonObject();
        json.addProperty("reportId", reportId.toString());
        json.addProperty("banId", banId);
        json.addProperty("target", targetName);
        json.addProperty("timestamp", System.currentTimeMillis());

        publish(CHANNEL_PUNISHMENT_EXECUTED, gson.toJson(json));
    }

    private void publish(String channel, String message) {
        plugin.getLogger().fine("发布消息到频道 " + channel + ": " + message);
    }

    private void notifyAdminsNewReport(String reportId, String reporter, String target, String type, String serverName) {
        String permission = "aeracraft.report.notify";

        Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.hasPermission(permission))
                .forEach(player -> {
                    String message = plugin.getLanguageManager().getMessage(player, "report.new-notify-cross", Map.of(
                            "id", reportId,
                            "reporter", reporter,
                            "target", target,
                            "type", type,
                            "server", serverName
                    ));
                    player.sendMessage(message);
                });
    }

    private void notifyAdminsStatusChange(String reportId, String newStatus, String handledBy) {
        String permission = "aeracraft.report.notify";

        Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.hasPermission(permission))
                .forEach(player -> {
                    String message = plugin.getLanguageManager().getMessage(player, "report.status-change-cross", Map.of(
                            "id", reportId,
                            "status", newStatus,
                            "handler", handledBy
                    ));
                    player.sendMessage(message);
                });
    }

    private void notifyReportCompletion(String reportId, String targetName) {
        plugin.getReportRepository().findById(UUID.fromString(reportId))
                .thenAccept(optionalReport -> {
                    if (optionalReport.isEmpty()) {
                        return;
                    }

                    Report report = optionalReport.get();
                    Player reporter = Bukkit.getPlayer(report.getReporter());

                    if (reporter != null && reporter.isOnline()) {
                        String message = plugin.getLanguageManager().getMessage(reporter, "report.completed-notify", Map.of(
                                "id", reportId,
                                "target", targetName
                        ));
                        reporter.sendMessage(message);
                    }
                });
    }
}
