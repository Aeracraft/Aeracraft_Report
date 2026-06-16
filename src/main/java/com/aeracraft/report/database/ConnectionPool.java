package com.aeracraft.report.database;

import com.aeracraft.report.AeracraftReport;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

public class ConnectionPool {

    private final AeracraftReport plugin;
    private HikariDataSource dataSource;
    private boolean useMySQL;

    public ConnectionPool(AeracraftReport plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        FileConfiguration config = plugin.getConfig();
        String databaseType = config.getString("database.type", "sqlite").toLowerCase();

        HikariConfig hikariConfig = new HikariConfig();

        if ("mysql".equals(databaseType)) {
            useMySQL = true;
            hikariConfig.setJdbcUrl(String.format(
                    "jdbc:mysql://%s:%d/%s?useSSL=%s&serverTimezone=%s&allowPublicKeyRetrieval=%s",
                    config.getString("database.mysql.host", "localhost"),
                    config.getInt("database.mysql.port", 3306),
                    config.getString("database.mysql.database", "aeracraft_report"),
                    config.getBoolean("database.mysql.useSSL", false),
                    config.getString("database.mysql.serverTimezone", "UTC"),
                    config.getBoolean("database.mysql.allowPublicKeyRetrieval", true)
            ));
            hikariConfig.setUsername(config.getString("database.mysql.username", "root"));
            hikariConfig.setPassword(config.getString("database.mysql.password", ""));
            hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");

            int maxPoolSize = config.getInt("database.mysql.maxPoolSize", 10);
            hikariConfig.setMaximumPoolSize(maxPoolSize);
            hikariConfig.setMinimumIdle(config.getInt("database.mysql.minIdle", 2));
        } else {
            useMySQL = false;
            String dbPath = plugin.getDataFolder().toPath().resolve("aeracraft_report.db").toString();
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbPath);
            hikariConfig.setDriverClassName("org.sqlite.JDBC");

            hikariConfig.setMaximumPoolSize(1);
            hikariConfig.setMinimumIdle(1);
        }

        hikariConfig.setPoolName("AeracraftReport-HikariCP");
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);
        hikariConfig.setAutoCommit(true);

        try {
            dataSource = new HikariDataSource(hikariConfig);
            plugin.getLogger().info("数据库连接池初始化成功！使用 " + (useMySQL ? "MySQL" : "SQLite"));
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "数据库连接池初始化失败！", e);
            throw new RuntimeException("Failed to initialize connection pool", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public boolean isUsingMySQL() {
        return useMySQL;
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("数据库连接池已关闭");
        }
    }
}
