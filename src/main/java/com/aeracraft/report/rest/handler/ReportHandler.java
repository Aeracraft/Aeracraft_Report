package com.aeracraft.report.rest.handler;

import com.aeracraft.report.AeracraftReport;
import com.aeracraft.report.model.Report;
import com.aeracraft.report.model.Report.ReportStatus;
import com.aeracraft.report.rest.dto.ApiResponse;
import com.aeracraft.report.rest.dto.ReportRequest;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import spark.Request;
import spark.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ReportHandler {

    private final AeracraftReport plugin;
    private final Gson gson;

    public ReportHandler(AeracraftReport plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
    }

    public void registerRoutes(Service server, String basePath) {
        server.post(basePath + "/reports", this::createReport);
        server.get(basePath + "/reports/:id", this::getReport);
        server.get(basePath + "/players/:name/reports", this::getPlayerReports);
        server.get(basePath + "/reports", this::getAllReports);
    }

    private String createReport(Request request, spark.Response response) {
        try {
            ReportRequest reportRequest = gson.fromJson(request.body(), ReportRequest.class);

            if (reportRequest.getTarget() == null || reportRequest.getType() == null) {
                response.status(400);
                return gson.toJson(ApiResponse.error("Missing required fields"));
            }

            String reporter = reportRequest.getReporter() != null
                    ? reportRequest.getReporter()
                    : "API";

            plugin.getReportService().createReport(
                            reporter,
                            reportRequest.getTarget(),
                            reportRequest.getType(),
                            reportRequest.getReason() != null ? reportRequest.getReason() : "未指定",
                            plugin.getConfigManager().getServerName()
                    ).thenAccept(report -> {
                        response.status(201);
                    });

            return gson.toJson(ApiResponse.success(null, "Report created"));

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create report via API: " + e.getMessage());
            response.status(500);
            return gson.toJson(ApiResponse.error("Failed to create report"));
        }
    }

    private String getReport(Request request, spark.Response response) {
        String idStr = request.params(":id");

        try {
            UUID reportId = UUID.fromString(idStr);
            Optional<Report> reportOpt = plugin.getReportService().getReportById(reportId).join();

            if (reportOpt.isEmpty()) {
                response.status(404);
                return gson.toJson(ApiResponse.error("Report not found"));
            }

            return gson.toJson(ApiResponse.success(reportOpt.get()));

        } catch (IllegalArgumentException e) {
            response.status(400);
            return gson.toJson(ApiResponse.error("Invalid report ID format"));
        } catch (Exception e) {
            response.status(500);
            return gson.toJson(ApiResponse.error("Failed to retrieve report"));
        }
    }

    private String getPlayerReports(Request request, spark.Response response) {
        String playerName = request.params(":name");
        String pageStr = request.queryParams("page");
        String limitStr = request.queryParams("limit");

        int page = pageStr != null ? Integer.parseInt(pageStr) : 0;
        int limit = limitStr != null ? Integer.parseInt(limitStr) : 20;

        try {
            List<Report> reports = plugin.getReportService()
                    .getReportsByReporter(playerName, page, limit)
                    .join();

            return gson.toJson(ApiResponse.success(reports));

        } catch (Exception e) {
            response.status(500);
            return gson.toJson(ApiResponse.error("Failed to retrieve reports"));
        }
    }

    private String getAllReports(Request request, spark.Response response) {
        String pageStr = request.queryParams("page");
        String limitStr = request.queryParams("limit");
        String statusStr = request.queryParams("status");

        int page = pageStr != null ? Integer.parseInt(pageStr) : 0;
        int limit = limitStr != null ? Integer.parseInt(limitStr) : 20;

        try {
            List<Report> reports;

            if (statusStr != null) {
                ReportStatus status = ReportStatus.valueOf(statusStr.toUpperCase());
                reports = plugin.getReportService()
                        .getReportsByStatus(status, page, limit)
                        .join();
            } else {
                reports = plugin.getReportService()
                        .getAllReports(page, limit)
                        .join();
            }

            return gson.toJson(ApiResponse.success(reports));

        } catch (IllegalArgumentException e) {
            response.status(400);
            return gson.toJson(ApiResponse.error("Invalid status value"));
        } catch (Exception e) {
            response.status(500);
            return gson.toJson(ApiResponse.error("Failed to retrieve reports"));
        }
    }
}
