package com.aeracraft.report.gui;

import com.aeracraft.report.AeracraftReport;
import com.aeracraft.report.model.Evidence;
import com.aeracraft.report.model.Report;
import com.aeracraft.report.model.Report.ReportStatus;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReportDetailGUI {

    private final AeracraftReport plugin;
    private final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ReportDetailGUI(AeracraftReport plugin) {
        this.plugin = plugin;
    }

    public Inventory createInventory(Report report, Evidence evidence) {
        String title = plugin.getLanguageManager().getMessage("gui.report-detail-title", Map.of(
                "id", report.getId().toString().substring(0, 8)
        ));
        Inventory inventory = Bukkit.createInventory(null, 54, title);

        setReportInfo(inventory, report);
        setActionButtons(inventory, report);
        setEvidenceInfo(inventory, evidence);

        return inventory;
    }

    private void setReportInfo(Inventory inventory, Report report) {
        ItemStack headItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta headMeta = headItem.getItemMeta();

        List<String> headLore = new ArrayList<>();
        headLore.add("§7举报人: §f" + report.getReporter());
        headLore.add("§7被举报人: §f" + report.getTarget());
        headLore.add("§7类型: §f" + report.getType());
        headLore.add("§7服务器: §f" + report.getServerName());
        headMeta.setLore(headLore);

        headMeta.setDisplayName("§6举报信息");
        headItem.setItemMeta(headMeta);
        inventory.setItem(0, headItem);

        ItemStack statusItem = new ItemStack(Material.BOOK);
        ItemMeta statusMeta = statusItem.getItemMeta();
        statusMeta.setDisplayName("§6状态: " + getStatusColor(report.getStatus()) + getStatusName(report.getStatus()));
        List<String> statusLore = new ArrayList<>();
        statusLore.add("§7创建时间: §f" + DATE_FORMAT.format(report.getCreatedAt().atZone(java.time.ZoneId.systemDefault())));
        statusLore.add("§7更新时间: §f" + DATE_FORMAT.format(report.getUpdatedAt().atZone(java.time.ZoneId.systemDefault())));
        if (report.getHandledBy() != null) {
            statusLore.add("§7处理人: §f" + report.getHandledBy());
        }
        if (report.getResolutionNote() != null) {
            statusLore.add("§7备注: §f" + report.getResolutionNote());
        }
        statusMeta.setLore(statusLore);
        statusItem.setItemMeta(statusMeta);
        inventory.setItem(1, statusItem);

        ItemStack idItem = new ItemStack(Material.NAME_TAG);
        ItemMeta idMeta = idItem.getItemMeta();
        idMeta.setDisplayName("§6举报ID");
        List<String> idLore = new ArrayList<>();
        idLore.add("§7" + report.getId().toString());
        idMeta.setLore(idLore);
        idItem.setItemMeta(idMeta);
        inventory.setItem(2, idItem);
    }

    private void setActionButtons(Inventory inventory, Report report) {
        int slot = 9;

        if (report.getStatus() == ReportStatus.PENDING || report.getStatus() == ReportStatus.IN_PROGRESS) {
            ItemStack completeBtn = createButton(Material.EMERALD, "§a完成举报",
                    List.of("§7将此举报标记为已完成"));
            inventory.setItem(slot++, completeBtn);

            ItemStack rejectBtn = createButton(Material.BARRIER, "§c驳回举报",
                    List.of("§7驳回此举报"));
            inventory.setItem(slot++, rejectBtn);
        }

        ItemStack teleportBtn = createButton(Material.ENDER_PEARL, "§e传送到位置",
                List.of("§7传送到被举报人位置"));
        inventory.setItem(slot++, teleportBtn);

        ItemStack freezeBtn = createButton(Material.ICE, "§b冻结玩家",
                List.of("§7冻结被举报玩家"));
        inventory.setItem(slot++, freezeBtn);

        ItemStack inventoryBtn = createButton(Material.CHEST, "§e查看背包",
                List.of("§7查看被举报人背包"));
        inventory.setItem(slot++, inventoryBtn);

        ItemStack historyBtn = createButton(Material.BOOK, "§e历史记录",
                List.of("§7查看被举报人的历史举报"));
        inventory.setItem(slot++, historyBtn);

        ItemStack warnBtn = createButton(Material.BLAZE_POWDER, "§c警告",
                List.of("§7向被举报人发送警告"));
        inventory.setItem(slot++, warnBtn);

        ItemStack muteBtn = createButton(Material.JUKEBOX, "§c禁言",
                List.of("§7禁言被举报玩家"));
        inventory.setItem(slot++, muteBtn);

        if (report.getStatus() == ReportStatus.PENDING || report.getStatus() == ReportStatus.IN_PROGRESS) {
            ItemStack banBtn = createButton(Material.NETHERITE_BLOCK, "§4封禁",
                    List.of("§7永久封禁被举报玩家"));
            inventory.setItem(slot++, banBtn);

            ItemStack tempBanBtn = createButton(Material.IRON_BLOCK, "§6临时封禁",
                    List.of("§7临时封禁被举报玩家"));
            inventory.setItem(slot++, tempBanBtn);
        }

        ItemStack kickBtn = createButton(Material.FIRE_CHARGE, "§c踢出",
                List.of("§7踢出被举报玩家"));
        inventory.setItem(slot++, kickBtn);
    }

    private void setEvidenceInfo(Inventory inventory, Evidence evidence) {
        if (evidence == null) {
            return;
        }

        int slot = 27;

        ItemStack locationBtn = new ItemStack(Material.COMPASS);
        ItemMeta locationMeta = locationBtn.getItemMeta();
        locationMeta.setDisplayName("§6位置信息");

        List<String> locationLore = new ArrayList<>();
        if (evidence.getLocation() != null) {
            locationLore.add("§7世界: §f" + evidence.getLocation().getWorld());
            locationLore.add("§7坐标: §f" +
                    (int) evidence.getLocation().getX() + ", " +
                    (int) evidence.getLocation().getY() + ", " +
                    (int) evidence.getLocation().getZ());
        }
        locationMeta.setLore(locationLore);
        locationBtn.setItemMeta(locationMeta);
        inventory.setItem(slot++, locationBtn);

        ItemStack heldItemBtn = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta heldItemMeta = heldItemBtn.getItemMeta();
        heldItemMeta.setDisplayName("§6手持物品");
        List<String> heldItemLore = new ArrayList<>();
        heldItemLore.add("§7物品: §f" + (evidence.getHeldItem() != null ? evidence.getHeldItem() : "无"));
        heldItemMeta.setLore(heldItemLore);
        heldItemBtn.setItemMeta(heldItemMeta);
        inventory.setItem(slot++, heldItemBtn);

        if (plugin.getCoreProtectIntegration().isEnabled() &&
                evidence.getCoreProtectLogs() != null &&
                !evidence.getCoreProtectLogs().isEmpty()) {

            ItemStack coreProtectBtn = new ItemStack(Material.BRICKS);
            ItemMeta coreProtectMeta = coreProtectBtn.getItemMeta();
            coreProtectMeta.setDisplayName("§6CoreProtect 记录");
            List<String> coreProtectLore = new ArrayList<>();
            coreProtectLore.add("§7方块操作记录数: §f" + evidence.getCoreProtectLogs().size());
            coreProtectLore.add("§e点击查看详细记录");
            coreProtectMeta.setLore(coreProtectLore);
            coreProtectBtn.setItemMeta(coreProtectMeta);
            inventory.setItem(slot++, coreProtectBtn);
        } else {
            ItemStack coreProtectBtn = createButton(Material.BRICKS, "§7CoreProtect 记录",
                    List.of("§7无记录或 CoreProtect 未启用"));
            inventory.setItem(slot++, coreProtectBtn);
        }

        if (evidence.getTargetSnapshot() != null) {
            ItemStack targetInfoBtn = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta targetInfoMeta = targetInfoBtn.getItemMeta();
            targetInfoMeta.setDisplayName("§6被举报人状态");

            List<String> targetLore = new ArrayList<>();
            targetLore.add("§7生命值: §f" + evidence.getTargetSnapshot().getHealth());
            targetLore.add("§7饥饿值: §f" + evidence.getTargetSnapshot().getFoodLevel());
            targetLore.add("§7游戏模式: §f" + evidence.getTargetSnapshot().getGameMode());
            targetInfoMeta.setLore(targetLore);
            targetInfoBtn.setItemMeta(targetInfoMeta);
            inventory.setItem(slot++, targetInfoBtn);
        }
    }

    private ItemStack createButton(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String getStatusName(ReportStatus status) {
        return switch (status) {
            case PENDING -> plugin.getLanguageManager().getMessage("status.pending");
            case IN_PROGRESS -> plugin.getLanguageManager().getMessage("status.in-progress");
            case COMPLETED -> plugin.getLanguageManager().getMessage("status.completed");
            case REJECTED -> plugin.getLanguageManager().getMessage("status.rejected");
        };
    }

    private String getStatusColor(ReportStatus status) {
        return switch (status) {
            case PENDING -> "§e";
            case IN_PROGRESS -> "§b";
            case COMPLETED -> "§a";
            case REJECTED -> "§c";
        };
    }

    public String getButtonAction(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }

        String name = item.getItemMeta().getDisplayName();

        if (name.contains("完成举报")) return "COMPLETE";
        if (name.contains("驳回举报")) return "REJECT";
        if (name.contains("传送")) return "TELEPORT";
        if (name.contains("冻结")) return "FREEZE";
        if (name.contains("背包")) return "INVENTORY";
        if (name.contains("历史")) return "HISTORY";
        if (name.contains("警告")) return "WARN";
        if (name.contains("禁言")) return "MUTE";
        if (name.contains("封禁")) return "BAN";
        if (name.contains("临时封禁")) return "TEMPBAN";
        if (name.contains("踢出")) return "KICK";
        if (name.contains("CoreProtect")) return "COREPROTECT";
        if (name.contains("位置信息")) return "LOCATION";
        if (name.contains("手持物品")) return "HELD_ITEM";
        if (name.contains("被举报人状态")) return "TARGET_INFO";

        return null;
    }
}
