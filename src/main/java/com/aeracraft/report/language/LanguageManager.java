package com.aeracraft.report.language;

import com.aeracraft.report.AeracraftReport;
import com.aeracraft.report.database.PlayerPreferencesRepository;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LanguageManager {

    private final AeracraftReport plugin;
    private final Map<String, FileConfiguration> languages;
    private String defaultLanguage;
    private String fallbackLanguage;

    public LanguageManager(AeracraftReport plugin) {
        this.plugin = plugin;
        this.languages = new HashMap<>();
        loadLanguages();
    }

    private void loadLanguages() {
        defaultLanguage = plugin.getConfigManager().getDefaultLanguage();
        fallbackLanguage = "en_US";

        loadLanguage("zh_CN");
        loadLanguage("en_US");

        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (langFolder.exists() && langFolder.isDirectory()) {
            File[] files = langFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    String name = file.getName().replace(".yml", "");
                    if (!name.equals("zh_CN") && !name.equals("en_US")) {
                        loadCustomLanguage(file);
                    }
                }
            }
        }

        if (!languages.containsKey(defaultLanguage)) {
            defaultLanguage = "zh_CN";
        }
    }

    private void loadLanguage(String name) {
        File langFile = new File(plugin.getDataFolder(), "lang/" + name + ".yml");
        if (!langFile.exists()) {
            plugin.saveResource("lang/" + name + ".yml", false);
        }

        FileConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);
        languages.put(name, langConfig);
        plugin.getLogger().info("已加载语言文件: " + name);
    }

    private void loadCustomLanguage(File file) {
        try {
            FileConfiguration langConfig = YamlConfiguration.loadConfiguration(file);
            String name = file.getName().replace(".yml", "");
            languages.put(name, langConfig);
            plugin.getLogger().info("已加载自定义语言文件: " + name);
        } catch (Exception e) {
            plugin.getLogger().warning("加载语言文件失败: " + file.getName() + " - " + e.getMessage());
        }
    }

    public void reload() {
        languages.clear();
        loadLanguages();
    }

    public String getMessage(Player player, String key, Map<String, String> placeholders) {
        String languageCode = getPlayerLanguage(player);
        return getMessage(languageCode, key, placeholders);
    }

    public String getMessage(Player player, String key) {
        return getMessage(player, key, Collections.emptyMap());
    }

    public String getMessage(String languageCode, String key, Map<String, String> placeholders) {
        FileConfiguration langConfig = languages.getOrDefault(languageCode, languages.get(defaultLanguage));
        if (langConfig == null) {
            langConfig = languages.get(fallbackLanguage);
        }

        String message = langConfig.getString(key);
        if (message == null) {
            FileConfiguration fallbackConfig = languages.get(fallbackLanguage);
            message = fallbackConfig != null ? fallbackConfig.getString(key) : key;
        }

        if (message == null) {
            return key;
        }

        if (plugin.getConfigManager().isColorCodeEnabled()) {
            message = message.replace("&", "§");
        }

        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        String prefix = plugin.getConfigManager().getMessagePrefix();
        if (plugin.getConfigManager().isColorCodeEnabled()) {
            prefix = prefix.replace("&", "§");
        }

        return prefix + message;
    }

    public String getMessage(String key) {
        return getMessage(defaultLanguage, key, Collections.emptyMap());
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        return getMessage(defaultLanguage, key, placeholders);
    }

    public String getPlayerLanguage(Player player) {
        UUID playerUuid = player.getUniqueId();
        Optional<String> languageCode = plugin.getPlayerPreferencesRepository()
                .getPlayerLanguage(playerUuid).join();

        return languageCode.orElse(defaultLanguage);
    }

    public void setPlayerLanguage(Player player, String languageCode) {
        if (languages.containsKey(languageCode)) {
            UUID playerUuid = player.getUniqueId();
            String playerName = player.getName();
            plugin.getPlayerPreferencesRepository().setPlayerLanguage(playerUuid, playerName, languageCode);
        }
    }

    public List<String> getAvailableLanguages() {
        return new ArrayList<>(languages.keySet());
    }

    public boolean isLanguageAvailable(String languageCode) {
        return languages.containsKey(languageCode);
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public String getFallbackLanguage() {
        return fallbackLanguage;
    }
}
