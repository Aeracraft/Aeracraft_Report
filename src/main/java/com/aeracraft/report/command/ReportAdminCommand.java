package com.aeracraft.report.command;

import com.aeracraft.report.AeracraftReport;
import com.aeracraft.report.model.Report;
import com.aeracraft.report.model.Report.ReportStatus;
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
import java.util.UUID;
import java.util.stream.Collectors;

public class ReportAdminCommand implements CommandExecutor, TabCompleter {

    private final AeracraftReport plugin;

    public ReportAdminCommand(AeracraftReport plugin) {
        this.plugin = plugin;
        plugin.getCommand("reportadmin").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload" -> handleReload(sender);
            case "status" -> handleStatus(sender, args);
            case "list" -> handleList(sender, args);
            case "complete" -> handleComplete(sender, args);
            case "reject" -> handleReject(sender, args);
            case "teleport", "tp" -> handleTeleport(sender, args);
            case "freeze" -> handleFreeze(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("=== AeracraftReport 管理命令 ===");
        sender.sendMessage("/reportadmin reload - 重载配置");
        sender.sendMessage("/reportadmin status <举报ID> - 查看举报状态");
        sender.sendMessage("/reportadmin list [页码] - 列出所有举报");
        sender.sendMessage("/reportadmin complete <举报ID> [备注] - 完成举报");
        sender.sendMessage("/reportadmin reject <举报ID> [原因] - 驳回举报");
        sender.sendMessage("/reportadmin teleport <举报ID> - 传送到举报地点");
        sender.sendMessage("/reportadmin freeze <玩家名> - 冻结玩家");
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("aeracraft.report.admin.reload")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.no-permission"));
            return;
        }

        plugin.reload();
        sender.sendMessage(plugin.getLanguageManager().getMessage("admin.config-reloaded"));
    }

    private void handleStatus(CommandSender sender, String[] args) {
        if (!sender.hasPermission("aeracraft.report.handle")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.no-permission"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("用法: /reportadmin status <举报ID>");
            return;
        }

        String reportIdStr = args[1];
        try {
            UUID reportId = UUID.fromString(reportIdStr);
            plugin.getReportService().getReportById(reportId)
                    .thenAccept(optionalReport -> {
                        if (optionalReport.isPresent()) {
                            Report report = optionalReport.get();
                            sender.sendMessage("=== 举报详情 ===");
                            sender.sendMessage("ID: " + report.getId());
                            sender.sendMessage("举报人: " + report.getReporter());
                            sender.sendMessage("被举报人: " + report.getTarget());
                            sender.sendMessage("类型: " + report.getType());
                            sender.sendMessage("状态: " + report.getStatus());
                            sender.sendMessage("创建时间: " + report.getCreatedAt());
                            if (report.getHandledBy() != null) {
                                sender.sendMessage("处理人: " + report.getHandledBy());
                            }
                            if (report.getResolutionNote() != null) {
                                sender.sendMessage("备注: " + report.getResolutionNote());
                            }
                        } else {
                            sender.sendMessage(plugin.getLanguageManager().getMessage("report.not-found"));
                        }
                    });
        } catch (IllegalArgumentException e) {
            sender.sendMessage("无效的举报ID格式");
        }
    }

    private void handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("aeracraft.report.handle")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.no-permission"));
            return;
        }

        int page = args.length > 1 ? Integer.parseInt(args[1]) : 0;
        int pageSize = 10;

        plugin.getReportService().getAllReports(page, pageSize)
                .thenAccept(reports -> {
                    sender.sendMessage("=== 举报列表 (页 " + (page + 1) + ") ===");
                    if (reports.isEmpty()) {
                        sender.sendMessage("没有更多举报");
                        return;
                    }
                    for (Report report : reports) {
                        sender.sendMessage(String.format("[%s] %s -> %s (%s) - %s",
                                report.getId().toString().substring(0, 8),
                                report.getReporter(),
                                report.getTarget(),
                                report.getType(),
                                report.getStatus()
                        ));
                    }
                });
    }

    private void handleComplete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("aeracraft.report.complete")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.no-permission"));
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.player-only"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("用法: /reportadmin complete <举报ID> [备注]");
            return;
        }

        String reportIdStr = args[1];
        String note = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "已处理";

        try {
            UUID reportId = UUID.fromString(reportIdStr);
            plugin.getReportService().updateReportStatus(
                    reportId,
                    ReportStatus.COMPLETED,
                    player.getName(),
                    note,
                    getPlayerIp(player)
            ).thenAccept(v -> {
                sender.sendMessage(plugin.getLanguageManager().getMessage("admin.report-completed"));
            });
        } catch (IllegalArgumentException e) {
            sender.sendMessage("无效的举报ID格式");
        }
    }

    private void handleReject(CommandSender sender, String[] args) {
        if (!sender.hasPermission("aeracraft.report.reject")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.no-permission"));
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.player-only"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("用法: /reportadmin reject <举报ID> [原因]");
            return;
        }

        String reportIdStr = args[1];
        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "驳回";

        try {
            UUID reportId = UUID.fromString(reportIdStr);
            plugin.getReportService().updateReportStatus(
                    reportId,
                    ReportStatus.REJECTED,
                    player.getName(),
                    reason,
                    getPlayerIp(player)
            ).thenAccept(v -> {
                sender.sendMessage(plugin.getLanguageManager().getMessage("admin.report-rejected"));
            });
        } catch (IllegalArgumentException e) {
            sender.sendMessage("无效的举报ID格式");
        }
    }

    private void handleTeleport(CommandSender sender, String[] args) {
        if (!sender.hasPermission("aeracraft.report.teleport")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.no-permission"));
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.player-only"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("用法: /reportadmin teleport <举报ID>");
            return;
        }

        sender.sendMessage("传送到举报地点功能需要GUI支持");
    }

    private void handleFreeze(CommandSender sender, String[] args) {
        if (!sender.hasPermission("aeracraft.report.freeze")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.no-permission"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("用法: /reportadmin freeze <玩家名>");
            return;
        }

        String targetName = args[1];
        sender.sendMessage("冻结玩家功能需要实现");
    }

    private String getPlayerIp(Player player) {
        return player.getAddress().getAddress().getHostAddress();
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String partial = args[0].toLowerCase();

            List<String> subCommands = Arrays.asList(
                    "reload", "status", "list", "complete", "reject", "teleport", "freeze"
            );

            completions.addAll(subCommands.stream()
                    .filter(cmd -> cmd.startsWith(partial))
                    .collect(Collectors.toList()));

            return completions;
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("status") ||
                    args[0].equalsIgnoreCase("complete") ||
                    args[0].equalsIgnoreCase("reject") ||
                    args[0].equalsIgnoreCase("teleport")) {

                return plugin.getReportRepository().findAll(100, 0)
                        .join()
                        .stream()
                        .map(r -> r.getId().toString().substring(0, 8))
                        .filter(id -> id.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}
