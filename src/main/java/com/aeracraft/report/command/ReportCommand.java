package com.aeracraft.report.command;

import com.aeracraft.report.AeracraftReport;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportCommand implements CommandExecutor, TabCompleter {

    private final AeracraftReport plugin;

    public ReportCommand(AeracraftReport plugin) {
        this.plugin = plugin;
        plugin.getCommand("report").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.player-only"));
            return true;
        }

        if (!player.hasPermission("aeracraft.report.use")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("command.no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("gui")) {
            openReportGUI(player);
            return true;
        }

        String targetName = args[0];
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "未指定";

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("report.player-not-found", Map.of(
                    "player", targetName
            )));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(plugin.getLanguageManager().getMessage("report.cannot-report-self"));
            return true;
        }

        String reportType = determineReportType(reason);
        submitReport(player, target, reportType, reason);

        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(plugin.getLanguageManager().getMessage("report.usage-header"));
        player.sendMessage(plugin.getLanguageManager().getMessage("report.usage-gui"));
        player.sendMessage(plugin.getLanguageManager().getMessage("report.usage-report"));
    }

    private void openReportGUI(Player player) {
        if (!player.hasPermission("aeracraft.report.gui")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("command.no-permission"));
            return;
        }

        player.sendMessage(plugin.getLanguageManager().getMessage("report.gui-opening"));
    }

    private String determineReportType(String reason) {
        String lowerReason = reason.toLowerCase();
        if (lowerReason.contains("作弊") || lowerReason.contains("外挂") || lowerReason.contains("hack")) {
            return "CHEATING";
        } else if (lowerReason.contains("骂") || lowerReason.contains("侮辱") || lowerReason.contains("curse")) {
            return "HARASSMENT";
        } else if (lowerReason.contains("破坏") || lowerReason.contains(" grief")) {
            return "GRIEFING";
        } else if (lowerReason.contains("刷") || lowerReason.contains("spam")) {
            return "SPAM";
        }
        return "OTHER";
    }

    private void submitReport(Player player, Player target, String type, String reason) {
        plugin.getReportService().isOnCooldown(player, target.getName())
                .thenAccept(onCooldown -> {
                    if (onCooldown) {
                        player.sendMessage(plugin.getLanguageManager().getMessage("report.on-cooldown"));
                        return;
                    }

                    plugin.getReportService().canCreateReport(player)
                            .thenAccept(canCreate -> {
                                if (!canCreate) {
                                    player.sendMessage(plugin.getLanguageManager().getMessage("report.daily-limit-reached"));
                                    return;
                                }

                                plugin.getReportService().createReport(player, target, type, reason)
                                        .thenAccept(report -> {
                                            player.sendMessage(plugin.getLanguageManager().getMessage("report.success", Map.of(
                                                    "id", report.getId().toString()
                                            )));
                                        });
                            });
                });
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String partial = args[0].toLowerCase();

            completions.add("gui");
            completions.addAll(Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.equals(sender))
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList()));

            return completions;
        }

        if (args.length == 2) {
            return List.of("<reason>");
        }

        return new ArrayList<>();
    }
}
