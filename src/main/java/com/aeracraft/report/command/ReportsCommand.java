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
        plugin.getLogger().info("正在为玩家 " + player.getName() + " 打开举报GUI...");
        player.sendMessage(plugin.getLanguageManager().getMessage("reports.gui-opening"));
        
        plugin.getReportService().getAllReports(0, 45)
                .thenAccept(reports -> {
                    plugin.getLogger().info("成功加载 " + reports.size() + " 条举报记录");
                    org.bukkit.inventory.Inventory inventory = plugin.getReportListGUI()
                            .createInventory(0, reports, null);
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                        if (player.isOnline()) {
                            player.openInventory(inventory);
                            plugin.getLogger().info("举报GUI已打开");
                        }
                    });
                })
                .exceptionally(ex -> {
                    plugin.getLogger().severe("加载举报列表失败: " + ex.getMessage());
                    ex.printStackTrace();
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                        if (player.isOnline()) {
                            player.sendMessage(plugin.getLanguageManager().getMessage("reports.error-loading"));
                        }
                    });
                    return null;
                });
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        return new ArrayList<>();
    }
}
