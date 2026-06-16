package com.aeracraft.report.database;

import com.aeracraft.report.model.PlayerPoints;

import java.sql.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerPreferencesRepository {

    private final ConnectionPool connectionPool;

    public PlayerPreferencesRepository(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public void initializeTables() {
        String createLanguageTable = """
            CREATE TABLE IF NOT EXISTS aeracraft_player_language (
                player_uuid VARCHAR(36) PRIMARY KEY,
                player_name VARCHAR(255) NOT NULL,
                language_code VARCHAR(10) NOT NULL DEFAULT 'zh_CN',
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createPointsTable = """
            CREATE TABLE IF NOT EXISTS aeracraft_player_points (
                id VARCHAR(36) PRIMARY KEY,
                player_uuid VARCHAR(36) NOT NULL UNIQUE,
                player_name VARCHAR(255) NOT NULL,
                points INTEGER NOT NULL DEFAULT 0,
                total_valid_reports INTEGER NOT NULL DEFAULT 0,
                total_reports INTEGER NOT NULL DEFAULT 0,
                last_decay_at TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """;

        try (Connection conn = connectionPool.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createLanguageTable);
            stmt.execute(createPointsTable);

            if (connectionPool.isUsingMySQL()) {
                stmt.execute("CREATE INDEX idx_points_player ON aeracraft_player_points(player_uuid)");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize player preferences tables", e);
        }
    }

    public CompletableFuture<Void> setPlayerLanguage(UUID playerUuid, String playerName, String languageCode) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO aeracraft_player_language (player_uuid, player_name, language_code)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE language_code = ?, player_name = ?
                """;

            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, playerName);
                stmt.setString(3, languageCode);
                stmt.setString(4, languageCode);
                stmt.setString(5, playerName);
                stmt.executeUpdate();

            } catch (SQLException e) {
                throw new RuntimeException("Failed to set player language", e);
            }
        });
    }

    public CompletableFuture<Optional<String>> getPlayerLanguage(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT language_code FROM aeracraft_player_language WHERE player_uuid = ?";

            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, playerUuid.toString());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return Optional.of(rs.getString("language_code"));
                }
                return Optional.empty();

            } catch (SQLException e) {
                throw new RuntimeException("Failed to get player language", e);
            }
        });
    }

    public CompletableFuture<PlayerPoints> getOrCreatePlayerPoints(UUID playerUuid, String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            String selectSql = "SELECT * FROM aeracraft_player_points WHERE player_uuid = ?";
            String insertSql = "INSERT INTO aeracraft_player_points (id, player_uuid, player_name, points) VALUES (?, ?, ?, 0)";

            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement selectStmt = conn.prepareStatement(selectSql);
                 PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

                selectStmt.setString(1, playerUuid.toString());
                ResultSet rs = selectStmt.executeQuery();

                if (rs.next()) {
                    return mapResultSetToPlayerPoints(rs);
                }

                UUID newId = UUID.randomUUID();
                insertStmt.setString(1, newId.toString());
                insertStmt.setString(2, playerUuid.toString());
                insertStmt.setString(3, playerName);
                insertStmt.executeUpdate();

                return new PlayerPoints(
                        newId,
                        playerName,
                        playerUuid,
                        0,
                        0,
                        0,
                        null,
                        Instant.now()
                );

            } catch (SQLException e) {
                throw new RuntimeException("Failed to get or create player points", e);
            }
        });
    }

    public CompletableFuture<Void> updatePlayerPoints(PlayerPoints playerPoints) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                UPDATE aeracraft_player_points
                SET points = ?, total_valid_reports = ?, total_reports = ?,
                    last_decay_at = ?, updated_at = CURRENT_TIMESTAMP
                WHERE player_uuid = ?
                """;

            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, playerPoints.getPoints());
                stmt.setInt(2, playerPoints.getTotalValidReports());
                stmt.setInt(3, playerPoints.getTotalReports());
                stmt.setTimestamp(4, playerPoints.getLastDecayAt() != null
                        ? Timestamp.from(playerPoints.getLastDecayAt()) : null);
                stmt.setString(5, playerPoints.getPlayerUniqueId().toString());
                stmt.executeUpdate();

            } catch (SQLException e) {
                throw new RuntimeException("Failed to update player points", e);
            }
        });
    }

    private PlayerPoints mapResultSetToPlayerPoints(ResultSet rs) throws SQLException {
        return new PlayerPoints(
                UUID.fromString(rs.getString("id")),
                rs.getString("player_name"),
                UUID.fromString(rs.getString("player_uuid")),
                rs.getInt("points"),
                rs.getInt("total_valid_reports"),
                rs.getInt("total_reports"),
                rs.getTimestamp("last_decay_at") != null
                        ? rs.getTimestamp("last_decay_at").toInstant() : null,
                rs.getTimestamp("updated_at").toInstant()
        );
    }
}
