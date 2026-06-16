package com.aeracraft.report.listener;

import com.aeracraft.report.AeracraftReport;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerListener implements Listener {

    private final AeracraftReport plugin;
    private final Map<UUID, Long> frozenPlayers;
    private final Map<UUID, Long> lastActionTime;

    public PlayerListener(AeracraftReport plugin) {
        this.plugin = plugin;
        this.frozenPlayers = new ConcurrentHashMap<>();
        this.lastActionTime = new ConcurrentHashMap<>();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("aeracraft.report.notify")) {
            notifyPendingReports(player);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        frozenPlayers.remove(player.getUniqueId());
        lastActionTime.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();

        if (!player.isSneaking()) {
            return;
        }

        if (!player.hasPermission("aeracraft.report.use")) {
            return;
        }

        if (!(event.getRightClicked() instanceof Player target)) {
            return;
        }

        if (target.equals(player)) {
            return;
        }

        event.setCancelled(true);

        String reason = "未指定";
        String type = "OTHER";

        plugin.getReportService().createReport(player, target, type, reason)
                .thenAccept(report -> {
                    player.sendMessage(plugin.getLanguageManager().getMessage("report.success", Map.of(
                            "id", report.getId().toString()
                    )));
                });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!isFrozen(player)) {
            return;
        }

        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                event.getFrom().getBlockY() != event.getTo().getBlockY() ||
                event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {

            event.setTo(event.getFrom());
        }
    }

    public void freezePlayer(Player player, long durationMillis) {
        frozenPlayers.put(player.getUniqueId(), System.currentTimeMillis() + durationMillis);
        player.sendMessage(plugin.getLanguageManager().getMessage("freeze.message"));
    }

    public void unfreezePlayer(Player player) {
        frozenPlayers.remove(player.getUniqueId());
        player.sendMessage(plugin.getLanguageManager().getMessage("unfreeze.message"));
    }

    public boolean isFrozen(Player player) {
        Long expiry = frozenPlayers.get(player.getUniqueId());
        if (expiry == null) {
            return false;
        }

        if (System.currentTimeMillis() > expiry) {
            frozenPlayers.remove(player.getUniqueId());
            return false;
        }

        return true;
    }

    public void recordAction(Player player) {
        lastActionTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public long getLastActionTime(Player player) {
        return lastActionTime.getOrDefault(player.getUniqueId(), 0L);
    }

    private void notifyPendingReports(Player player) {
        plugin.getReportRepository().countByStatus(com.aeracraft.report.model.Report.ReportStatus.PENDING)
                .thenAccept(count -> {
                    if (count > 0) {
                        player.sendMessage(plugin.getLanguageManager().getMessage("report.pending-notify", Map.of(
                                "count", String.valueOf(count)
                        )));
                    }
                });
    }
}
