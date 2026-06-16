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
import java.util.regex.Pattern;

public class ReportHandler {

    private final AeracraftReport plugin;
    private final Gson gson;

    private static final int MAX_PLAYER_NAME_LENGTH = 16;
    private static final int MAX_REASON_LENGTH = 500;
    private static final int MAX_REPORT_TYPE_LENGTH = 50;
    private static final int MAX_LIST_LIMIT = 100;
    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{2,16}$");
    private static final Pattern REPORT_TYPE_PATTERN = Pattern.compile("^[a-zA-Z_]{2,50}$");

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
            String body = request.body();
            if (body == null || body.isEmpty()) {
                response.status(400);
                return gson.toJson(ApiResponse.error("Missing request body"));
            }

            ReportRequest reportRequest;
            try {
                reportRequest = gson.fromJson(body, ReportRequest.class);
            } catch (Exception e) {
                response.status(400);
                return gson.toJson(ApiResponse.error("Invalid JSON format"));
            }

            if (reportRequest.getTarget() == null || reportRequest.getTarget().isEmpty()) {
                response.status(400);
                return gson.toJson(ApiResponse.error("Target player is required"));
            }

            if (reportRequest.getType() == null || reportRequest.getType().isEmpty()) {
                response.status(400);
                return gson.toJson(ApiResponse.error("Report type is required"));
            }

            if (!validatePlayerName(reportRequest.getTarget())) {
                response.status(400);
                return gson.toJson(ApiResponse.error("Invalid target player name"));
            }

            if (!REPORT_TYPE_PATTERN.matcher(reportRequest.getType()).matches()) {
                response.status(400);
                return gson.toJson(ApiResponse.error("Invalid report type format"));
            }

            String reporter = reportRequest.getReporter() != null
                    ? reportRequest.getReporter()
                    : "API";

            if (!validatePlayerName(reporter) && !"API".equals(reporter)) {
                response.status(400);
                return gson.toJson(ApiResponse.error("Invalid reporter player name"));
            }

            if (reportRequest.getReason() != null && reportRequest.getReason().length() > MAX_REASON_LENGTH) {
                response.status(400);
                return gson.toJson(ApiResponse.error("Reason is too long (max " + MAX_REASON_LENGTH + " characters)"));
            }

            Report createdReport = plugin.getReportService().createReport(
                            reporter,
                            reportRequest.getTarget(),
                            reportRequest.getType(),
                            reportRequest.getReason() != null ? reportRequest.getReason() : "未指定",
                            plugin.getConfigManager().getServerName()
                    ).join();

            if (createdReport != null) {
                response.status(201);
                return gson.toJson(ApiResponse.success(Map.of("id", createdReport.getId().toString()), "Report created"));
            } else {
                response.status(500);
                return gson.toJson(ApiResponse.error("Failed to create report"));
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create report via API: " + e.getMessage());
            response.status(500);
            return gson.toJson(ApiResponse.error("Failed to create report"));
        }
    }

    private String getReport(Request request, spark.Response response) {
        String idStr = request.params(":id");

        if (idStr == null || idStr.isEmpty()) {
            response.status(400);
            return gson.toJson(ApiResponse.error("Report ID is required"));
        }

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
            plugin.getLogger().warning("Failed to get report via API: " + e.getMessage());
            response.status(500);
            return gson.toJson(ApiResponse.error("Failed to retrieve report"));
        }
    }

    private String getPlayerReports(Request request, spark.Response response) {
        String playerName = request.params(":name");

        if (playerName == null || playerName.isEmpty()) {
            response.status(400);
            return gson.toJson(ApiResponse.error("Player name is required"));
        }

        if (!validatePlayerName(playerName)) {
            response.status(400);
            return gson.toJson(ApiResponse.error("Invalid player name format"));
        }

        int page = parsePage(request.queryParams("page"));
        int limit = parseLimit(request.queryParams("limit"));

        try {
            List<Report> reports = plugin.getReportService()
                    .getReportsByReporter(playerName, page, limit)
                    .join();

            return gson.toJson(ApiResponse.success(reports));

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get player reports via API: " + e.getMessage());
            response.status(500);
            return gson.toJson(ApiResponse.error("Failed to retrieve reports"));
        }
    }

    private String getAllReports(Request request, spark.Response response) {
        int page = parsePage(request.queryParams("page"));
        int limit = parseLimit(request.queryParams("limit"));
        String statusStr = request.queryParams("status");

        try {
            List<Report> reports;

            if (statusStr != null && !statusStr.isEmpty()) {
                ReportStatus status;
                try {
                    status = ReportStatus.valueOf(statusStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    response.status(400);
                    return gson.toJson(ApiResponse.error("Invalid status value: " + statusStr));
                }
                reports = plugin.getReportService()
                        .getReportsByStatus(status, page, limit)
                        .join();
            } else {
                reports = plugin.getReportService()
                        .getAllReports(page, limit)
                        .join();
            }

            return gson.toJson(ApiResponse.success(reports));

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get all reports via API: " + e.getMessage());
            response.status(500);
            return gson.toJson(ApiResponse.error("Failed to retrieve reports"));
        }
    }

    private boolean validatePlayerName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return PLAYER_NAME_PATTERN.matcher(name).matches();
    }

    private int parsePage(String pageStr) {
        if (pageStr == null || pageStr.isEmpty()) {
            return 0;
        }
        try {
            int page = Integer.parseInt(pageStr);
            return Math.max(0, page);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int parseLimit(String limitStr) {
        if (limitStr == null || limitStr.isEmpty()) {
            return 20;
        }
        try {
            int limit = Integer.parseInt(limitStr);
            return Math.min(Math.max(1, limit), MAX_LIST_LIMIT);
        } catch (NumberFormatException e) {
            return 20;
        }
    }
}