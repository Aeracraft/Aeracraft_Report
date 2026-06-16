package com.aeracraft.report.command;

import com.aeracraft.report.AeracraftReport;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LanguageCommand implements CommandExecutor, TabCompleter {

    private final AeracraftReport plugin;

    public LanguageCommand(AeracraftReport plugin) {
        this.plugin = plugin;
        plugin.getCommand("language").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.player-only"));
            return true;
        }

        if (args.length == 0) {
            sendCurrentLanguage(player);
            sendAvailableLanguages(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            sendAvailableLanguages(player);
            return true;
        }

        String languageCode = args[0];

        if (!plugin.getLanguageManager().isLanguageAvailable(languageCode)) {
            player.sendMessage(plugin.getLanguageManager().getMessage("language.not-available", List.of(
                    new java.util.AbstractMap.SimpleEntry<>("code", languageCode)
            ).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));
            sendAvailableLanguages(player);
            return true;
        }

        plugin.getLanguageManager().setPlayerLanguage(player, languageCode);
        player.sendMessage(plugin.getLanguageManager().getMessage("language.changed", List.of(
                new java.util.AbstractMap.SimpleEntry<>("language", languageCode)
        ).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));

        return true;
    }

    private void sendCurrentLanguage(Player player) {
        String currentLang = plugin.getLanguageManager().getPlayerLanguage(player);
        player.sendMessage(plugin.getLanguageManager().getMessage("language.current", List.of(
                new java.util.AbstractMap.SimpleEntry<>("language", currentLang)
        ).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));
    }

    private void sendAvailableLanguages(Player player) {
        List<String> languages = plugin.getLanguageManager().getAvailableLanguages();
        player.sendMessage(plugin.getLanguageManager().getMessage("language.available"));
        player.sendMessage(String.join(", ", languages));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String partial = args[0].toLowerCase();

            completions.addAll(plugin.getLanguageManager().getAvailableLanguages().stream()
                    .filter(lang -> lang.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList()));

            return completions;
        }

        return new ArrayList<>();
    }
}
