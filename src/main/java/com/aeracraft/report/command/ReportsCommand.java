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
import java.util.stream.Collectors;

public class ReportsCommand implements CommandExecutor, TabCompleter {

    private final AeracraftReport plugin;

    public ReportsCommand(AeracraftReport plugin) {
        this.plugin = plugin;
        plugin.getCommand("reports").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.player-only"));
            return true;
        }

        if (!player.hasPermission("aeracraft.report.gui")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("command.no-permission"));
            return true;
        }

        openReportsGUI(player);

        return true;
    }

    private void openReportsGUI(Player player) {
        plugin.getReportService().getAllReports(0, 45)
                .thenAccept(reports -> {
                    org.bukkit.inventory.Inventory inventory = plugin.getReportListGUI()
                            .createInventory(0, reports, null);
                    player.openInventory(inventory);
                })
                .exceptionally(ex -> {
                    player.sendMessage(plugin.getLanguageManager().getMessage("reports.error-loading"));
                    return null;
                });
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        return new ArrayList<>();
    }
}
