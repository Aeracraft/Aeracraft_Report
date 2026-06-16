package com.aeracraft.report.rest.handler;

import com.aeracraft.report.AeracraftReport;
import com.aeracraft.report.model.Report.ReportStatus;
import com.aeracraft.report.rest.dto.ApiResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import spark.Request;
import spark.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PunishmentHandler {

    private final AeracraftReport plugin;
    private final Gson gson;

    public PunishmentHandler(AeracraftReport plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
    }

    public void registerRoutes(Service server, String basePath) {
        server.post(basePath + "/punishments", this::executePunishment);
        server.delete(basePath + "/punishments/:banId", this::revokePunishment);
    }

    private String executePunishment(Request request, spark.Response response) {
        try {
            JsonObject json = gson.fromJson(request.body(), JsonObject.class);

            String type = json.has("type") ? json.get("type").getAsString() : null;
            String target = json.has("target") ? json.get("target").getAsString() : null;
            String reason = json.has("reason") ? json.get("reason").getAsString() : "API处罚";
            long duration = json.has("duration") ? json.get("duration").getAsLong() : 0;
            String reportId = json.has("reportId") ? json.get("reportId").getAsString() : null;

            if (type == null || target == null) {
                response.status(400);
                return gson.toJson(ApiResponse.error("Missing required fields: type, target"));
            }

            org.bukkit.command.CommandSender operator = Bukkit.getConsoleSender();

            CompletableFuture<String> future;

            switch (type.toLowerCase()) {
                case "ban" -> future = plugin.getPunishmentProvider().ban(operator, target, reason, 0, reportId);
                case "tempban" -> future = plugin.getPunishmentProvider().tempBan(operator, target, reason, duration, reportId);
                case "mute" -> future = plugin.getPunishmentProvider().mute(operator, target, reason, duration);
                case "kick" -> {
                    plugin.getPunishmentProvider().kick(operator, target, reason);
                    return gson.toJson(ApiResponse.success(Map.of("success", true), "Kick executed"));
                }
                case "warn" -> {
                    plugin.getPunishmentProvider().warn(operator, target, reason);
                    return gson.toJson(ApiResponse.success(Map.of("success", true), "Warning sent"));
                }
                default -> {
                    response.status(400);
                    return gson.toJson(ApiResponse.error("Invalid punishment type"));
                }
            }

            String punishmentId = future.join();

            if (reportId != null && punishmentId != null) {
                try {
                    java.util.UUID uuid = java.util.UUID.fromString(reportId);
                    plugin.getReportService().updateReportStatus(
                            uuid,
                            ReportStatus.IN_PROGRESS,
                            "API",
                            "处罚已执行",
                            null
                    );
                } catch (IllegalArgumentException ignored) {
                }
            }

            return gson.toJson(ApiResponse.success(Map.of("punishmentId", punishmentId != null ? punishmentId : ""), "Punishment executed"));

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to execute punishment via API: " + e.getMessage());
            response.status(500);
            return gson.toJson(ApiResponse.error("Failed to execute punishment"));
        }
    }

    private String revokePunishment(Request request, spark.Response response) {
        String banId = request.params(":banId");

        try {
            plugin.getPunishmentProvider().unban(banId, "API撤销");

            return gson.toJson(ApiResponse.success(null, "Punishment revoked"));

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to revoke punishment via API: " + e.getMessage());
            response.status(500);
            return gson.toJson(ApiResponse.error("Failed to revoke punishment"));
        }
    }
}
