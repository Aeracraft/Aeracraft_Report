package com.aeracraft.report.listener;

import com.aeracraft.report.AeracraftReport;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

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
            handleReportListClick(player, event);
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

    private void handleReportListClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        if (event.getCurrentItem() == null) {
            return;
        }

        player.sendMessage("点击了槽位: " + slot);
    }

    private void handleReportDetailClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        if (event.getCurrentItem() == null) {
            return;
        }

        player.sendMessage("点击了槽位: " + slot);
    }
}
