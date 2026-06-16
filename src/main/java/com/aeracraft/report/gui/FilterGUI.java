package com.aeracraft.report.gui;

import com.aeracraft.report.AeracraftReport;
import com.aeracraft.report.model.Report;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FilterGUI {

    private final AeracraftReport plugin;
    private static final int INVENTORY_SIZE = 27;

    public FilterGUI(AeracraftReport plugin) {
        this.plugin = plugin;
    }

    public Inventory createInventory() {
        String title = plugin.getLanguageManager().getMessage("gui.filter-title");
        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, title);

        setFilterItems(inventory);
        setNavigationItems(inventory);

        return inventory;
    }

    private void setFilterItems(Inventory inventory) {
        ItemStack allReports = createFilterItem(Material.BOOK, "§6全部举报", 
                List.of("§7显示所有举报", "§e点击查看"), "ALL");
        inventory.setItem(11, allReports);

        ItemStack pendingReports = createFilterItem(Material.BLAZE_POWDER, "§e待处理举报", 
                List.of("§7仅显示待处理的举报", "§e点击查看"), "PENDING");
        inventory.setItem(12, pendingReports);

        ItemStack inProgressReports = createFilterItem(Material.DIAMOND_PICKAXE, "§b处理中举报", 
                List.of("§7仅显示处理中的举报", "§e点击查看"), "IN_PROGRESS");
        inventory.setItem(13, inProgressReports);

        ItemStack completedReports = createFilterItem(Material.EMERALD, "§a已完成举报", 
                List.of("§7仅显示已完成的举报", "§e点击查看"), "COMPLETED");
        inventory.setItem(14, completedReports);

        ItemStack rejectedReports = createFilterItem(Material.BARRIER, "§c已驳回举报", 
                List.of("§7仅显示已驳回的举报", "§e点击查看"), "REJECTED");
        inventory.setItem(15, rejectedReports);
    }

    private void setNavigationItems(Inventory inventory) {
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName("§c关闭");
        closeItem.setItemMeta(closeMeta);
        inventory.setItem(22, closeItem);
    }

    private ItemStack createFilterItem(Material material, String name, List<String> lore, String filterType) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public Report.ReportStatus getFilterFromItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }

        String displayName = item.getItemMeta().getDisplayName();
        if (displayName.contains("全部举报")) return null;
        if (displayName.contains("待处理")) return Report.ReportStatus.PENDING;
        if (displayName.contains("处理中")) return Report.ReportStatus.IN_PROGRESS;
        if (displayName.contains("已完成")) return Report.ReportStatus.COMPLETED;
        if (displayName.contains("已驳回")) return Report.ReportStatus.REJECTED;

        return null;
    }
}