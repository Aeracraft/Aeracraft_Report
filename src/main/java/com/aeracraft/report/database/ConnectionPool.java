package com.aeracraft.report.database;

import com.aeracraft.report.AeracraftReport;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class ConnectionPool {

    private final AeracraftReport plugin;
    private HikariDataSource dataSource;
    private boolean useMySQL;

    private static final Pattern HOSTNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9.-]+$");
    private static final Pattern DATABASE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    public ConnectionPool(AeracraftReport plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        FileConfiguration config = plugin.getConfig();
        String databaseType = config.getString("database.type", "sqlite").toLowerCase();

        if (!"mysql".equals(databaseType) && !"sqlite".equals(databaseType)) {
            plugin.getLogger().log(Level.SEVERE, "无效的数据库类型: " + databaseType + "，使用 SQLite");
            databaseType = "sqlite";
        }

        HikariConfig hikariConfig = new HikariConfig();

        if ("mysql".equals(databaseType)) {
            useMySQL = true;
            
            String host = config.getString("database.mysql.host", "localhost");
            int port = config.getInt("database.mysql.port", 3306);
            String database = config.getString("database.mysql.database", "aeracraft_report");
            String username = config.getString("database.mysql.username", "root");
            String password = config.getString("database.mysql.password", "");

            validateMySQLConfig(host, port, database, username);

            hikariConfig.setJdbcUrl(String.format(
                    "jdbc:mysql://%s:%d/%s?useSSL=%s&serverTimezone=%s&allowPublicKeyRetrieval=%s&characterEncoding=utf8mb4",
                    host,
                    port,
                    database,
                    config.getBoolean("database.mysql.useSSL", false),
                    config.getString("database.mysql.serverTimezone", "UTC"),
                    config.getBoolean("database.mysql.allowPublicKeyRetrieval", true)
            ));
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");

            int maxPoolSize = Math.min(config.getInt("database.mysql.maxPoolSize", 10), 20);
            int minIdle = Math.min(config.getInt("database.mysql.minIdle", 2), maxPoolSize);
            
            hikariConfig.setMaximumPoolSize(maxPoolSize);
            hikariConfig.setMinimumIdle(minIdle);
            hikariConfig.setLeakDetectionThreshold(30000);
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
        hikariConfig.setRegisterMbeans(false);

        try {
            dataSource = new HikariDataSource(hikariConfig);
            
            try (Connection testConn = dataSource.getConnection()) {
                if (testConn.isValid(5)) {
                    plugin.getLogger().info("数据库连接池初始化成功！使用 " + (useMySQL ? "MySQL" : "SQLite"));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "数据库连接池初始化失败！", e);
            throw new RuntimeException("Failed to initialize connection pool", e);
        }
    }

    private void validateMySQLConfig(String host, int port, String database, String username) {
        if (!HOSTNAME_PATTERN.matcher(host).matches()) {
            throw new IllegalArgumentException("无效的 MySQL 主机名: " + host);
        }
        
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("无效的 MySQL 端口: " + port);
        }
        
        if (!DATABASE_NAME_PATTERN.matcher(database).matches()) {
            throw new IllegalArgumentException("无效的 MySQL 数据库名: " + database);
        }
        
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("无效的 MySQL 用户名: " + username);
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("连接池未初始化或已关闭");
        }
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
