package com.aeracraft.report.integration.placeholder;

import com.aeracraft.report.AeracraftReport;
import com.aeracraft.report.model.Report.ReportStatus;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class PAPIExpansion extends PlaceholderExpansion {

    private final AeracraftReport plugin;

    public PAPIExpansion(AeracraftReport plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "aeracraftreport";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        switch (params.toLowerCase()) {
            case "pending_count":
                return getPendingCount();
            case "today_count":
                return getTodayCount();
            case "my_reports_count":
                return getMyReportsCount(player.getName());
            case "my_points":
                return getMyPoints(player.getName());
            default:
                return null;
        }
    }

    private String getPendingCount() {
        try {
            int count = plugin.getReportRepository()
                    .countByStatus(ReportStatus.PENDING)
                    .join();
            return String.valueOf(count);
        } catch (Exception e) {
            return "0";
        }
    }

    private String getTodayCount() {
        try {
            int count = plugin.getReportRepository()
                    .countTodayReports()
                    .join();
            return String.valueOf(count);
        } catch (Exception e) {
            return "0";
        }
    }

    private String getMyReportsCount(String playerName) {
        try {
            int count = plugin.getReportRepository()
                    .findByReporter(playerName, 0, Integer.MAX_VALUE)
                    .join()
                    .size();
            return String.valueOf(count);
        } catch (Exception e) {
            return "0";
        }
    }

    private String getMyPoints(String playerName) {
        try {
            int points = plugin.getPointsManager()
                    .getPlayerPointsByName(playerName)
                    .join()
                    .getPoints();
            return String.valueOf(points);
        } catch (Exception e) {
            return "0";
        }
    }
}
