package com.aeracraft.report.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class ConfigValidator {

    private final FileConfiguration config;
    private final List<String> errors;

    public ConfigValidator(FileConfiguration config) {
        this.config = config;
        this.errors = new ArrayList<>();
    }

    public boolean validate() {
        validateDatabase();
        validateRedis();
        validateReport();
        validateCoreProtect();
        validateRestApi();
        validateWebhook();
        validatePoints();
        validateRewards();
        return errors.isEmpty();
    }

    private void validateDatabase() {
        String type = config.getString("database.type", "sqlite");
        if (!type.equals("sqlite") && !type.equals("mysql")) {
            errors.add("database.type must be 'sqlite' or 'mysql'");
            return;
        }

        if (type.equals("mysql")) {
            if (!config.contains("database.mysql.host")) {
                errors.add("database.mysql.host is required for MySQL");
            }
            if (!config.contains("database.mysql.database")) {
                errors.add("database.mysql.database is required for MySQL");
            }
        }
    }

    private void validateRedis() {
        if (config.getBoolean("cross-server.enabled", false)) {
            if (!config.contains("redis.host")) {
                errors.add("redis.host is required for cross-server mode");
            }
            if (!config.contains("redis.port")) {
                errors.add("redis.port is required for cross-server mode");
            }
        }
    }

    private void validateReport() {
        if (!config.contains("report.types")) {
            errors.add("report.types is required");
            return;
        }

        ConfigurationSection types = config.getConfigurationSection("report.types");
        if (types == null || types.getKeys(false).isEmpty()) {
            errors.add("report.types must have at least one report type");
        }
    }

    private void validateCoreProtect() {
        int minutes = config.getInt("coreprotect.query-minutes", 30);
        if (minutes < 1 || minutes > 1440) {
            errors.add("coreprotect.query-minutes must be between 1 and 1440");
        }

        int radius = config.getInt("coreprotect.query-radius", 50);
        if (radius < 1 || radius > 200) {
            errors.add("coreprotect.query-radius must be between 1 and 200");
        }
    }

    private void validateRestApi() {
        if (config.getBoolean("rest-api.enabled", false)) {
            if (!config.contains("rest-api.token") || config.getString("rest-api.token", "").isEmpty()) {
                errors.add("rest-api.token is required when REST API is enabled");
            }

            int port = config.getInt("rest-api.port", 4567);
            if (port < 1 || port > 65535) {
                errors.add("rest-api.port must be between 1 and 65535");
            }
        }
    }

    private void validateWebhook() {
        if (config.getBoolean("webhook.enabled", false)) {
            if (!config.contains("webhook.url") || config.getString("webhook.url", "").isEmpty()) {
                errors.add("webhook.url is required when webhook is enabled");
            }
        }
    }

    private void validatePoints() {
        if (config.contains("points.threshold")) {
            int threshold = config.getInt("points.threshold");
            if (threshold < 0) {
                errors.add("points.threshold must be non-negative");
            }
        }
    }

    private void validateRewards() {
        if (!config.contains("rewards.per-type")) {
            return;
        }

        ConfigurationSection rewards = config.getConfigurationSection("rewards.per-type");
        if (rewards != null) {
            for (String key : rewards.getKeys(false)) {
                int amount = rewards.getInt(key);
                if (amount < 0) {
                    errors.add("rewards.per-type." + key + " must be non-negative");
                }
            }
        }
    }

    public List<String> getErrors() {
        return errors;
    }
}
