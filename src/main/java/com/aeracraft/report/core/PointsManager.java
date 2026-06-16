package com.aeracraft.report.core;

import com.aeracraft.report.AeracraftReport;
import com.aeracraft.report.model.PlayerPoints;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PointsManager {

    private final AeracraftReport plugin;

    public PointsManager(AeracraftReport plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Void> addPointsForValidReport(String reporterName, String reportType) {
        return CompletableFuture.runAsync(() -> {
            int pointsToAdd = getPointsForReportType(reportType);
            if (pointsToAdd <= 0) {
                return;
            }

            UUID playerUuid = getPlayerUuid(reporterName);
            if (playerUuid == null) {
                return;
            }

            PlayerPoints playerPoints = plugin.getPlayerPreferencesRepository()
                    .getOrCreatePlayerPoints(playerUuid, reporterName)
                    .join();

            playerPoints.addPoints(pointsToAdd);
            playerPoints.setUpdatedAt(Instant.now());

            plugin.getPlayerPreferencesRepository().updatePlayerPoints(playerPoints).join();

            if (shouldNotifyThreshold(playerPoints)) {
                notifyAdminsThresholdReached(reporterName, playerPoints);
            }

            giveReward(reporterName, reportType);

        });
    }

    public CompletableFuture<Void> incrementReportCount(String reporterName) {
        return CompletableFuture.runAsync(() -> {
            UUID playerUuid = getPlayerUuid(reporterName);
            if (playerUuid == null) {
                return;
            }

            PlayerPoints playerPoints = plugin.getPlayerPreferencesRepository()
                    .getOrCreatePlayerPoints(playerUuid, reporterName)
                    .join();

            playerPoints.setTotalReports(playerPoints.getTotalReports() + 1);
            playerPoints.setUpdatedAt(Instant.now());

            plugin.getPlayerPreferencesRepository().updatePlayerPoints(playerPoints).join();
        });
    }

    public void notifyThresholdReached(String playerName) {
        plugin.getTaskScheduler().runSync(() -> {
            UUID playerUuid = getPlayerUuid(playerName);
            if (playerUuid == null) {
                return;
            }

            PlayerPoints playerPoints = plugin.getPlayerPreferencesRepository()
                    .getOrCreatePlayerPoints(playerUuid, playerName)
                    .join();

            if (shouldNotifyThreshold(playerPoints)) {
                notifyAdminsThresholdReached(playerName, playerPoints);
            }
        });
    }

    public CompletableFuture<PlayerPoints> getPlayerPoints(UUID playerUuid, String playerName) {
        return plugin.getPlayerPreferencesRepository().getOrCreatePlayerPoints(playerUuid, playerName);
    }

    public CompletableFuture<PlayerPoints> getPlayerPointsByName(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            UUID playerUuid = getPlayerUuid(playerName);
            if (playerUuid == null) {
                return new PlayerPoints(
                        UUID.randomUUID(),
                        playerName,
                        UUID.randomUUID(),
                        0,
                        0,
                        0,
                        null,
                        Instant.now()
                );
            }
            return plugin.getPlayerPreferencesRepository()
                    .getOrCreatePlayerPoints(playerUuid, playerName)
                    .join();
        });
    }

    public CompletableFuture<Void> applyDailyDecay() {
        return CompletableFuture.runAsync(() -> {
            int decayAmount = plugin.getConfigManager().getPointsDecayAmount();
            int threshold = plugin.getConfigManager().getPointsThreshold();

            Bukkit.getOnlinePlayers().forEach(player -> {
                PlayerPoints playerPoints = plugin.getPlayerPreferencesRepository()
                        .getOrCreatePlayerPoints(player.getUniqueId(), player.getName())
                        .join();

                if (playerPoints.getPoints() > 0 && playerPoints.getPoints() >= threshold) {
                    playerPoints.applyDecay(decayAmount);
                    playerPoints.setUpdatedAt(Instant.now());
                    plugin.getPlayerPreferencesRepository().updatePlayerPoints(playerPoints).join();
                }
            });
        });
    }

    private int getPointsForReportType(String reportType) {
        Map<String, Integer> pointsMap = plugin.getConfigManager().getPointsPerReportType();
        return pointsMap.getOrDefault(reportType, 5);
    }

    private void giveReward(String playerName, String reportType) {
        if (!plugin.getVaultHook().isEnabled()) {
            return;
        }

        Map<String, Integer> rewardMap = plugin.getConfigManager().getRewardPerReportType();
        int rewardAmount = rewardMap.getOrDefault(reportType, 0);

        if (rewardAmount > 0) {
            plugin.getVaultHook().deposit(playerName, rewardAmount);
        }
    }

    private boolean shouldNotifyThreshold(PlayerPoints playerPoints) {
        int threshold = plugin.getConfigManager().getPointsThreshold();
        return playerPoints.getPoints() >= threshold;
    }

    private void notifyAdminsThresholdReached(String playerName, PlayerPoints playerPoints) {
        String permission = "aeracraft.report.notify";
        String message = plugin.getLanguageManager().getMessage("points.threshold-reached", Map.of(
                "player", playerName,
                "points", String.valueOf(playerPoints.getPoints()),
                "threshold", String.valueOf(plugin.getConfigManager().getPointsThreshold())
        ));

        Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.hasPermission(permission))
                .forEach(player -> player.sendMessage(message));
    }

    private UUID getPlayerUuid(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            return player.getUniqueId();
        }
        return null;
    }
}
