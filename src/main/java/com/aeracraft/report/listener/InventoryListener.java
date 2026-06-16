package com.aeracraft.report.listener;

import com.aeracraft.report.AeracraftReport;
import com.aeracraft.report.model.Evidence;
import com.aeracraft.report.model.Report;
import com.aeracraft.report.model.Report.ReportStatus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class InventoryListener implements Listener {

    private final AeracraftReport plugin;

    public InventoryListener(AeracraftReport plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = event.getView().getTitle();

        if (title.contains("举报列表") || title.contains("Report List")) {
            event.setCancelled(true);
            handleReportListClick(player, event, title);
        } else if (title.contains("举报详情") || title.contains("Report Detail")) {
            event.setCancelled(true);
            handleReportDetailClick(player, event);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
    }

    private void handleReportListClick(Player player, InventoryClickEvent event, String title) {
        int slot = event.getRawSlot();

        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item == null) {
            return;
        }

        if (slot == 45) {
            navigatePage(player, title, -1);
        } else if (slot == 53) {
            navigatePage(player, title, 1);
        } else if (slot == 49) {
            player.closeInventory();
        } else if (slot == 50) {
            openReportsGUI(player, 0);
        } else if (slot < 45) {
            UUID reportId = plugin.getReportListGUI().extractReportIdFromItem(item);
            if (reportId != null) {
                openReportDetail(player, reportId);
            }
        }
    }

    private void navigatePage(Player player, String title, int direction) {
        int currentPage = 0;
        try {
            String[] parts = title.split(" ");
            for (String part : parts) {
                if (part.matches("\\d+")) {
                    currentPage = Integer.parseInt(part) - 1;
                    break;
                }
            }
        } catch (Exception ignored) {
        }

        int newPage = currentPage + direction;
        if (newPage < 0) {
            return;
        }

        openReportsGUI(player, newPage);
    }

    private void openReportsGUI(Player player, int page) {
        plugin.getReportService().getAllReports(page, 45)
                .thenAccept(reports -> {
                    Inventory inventory = plugin.getReportListGUI()
                            .createInventory(page, reports, null);
                    player.openInventory(inventory);
                })
                .exceptionally(ex -> {
                    player.sendMessage(plugin.getLanguageManager().getMessage("reports.error-loading"));
                    return null;
                });
    }

    private void openReportDetail(Player player, UUID reportId) {
        plugin.getReportService().getReportById(reportId)
                .thenCompose(optionalReport -> {
                    if (optionalReport.isEmpty()) {
                        player.sendMessage(plugin.getLanguageManager().getMessage("report.not-found"));
                        return null;
                    }
                    Report report = optionalReport.get();
                    return plugin.getReportService().getEvidence(reportId)
                            .thenApply(optionalEvidence -> {
                                Evidence evidence = optionalEvidence.orElse(null);
                                Inventory inventory = plugin.getReportDetailGUI()
                                        .createInventory(report, evidence);
                                player.openInventory(inventory);
                                return null;
                            });
                })
                .exceptionally(ex -> {
                    player.sendMessage(plugin.getLanguageManager().getMessage("report.error-loading"));
                    return null;
                });
    }

    private void handleReportDetailClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item == null) {
            return;
        }

        String action = plugin.getReportDetailGUI().getButtonAction(item);
        if (action == null) {
            return;
        }

        UUID reportId = extractReportIdFromHiddenItem(event.getInventory());
        if (reportId == null) {
            return;
        }

        switch (action) {
            case "COMPLETE":
                handleCompleteReport(player, reportId);
                break;
            case "REJECT":
                handleRejectReport(player, reportId);
                break;
            case "TELEPORT":
                handleTeleport(player, reportId);
                break;
            case "FREEZE":
                handleFreeze(player, reportId);
                break;
            case "INVENTORY":
                handleViewInventory(player, reportId);
                break;
            case "HISTORY":
                handleViewHistory(player, reportId);
                break;
            case "WARN":
                handleWarn(player, reportId);
                break;
            case "MUTE":
                handleMute(player, reportId);
                break;
            case "BAN":
                handleBan(player, reportId);
                break;
            case "TEMPBAN":
                handleTempBan(player, reportId);
                break;
            case "KICK":
                handleKick(player, reportId);
                break;
            case "COREPROTECT":
                handleCoreProtect(player, reportId);
                break;
        }
    }

    private UUID extractReportIdFromHiddenItem(Inventory inventory) {
        try {
            ItemStack hiddenItem = inventory.getItem(53);
            if (hiddenItem != null && hiddenItem.getItemMeta() != null) {
                String displayName = hiddenItem.getItemMeta().getDisplayName();
                return UUID.fromString(displayName);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void handleCompleteReport(Player player, UUID reportId) {
        plugin.getReportService().updateReportStatus(reportId, ReportStatus.COMPLETED,
                        player.getName(), "已处理", player.getAddress() != null ? player.getAddress().getAddress().toString() : "unknown")
                .thenRun(() -> {
                    player.sendMessage(plugin.getLanguageManager().getMessage("report.completed"));
                    player.closeInventory();
                })
                .exceptionally(ex -> {
                    player.sendMessage(plugin.getLanguageManager().getMessage("report.error-updating"));
                    return null;
                });
    }

    private void handleRejectReport(Player player, UUID reportId) {
        plugin.getReportService().updateReportStatus(reportId, ReportStatus.REJECTED,
                        player.getName(), "已驳回", player.getAddress() != null ? player.getAddress().getAddress().toString() : "unknown")
                .thenRun(() -> {
                    player.sendMessage(plugin.getLanguageManager().getMessage("report.rejected"));
                    player.closeInventory();
                })
                .exceptionally(ex -> {
                    player.sendMessage(plugin.getLanguageManager().getMessage("report.error-updating"));
                    return null;
                });
    }

    private void handleTeleport(Player player, UUID reportId) {
        plugin.getReportService().getEvidence(reportId)
                .thenAccept(optionalEvidence -> {
                    if (optionalEvidence.isEmpty()) {
                        player.sendMessage(plugin.getLanguageManager().getMessage("report.no-evidence"));
                        return;
                    }
                    Evidence evidence = optionalEvidence.get();
                    if (evidence.getLocation() != null) {
                        player.teleport(evidence.getLocation());
                        player.sendMessage(plugin.getLanguageManager().getMessage("report.teleported"));
                    } else {
                        player.sendMessage(plugin.getLanguageManager().getMessage("report.no-location"));
                    }
                });
    }

    private void handleFreeze(Player player, UUID reportId) {
        plugin.getReportService().getReportById(reportId)
                .thenAccept(optionalReport -> {
                    if (optionalReport.isEmpty()) {
                        return;
                    }
                    String targetName = optionalReport.get().getTarget();
                    Player target = Bukkit.getPlayer(targetName);
                    if (target != null && target.isOnline()) {
                        player.sendMessage(plugin.getLanguageManager().getMessage("report.player-frozen",
                                java.util.Map.of("player", targetName)));
                    } else {
                        player.sendMessage(plugin.getLanguageManager().getMessage("report.player-offline"));
                    }
                });
    }

    private void handleViewInventory(Player player, UUID reportId) {
        plugin.getReportService().getReportById(reportId)
                .thenAccept(optionalReport -> {
                    if (optionalReport.isEmpty()) {
                        return;
                    }
                    String targetName = optionalReport.get().getTarget();
                    Player target = Bukkit.getPlayer(targetName);
                    if (target != null && target.isOnline()) {
                        player.openInventory(target.getInventory());
                    } else {
                        player.sendMessage(plugin.getLanguageManager().getMessage("report.player-offline"));
                    }
                });
    }

    private void handleViewHistory(Player player, UUID reportId) {
        plugin.getReportService().getReportById(reportId)
                .thenAccept(optionalReport -> {
                    if (optionalReport.isEmpty()) {
                        return;
                    }
                    String targetName = optionalReport.get().getTarget();
                    plugin.getReportService().getReportsByTarget(targetName, 0, 10)
                            .thenAccept(reports -> {
                                if (reports.isEmpty()) {
                                    player.sendMessage(plugin.getLanguageManager().getMessage("report.no-history"));
                                } else {
                                    player.sendMessage("§6" + targetName + " 的历史举报记录:");
                                    for (Report r : reports) {
                                        player.sendMessage("§7- #" + r.getId().toString().substring(0, 8) + " [" + r.getStatus() + "]");
                                    }
                                }
                            });
                });
    }

    private void handleWarn(Player player, UUID reportId) {
        plugin.getReportService().getReportById(reportId)
                .thenAccept(optionalReport -> {
                    if (optionalReport.isEmpty()) {
                        return;
                    }
                    String targetName = optionalReport.get().getTarget();
                    plugin.getPunishmentProvider().warn(player, targetName, "举报处理 - 警告")
                            .thenAccept(success -> {
                                if (success) {
                                    player.sendMessage(plugin.getLanguageManager().getMessage("report.warn-success"));
                                } else {
                                    player.sendMessage(plugin.getLanguageManager().getMessage("report.warn-failed"));
                                }
                            });
                });
    }

    private void handleMute(Player player, UUID reportId) {
        plugin.getReportService().getReportById(reportId)
                .thenAccept(optionalReport -> {
                    if (optionalReport.isEmpty()) {
                        return;
                    }
                    String targetName = optionalReport.get().getTarget();
                    plugin.getPunishmentProvider().mute(player, targetName, "举报处理 - 禁言", 60)
                            .thenAccept(success -> {
                                if (success) {
                                    player.sendMessage(plugin.getLanguageManager().getMessage("report.mute-success"));
                                } else {
                                    player.sendMessage(plugin.getLanguageManager().getMessage("report.mute-failed"));
                                }
                            });
                });
    }

    private void handleBan(Player player, UUID reportId) {
        plugin.getReportService().getReportById(reportId)
                .thenAccept(optionalReport -> {
                    if (optionalReport.isEmpty()) {
                        return;
                    }
                    String targetName = optionalReport.get().getTarget();
                    plugin.getPunishmentProvider().ban(player, targetName, "举报处理 - 永久封禁", 0, reportId.toString())
                            .thenAccept(banId -> {
                                if (banId != null) {
                                    player.sendMessage(plugin.getLanguageManager().getMessage("report.ban-success"));
                                    plugin.getReportService().linkBanToReport(reportId, banId, player.getName(),
                                            player.getAddress() != null ? player.getAddress().getAddress().toString() : "unknown");
                                } else {
                                    player.sendMessage(plugin.getLanguageManager().getMessage("report.ban-failed"));
                                }
                            });
                });
    }

    private void handleTempBan(Player player, UUID reportId) {
        plugin.getReportService().getReportById(reportId)
                .thenAccept(optionalReport -> {
                    if (optionalReport.isEmpty()) {
                        return;
                    }
                    String targetName = optionalReport.get().getTarget();
                    plugin.getPunishmentProvider().tempBan(player, targetName, "举报处理 - 临时封禁", 1440, reportId.toString())
                            .thenAccept(banId -> {
                                if (banId != null) {
                                    player.sendMessage(plugin.getLanguageManager().getMessage("report.tempban-success"));
                                    plugin.getReportService().linkBanToReport(reportId, banId, player.getName(),
                                            player.getAddress() != null ? player.getAddress().getAddress().toString() : "unknown");
                                } else {
                                    player.sendMessage(plugin.getLanguageManager().getMessage("report.tempban-failed"));
                                }
                            });
                });
    }

    private void handleKick(Player player, UUID reportId) {
        plugin.getReportService().getReportById(reportId)
                .thenAccept(optionalReport -> {
                    if (optionalReport.isEmpty()) {
                        return;
                    }
                    String targetName = optionalReport.get().getTarget();
                    plugin.getPunishmentProvider().kick(player, targetName, "举报处理 - 踢出")
                            .thenAccept(success -> {
                                if (success) {
                                    player.sendMessage(plugin.getLanguageManager().getMessage("report.kick-success"));
                                } else {
                                    player.sendMessage(plugin.getLanguageManager().getMessage("report.kick-failed"));
                                }
                            });
                });
    }

    private void handleCoreProtect(Player player, UUID reportId) {
        plugin.getReportService().getEvidence(reportId)
                .thenAccept(optionalEvidence -> {
                    if (optionalEvidence.isEmpty()) {
                        return;
                    }
                    Evidence evidence = optionalEvidence.get();
                    if (evidence.getCoreProtectLogs() != null && !evidence.getCoreProtectLogs().isEmpty()) {
                        player.sendMessage("§6CoreProtect 记录:");
                        for (String log : evidence.getCoreProtectLogs()) {
                            player.sendMessage("§7- " + log);
                        }
                    } else {
                        player.sendMessage(plugin.getLanguageManager().getMessage("report.no-coreprotect"));
                    }
                });
    }
}
