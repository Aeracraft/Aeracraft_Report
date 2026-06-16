package com.aeracraft.report.rest.handler;

import com.aeracraft.report.AeracraftReport;
import com.aeracraft.report.model.Report.ReportStatus;
import com.aeracraft.report.rest.dto.ApiResponse;
import com.google.gson.Gson;
import spark.Request;
import spark.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class StatsHandler {

    private final AeracraftReport plugin;
    private final Gson gson;

    public StatsHandler(AeracraftReport plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
    }

    public void registerRoutes(Service server, String basePath) {
        server.get(basePath + "/stats", this::getStats);
    }

    private String getStats(Request request, spark.Response response) {
        try {
            CompletableFuture<Integer> pendingCount = plugin.getReportRepository()
                    .countByStatus(ReportStatus.PENDING);

            CompletableFuture<Integer> inProgressCount = plugin.getReportRepository()
                    .countByStatus(ReportStatus.IN_PROGRESS);

            CompletableFuture<Integer> completedCount = plugin.getReportRepository()
                    .countByStatus(ReportStatus.COMPLETED);

            CompletableFuture<Integer> rejectedCount = plugin.getReportRepository()
                    .countByStatus(ReportStatus.REJECTED);

            CompletableFuture<Integer> todayCount = plugin.getReportRepository()
                    .countTodayReports();

            CompletableFuture.allOf(
                    pendingCount, inProgressCount, completedCount, rejectedCount, todayCount
            ).join();

            Map<String, Object> stats = new HashMap<>();
            stats.put("pending", pendingCount.get());
            stats.put("inProgress", inProgressCount.get());
            stats.put("completed", completedCount.get());
            stats.put("rejected", rejectedCount.get());
            stats.put("today", todayCount.get());
            stats.put("total", pendingCount.get() + inProgressCount.get() + completedCount.get() + rejectedCount.get());

            return gson.toJson(ApiResponse.success(stats));

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get stats via API: " + e.getMessage());
            response.status(500);
            return gson.toJson(ApiResponse.error("Failed to retrieve stats"));
        }
    }
}
