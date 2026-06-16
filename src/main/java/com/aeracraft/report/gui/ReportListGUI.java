package com.aeracraft.report.gui;

import com.aeracraft.report.AeracraftReport;
import com.aeracraft.report.model.Report;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReportListGUI {

    private final AeracraftReport plugin;
    private static final int PAGE_SIZE = 45;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public ReportListGUI(AeracraftReport plugin) {
        this.plugin = plugin;
    }

    public Inventory createInventory(int page, List<Report> reports, Report.ReportStatus filterStatus) {
        String title = plugin.getLanguageManager().getMessage("gui.report-list-title", Map.of(
                "page", String.valueOf(page + 1)
        ));
        Inventory inventory = Bukkit.createInventory(null, 54, title);

        int startIndex = page * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, reports.size());

        for (int i = startIndex; i < endIndex; i++) {
            Report report = reports.get(i);
            int slot = i - startIndex;

            if (slot < 45) {
                ItemStack item = createReportItem(report);
                inventory.setItem(slot, item);
            }
        }

        setNavigationItems(inventory, page, reports.size(), filterStatus);

        return inventory;
    }

    private ItemStack createReportItem(Report report) {
        Material material = getMaterialForStatus(report.getStatus());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String statusName = getStatusDisplayName(report.getStatus());
        String createdAt = report.getCreatedAt().toString();

        List<String> lore = new ArrayList<>();
        lore.add("§7举报人: §f" + report.getReporter());
        lore.add("§7被举报人: §f" + report.getTarget());
        lore.add("§7类型: §f" + report.getType());
        lore.add("§7状态: " + getStatusColor(report.getStatus()) + statusName);
        lore.add("§7时间: §f" + createdAt);
        lore.add("");
        lore.add("§e点击查看详情");

        meta.setDisplayName("§6举报 #" + report.getId().toString().substring(0, 8));
        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    private Material getMaterialForStatus(Report.ReportStatus status) {
        return switch (status) {
            case PENDING -> Material.BOOK;
            case IN_PROGRESS -> Material.BLAZE_POWDER;
            case COMPLETED -> Material.EMERALD;
            case REJECTED -> Material.BARRIER;
        };
    }

    private String getStatusDisplayName(Report.ReportStatus status) {
        return switch (status) {
            case PENDING -> plugin.getLanguageManager().getMessage("status.pending");
            case IN_PROGRESS -> plugin.getLanguageManager().getMessage("status.in-progress");
            case COMPLETED -> plugin.getLanguageManager().getMessage("status.completed");
            case REJECTED -> plugin.getLanguageManager().getMessage("status.rejected");
        };
    }

    private String getStatusColor(Report.ReportStatus status) {
        return switch (status) {
            case PENDING -> "§e";
            case IN_PROGRESS -> "§b";
            case COMPLETED -> "§a";
            case REJECTED -> "§c";
        };
    }

    private void setNavigationItems(Inventory inventory, int currentPage, int totalReports, Report.ReportStatus filterStatus) {
        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName("§6举报列表");
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7当前页: " + (currentPage + 1));
        infoLore.add("§7每页显示: 45条");
        infoMeta.setLore(infoLore);
        infoItem.setItemMeta(infoMeta);
        inventory.setItem(48, infoItem);

        ItemStack prevButton = new ItemStack(Material.ARROW);
        ItemMeta prevMeta = prevButton.getItemMeta();
        prevMeta.setDisplayName("§e上一页");
        prevButton.setItemMeta(prevMeta);
        if (currentPage > 0) {
            inventory.setItem(45, prevButton);
        }

        ItemStack nextButton = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = nextButton.getItemMeta();
        nextMeta.setDisplayName("§e下一页");
        nextButton.setItemMeta(nextMeta);
        int totalPages = (int) Math.ceil((double) totalReports / PAGE_SIZE);
        if (currentPage < totalPages - 1) {
            inventory.setItem(53, nextButton);
        }

        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeButton.getItemMeta();
        closeMeta.setDisplayName("§c关闭");
        closeButton.setItemMeta(closeMeta);
        inventory.setItem(49, closeButton);

        ItemStack refreshButton = new ItemStack(Material.LAVA_BUCKET);
        ItemMeta refreshMeta = refreshButton.getItemMeta();
        refreshMeta.setDisplayName("§a刷新");
        refreshButton.setItemMeta(refreshMeta);
        inventory.setItem(50, refreshButton);

        ItemStack filterButton = new ItemStack(Material.HOPPER);
        ItemMeta filterMeta = filterButton.getItemMeta();
        filterMeta.setDisplayName("§6过滤器");
        filterButton.setItemMeta(filterMeta);
        inventory.setItem(51, filterButton);
    }

    public UUID extractReportIdFromItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }

        String displayName = item.getItemMeta().getDisplayName();
        if (displayName.contains("#")) {
            String idStr = displayName.split("#")[1];
            try {
                return UUID.fromString(idStr);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }
}
