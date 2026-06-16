package com.aeracraft.report.database;

import com.aeracraft.report.model.AuditLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AuditRepository {

    private final ConnectionPool connectionPool;

    public AuditRepository(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public void initializeTables() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS aeracraft_audit_log (
                id VARCHAR(36) PRIMARY KEY,
                report_id VARCHAR(36) NOT NULL,
                action VARCHAR(50) NOT NULL,
                operator VARCHAR(255) NOT NULL,
                operator_ip VARCHAR(45),
                before_snapshot TEXT,
                after_snapshot TEXT,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """;

        try (Connection conn = connectionPool.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createTableSQL);

            if (connectionPool.isUsingMySQL()) {
                stmt.execute("CREATE INDEX idx_audit_report ON aeracraft_audit_log(report_id)");
                stmt.execute("CREATE INDEX idx_audit_operator ON aeracraft_audit_log(operator)");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize audit log table", e);
        }
    }

    public CompletableFuture<Void> createAuditLog(AuditLog auditLog) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO aeracraft_audit_log (id, report_id, action, operator, operator_ip,
                    before_snapshot, after_snapshot, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, auditLog.getId().toString());
                stmt.setString(2, auditLog.getReportId().toString());
                stmt.setString(3, auditLog.getAction());
                stmt.setString(4, auditLog.getOperator());
                stmt.setString(5, auditLog.getOperatorIp());
                stmt.setString(6, auditLog.getBeforeSnapshot());
                stmt.setString(7, auditLog.getAfterSnapshot());
                stmt.setTimestamp(8, Timestamp.from(auditLog.getCreatedAt()));
                stmt.executeUpdate();

            } catch (SQLException e) {
                throw new RuntimeException("Failed to create audit log", e);
            }
        });
    }

    public CompletableFuture<List<AuditLog>> findByReportId(UUID reportId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                SELECT * FROM aeracraft_audit_log
                WHERE report_id = ?
                ORDER BY created_at ASC
                """;

            List<AuditLog> logs = new ArrayList<>();

            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, reportId.toString());
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    logs.add(mapResultSetToAuditLog(rs));
                }

            } catch (SQLException e) {
                throw new RuntimeException("Failed to find audit logs", e);
            }

            return logs;
        });
    }

    public CompletableFuture<List<AuditLog>> findByOperator(String operator, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                SELECT * FROM aeracraft_audit_log
                WHERE operator = ?
                ORDER BY created_at DESC
                LIMIT ?
                """;

            List<AuditLog> logs = new ArrayList<>();

            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, operator);
                stmt.setInt(2, limit);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    logs.add(mapResultSetToAuditLog(rs));
                }

            } catch (SQLException e) {
                throw new RuntimeException("Failed to find audit logs by operator", e);
            }

            return logs;
        });
    }

    private AuditLog mapResultSetToAuditLog(ResultSet rs) throws SQLException {
        return new AuditLog(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("report_id")),
                rs.getString("action"),
                rs.getString("operator"),
                rs.getString("operator_ip"),
                rs.getString("before_snapshot"),
                rs.getString("after_snapshot"),
                rs.getTimestamp("created_at").toInstant()
        );
    }
}
