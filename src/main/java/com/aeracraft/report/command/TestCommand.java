package com.aeracraft.report.command;

import com.aeracraft.report.AeracraftReport;
import com.aeracraft.report.model.Evidence;
import com.aeracraft.report.model.Report;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TestCommand implements CommandExecutor, TabCompleter {

    private final AeracraftReport plugin;

    public TestCommand(AeracraftReport plugin) {
        this.plugin = plugin;
        plugin.getCommand("testreport").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("此命令只能由玩家执行");
            return true;
        }

        if (!player.hasPermission("aeracraft.report.admin")) {
            player.sendMessage("您没有权限执行此命令");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("用法: /testreport <count> - 创建测试举报数据");
            return true;
        }

        try {
            int count = Integer.parseInt(args[0]);
            createTestReports(player, count);
            player.sendMessage("已创建 " + count + " 条测试举报数据");
        } catch (NumberFormatException e) {
            player.sendMessage("请输入有效的数字");
        }

        return true;
    }

    private void createTestReports(Player player, int count) {
        String[] types = {"CHEATING", "HARASSMENT", "GRIEFING", "SPAM", "OTHER"};
        String[] targets = {"Player1", "Player2", "Player3", "Player4", "Player5"};
        Report.ReportStatus[] statuses = {
            Report.ReportStatus.PENDING,
            Report.ReportStatus.IN_PROGRESS,
            Report.ReportStatus.COMPLETED,
            Report.ReportStatus.REJECTED
        };

        for (int i = 0; i < count; i++) {
            UUID reportId = UUID.randomUUID();
            Instant now = Instant.now();

            Report report = new Report(
                    reportId,
                    player.getName(),
                    targets[i % targets.length],
                    types[i % types.length],
                    statuses[i % statuses.length],
                    now.minusSeconds(i * 3600L), // 每个举报间隔1小时
                    now,
                    "test_hash_" + i,
                    null,
                    plugin.getConfigManager().getServerName(),
                    i % 3 == 0 ? player.getName() : null, // 每3个举报有一个被处理
                    i % 3 == 0 ? "测试处理备注" : null
            );

            Evidence evidence = new Evidence();
            evidence.setReportId(reportId);
            evidence.setCollectedAt(now);

            plugin.getReportRepository().createReport(report, evidence).join();
        }

        plugin.getLogger().info("测试数据创建完成，共 " + count + " 条");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("5");
            completions.add("10");
            completions.add("20");
        }
        return completions;
    }
}