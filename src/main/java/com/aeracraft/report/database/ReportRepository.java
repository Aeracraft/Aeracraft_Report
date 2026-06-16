package com.aeracraft.report.database;

import com.aeracraft.report.model.Evidence;
import com.aeracraft.report.model.Report;
import com.aeracraft.report.model.Report.ReportStatus;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class ReportRepository {

    private final ConnectionPool connectionPool;
    private final Gson gson;

    public ReportRepository(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
        this.gson = new Gson();
    }

    public void initializeTables() {
        String createReportsTable = """
            CREATE TABLE IF NOT EXISTS aeracraft_reports (
                id VARCHAR(36) PRIMARY KEY,
                reporter VARCHAR(255) NOT NULL,
                target VARCHAR(255) NOT NULL,
                type VARCHAR(50) NOT NULL,
                status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                evidence_hash VARCHAR(64),
                linked_ban_id VARCHAR(255),
                server_name VARCHAR(100),
                handled_by VARCHAR(255),
                resolution_note TEXT,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createEvidenceTable = """
            CREATE TABLE IF NOT EXISTS aeracraft_evidence (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                report_id VARCHAR(36) NOT NULL,
                evidence_json TEXT NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (report_id) REFERENCES aeracraft_reports(id) ON DELETE CASCADE
            )
            """;

        try (Connection conn = connectionPool.getConnection();
             Statement stmt = conn.createStatement()) {

            if (connectionPool.isUsingMySQL()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS aeracraft_reports (" +
                        "id VARCHAR(36) PRIMARY KEY," +
                        "reporter VARCHAR(255) NOT NULL," +
                        "target VARCHAR(255) NOT NULL," +
                        "type VARCHAR(50) NOT NULL," +
                        "status VARCHAR(20) NOT NULL DEFAULT 'PENDING'," +
                        "evidence_hash VARCHAR(64)," +
                        "linked_ban_id VARCHAR(255)," +
                        "server_name VARCHAR(100)," +
                        "handled_by VARCHAR(255)," +
                        "resolution_note TEXT," +
                        "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
                stmt.execute("CREATE INDEX idx_status_created ON aeracraft_reports(status, created_at)");
                stmt.execute("CREATE INDEX idx_target ON aeracraft_reports(target)");
                stmt.execute("CREATE INDEX idx_reporter ON aeracraft_reports(reporter)");
            } else {
                stmt.execute(createReportsTable.replace("BIGINT", "INTEGER"));
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_status_created ON aeracraft_reports(status, created_at)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_target ON aeracraft_reports(target)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_reporter ON aeracraft_reports(reporter)");
            }
            stmt.execute(createEvidenceTable);

            Bukkit.getLogger().info("数据库表初始化完成");
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "初始化数据库表失败", e);
        }
    }

    public CompletableFuture<Report> createReport(Report report, Evidence evidence) {
        return CompletableFuture.runAsync(() -> {
            String insertReportSQL = """
                INSERT INTO aeracraft_reports (id, reporter, target, type, status, evidence_hash,
                    server_name, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

            String insertEvidenceSQL = """
                INSERT INTO aeracraft_evidence (report_id, evidence_json, created_at)
                VALUES (?, ?, ?)
                """;

            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement reportStmt = conn.prepareStatement(insertReportSQL);
                 PreparedStatement evidenceStmt = conn.prepareStatement(insertEvidenceSQL)) {

                reportStmt.setString(1, report.getId().toString());
                reportStmt.setString(2, report.getReporter());
                reportStmt.setString(3, report.getTarget());
                reportStmt.setString(4, report.getType());
                reportStmt.setString(5, report.getStatus().name());
                reportStmt.setString(6, report.getEvidenceHash());
                reportStmt.setString(7, report.getServerName());
                reportStmt.setTimestamp(8, Timestamp.from(report.getCreatedAt()));
                reportStmt.setTimestamp(9, Timestamp.from(report.getUpdatedAt()));
                reportStmt.executeUpdate();

                evidenceStmt.setString(1, report.getId().toString());
                evidenceStmt.setString(2, gson.toJson(evidence));
                evidenceStmt.setTimestamp(3, Timestamp.from(evidence.getCollectedAt()));
                evidenceStmt.executeUpdate();

            } catch (SQLException e) {
                throw new RuntimeException("Failed to create report", e);
            }
        }).thenApply(v -> report);
    }

    public CompletableFuture<Optional<Report>> findById(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM aeracraft_reports WHERE id = ?";

            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, id.toString());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return Optional.of(mapResultSetToReport(rs));
                }
                return Optional.empty();

            } catch (SQLException e) {
                throw new RuntimeException("Failed to find report by id", e);
            }
        });
    }

    public CompletableFuture<List<Report>> findByStatus(ReportStatus status, int limit, int offset) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                SELECT * FROM aeracraft_reports
                WHERE status = ?
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """;

            List<Report> reports = new ArrayList<>();

            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, status.name());
                stmt.setInt(2, limit);
                stmt.setInt(3, offset);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    reports.add(mapResultSetToReport(rs));
                }

            } catch (SQLException e) {
                throw new RuntimeException("Failed to find reports by status", e);
            }

            return reports;
        });
    }

    public CompletableFuture<List<Report>> findByTarget(String target, int limit, int offset) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                SELECT * FROM aeracraft_reports
                WHERE target = ?
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """;

            List<Report> reports = new ArrayList<>();

            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, target);
                stmt.setInt(2, limit);
                stmt.setInt(3, offset);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    reports.add(mapResultSetToReport(rs));
                }

            } catch (SQLException e) {
                throw new RuntimeException("Failed to find reports by target", e);
            }

            return reports;
        });
    }

    public CompletableFuture<List<Report>> findByReporter(String reporter, int limit, int offset) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                SELECT * FROM aeracraft_reports
                WHERE reporter = ?
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """;

            List<Report> reports = new ArrayList<>();

            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, reporter);
                stmt.setInt(2, limit);
                stmt.setInt(3, offset);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    reports.add(mapResultSetToReport(rs));
                }

            } catch (SQLException e) {
                throw new RuntimeException("Failed to find reports by reporter", e);
            }

            return reports;
        });
    }

    public CompletableFuture<List<Report>> findAll(int limit, int offset) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                SELECT * FROM aeracraft_reports
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """;

            List<Report> reports = new ArrayList<>();

            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, limit);
                stmt.setInt(2, offset);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    reports.add(mapResultSetToReport(rs));
                }

            } catch (SQLException e) {
                throw new RuntimeException("Failed to find all reports", e);
            }

            return reports;
        });
    }

    public CompletableFuture<Void> updateStatus(UUID id, ReportStatus status, String handledBy, String note) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                UPDATE aeracraft_reports
                SET status = ?, handled_by = ?, resolution_note = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, status.name());
                stmt.setString(2, handledBy);
                stmt.setString(3, note);
                stmt.setString(4, id.toString());
                stmt.executeUpdate();

            } catch (SQLException e) {
                throw new RuntimeException("Failed to update report status", e);
            }
        });
    }

    public CompletableFuture<Void> linkBan(UUID reportId, String banId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE aeracraft_reports SET linked_ban_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, banId);
                stmt.setString(2, reportId.toString());
                stmt.executeUpdate();

            } catch (SQLException e) {
                throw new RuntimeException("Failed to link ban", e);
            }
        });
    }

    public CompletableFuture<Optional<Evidence>> findEvidenceByReportId(UUID reportId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT evidence_json FROM aeracraft_evidence WHERE report_id = ? ORDER BY created_at DESC LIMIT 1";

            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, reportId.toString());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String json = rs.getString("evidence_json");
                    Evidence evidence = gson.fromJson(json, Evidence.class);
                    return Optional.of(evidence);
                }
                return Optional.empty();

            } catch (SQLException e) {
                throw new RuntimeException("Failed to find evidence", e);
            }
        });
    }

    public CompletableFuture<Integer> countByStatus(ReportStatus status) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM aeracraft_reports WHERE status = ?";

            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, status.name());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;

            } catch (SQLException e) {
                throw new RuntimeException("Failed to count reports", e);
            }
        });
    }

    public CompletableFuture<Integer> countTodayReports() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = connectionPool.isUsingMySQL()
                    ? "SELECT COUNT(*) FROM aeracraft_reports WHERE DATE(created_at) = CURDATE()"
                    : "SELECT COUNT(*) FROM aeracraft_reports WHERE DATE(created_at) = DATE('now')";

            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;

            } catch (SQLException e) {
                throw new RuntimeException("Failed to count today's reports", e);
            }
        });
    }

    public CompletableFuture<Boolean> existsDuplicateReport(String reporter, String target, long cooldownMinutes) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = connectionPool.isUsingMySQL()
                    ? "SELECT COUNT(*) FROM aeracraft_reports WHERE reporter = ? AND target = ? AND created_at > DATE_SUB(NOW(), INTERVAL ? MINUTE)"
                    : "SELECT COUNT(*) FROM aeracraft_reports WHERE reporter = ? AND target = ? AND created_at > datetime('now', '-' || ? || ' minutes')";

            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, reporter);
                stmt.setString(2, target);
                stmt.setInt(3, (int) cooldownMinutes);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;

            } catch (SQLException e) {
                throw new RuntimeException("Failed to check duplicate report", e);
            }
        });
    }

    private Report mapResultSetToReport(ResultSet rs) throws SQLException {
        return new Report(
                UUID.fromString(rs.getString("id")),
                rs.getString("reporter"),
                rs.getString("target"),
                rs.getString("type"),
                ReportStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getString("evidence_hash"),
                rs.getString("linked_ban_id"),
                rs.getString("server_name"),
                rs.getString("handled_by"),
                rs.getString("resolution_note")
        );
    }
}
