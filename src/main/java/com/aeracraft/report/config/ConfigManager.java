package com.aeracraft.report.config;

import com.aeracraft.report.AeracraftReport;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final AeracraftReport plugin;
    private FileConfiguration config;

    public ConfigManager(AeracraftReport plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        validateConfig();
    }

    private void validateConfig() {
        ConfigValidator validator = new ConfigValidator(config);
        if (!validator.validate()) {
            plugin.getLogger().warning("配置文件验证失败，使用默认配置");
        }
    }

    public String getDatabaseType() {
        return config.getString("database.type", "sqlite");
    }

    public String getMySQLHost() {
        return config.getString("database.mysql.host", "localhost");
    }

    public int getMySQLPort() {
        return config.getInt("database.mysql.port", 3306);
    }

    public String getMySQLDatabase() {
        return config.getString("database.mysql.database", "aeracraft_report");
    }

    public String getMySQLUsername() {
        return config.getString("database.mysql.username", "root");
    }

    public String getMySQLPassword() {
        return config.getString("database.mysql.password", "");
    }

    public String getRedisHost() {
        return config.getString("redis.host", "localhost");
    }

    public int getRedisPort() {
        return config.getInt("redis.port", 6379);
    }

    public String getRedisPassword() {
        return config.getString("redis.password", "");
    }

    public String getServerName() {
        return config.getString("server.name", "server");
    }

    public boolean isCrossServerEnabled() {
        return config.getBoolean("cross-server.enabled", false);
    }

    public int getReportCooldownMinutes() {
        return config.getInt("report.cooldown-minutes", 5);
    }

    public int getDailyReportLimit() {
        return config.getInt("report.daily-limit", 10);
    }

    public boolean canBypassCooldown(String permission) {
        return config.getBoolean("report.bypass-permissions." + permission, false);
    }

    public List<Map<?, ?>> getReportTypes() {
        return config.getMapList("report.types");
    }

    public int getCoreProtectQueryMinutes() {
        return config.getInt("coreprotect.query-minutes", 30);
    }

    public int getCoreProtectQueryRadius() {
        return config.getInt("coreprotect.query-radius", 50);
    }

    public boolean isRestApiEnabled() {
        return config.getBoolean("rest-api.enabled", false);
    }

    public int getRestApiPort() {
        return config.getInt("rest-api.port", 4567);
    }

    public String getRestApiToken() {
        return config.getString("rest-api.token", "");
    }

    public int getRestApiRateLimit() {
        return config.getInt("rest-api.rate-limit", 100);
    }

    public boolean isWebhookEnabled() {
        return config.getBoolean("webhook.enabled", false);
    }

    public String getWebhookUrl() {
        return config.getString("webhook.url", "");
    }

    public List<String> getWebhookEvents() {
        return config.getStringList("webhook.events");
    }

    public int getPointsThreshold() {
        return config.getInt("points.threshold", 100);
    }

    public int getPointsDecayAmount() {
        return config.getInt("points.decay-amount", 5);
    }

    public int getPointsDecayIntervalDays() {
        return config.getInt("points.decay-interval-days", 7);
    }

    public Map<String, Integer> getPointsPerReportType() {
        return config.getConfigurationSection("points.per-type").getValues(false)
                .entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        e -> e.getKey(),
                        e -> (Integer) e.getValue()
                ));
    }

    public Map<String, Integer> getRewardPerReportType() {
        return config.getConfigurationSection("rewards.per-type").getValues(false)
                .entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        e -> e.getKey(),
                        e -> (Integer) e.getValue()
                ));
    }

    public String getDefaultLanguage() {
        return config.getString("language.default", "zh_CN");
    }

    public boolean isColorCodeEnabled() {
        return config.getBoolean("language.color-code", true);
    }

    public String getMessagePrefix() {
        return config.getString("language.prefix", "&6[AeracraftReport] &r");
    }
}
