package com.aeracraft.report.core;

import com.aeracraft.report.AeracraftReport;
import com.aeracraft.report.model.Evidence;
import com.aeracraft.report.model.Evidence.*;
import com.google.gson.Gson;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class EvidenceCollector {

    private final AeracraftReport plugin;
    private final Gson gson;
    private static final int NEARBY_RADIUS = 10;
    private static final int CHAT_HISTORY_LINES = 60;

    public EvidenceCollector(AeracraftReport plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
    }

    public Evidence collectEvidence(Player reporter, Player target, String type, String description) {
        UUID reportId = UUID.randomUUID();

        PlayerSnapshot reporterSnapshot = capturePlayerSnapshot(reporter);
        PlayerSnapshot targetSnapshot = capturePlayerSnapshot(target);
        LocationSnapshot locationSnapshot = captureLocationSnapshot(target.getLocation());
        String heldItem = captureHeldItem(target);
        List<EntitySnapshot> nearbyEntities = captureNearbyEntities(target);
        List<String> recentChat = captureRecentChat(target);
        String actionSummary = generateActionSummary(reporter, target, description);

        List<BlockChange> coreProtectLogs = new ArrayList<>();
        if (shouldQueryCoreProtect(type)) {
            coreProtectLogs = queryCoreProtect(target);
        }

        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("description", description);
        additionalData.put("reportType", type);
        additionalData.put("serverName", plugin.getConfigManager().getServerName());

        return new Evidence(
                reportId,
                reporterSnapshot,
                targetSnapshot,
                locationSnapshot,
                heldItem,
                nearbyEntities,
                recentChat,
                actionSummary,
                coreProtectLogs,
                additionalData,
                Instant.now()
        );
    }

    public Evidence collectOfflineEvidence(String reporterName, String targetName, String type, String description) {
        UUID reportId = UUID.randomUUID();

        PlayerSnapshot reporterSnapshot = new PlayerSnapshot();
        reporterSnapshot.setName(reporterName);
        reporterSnapshot.setUniqueId("OFFLINE");

        PlayerSnapshot targetSnapshot = new PlayerSnapshot();
        targetSnapshot.setName(targetName);
        targetSnapshot.setUniqueId("OFFLINE");

        LocationSnapshot locationSnapshot = new LocationSnapshot();
        locationSnapshot.setWorld("UNKNOWN");

        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("description", description);
        additionalData.put("reportType", type);
        additionalData.put("serverName", plugin.getConfigManager().getServerName());
        additionalData.put("offline", true);

        return new Evidence(
                reportId,
                reporterSnapshot,
                targetSnapshot,
                locationSnapshot,
                "UNKNOWN",
                new ArrayList<>(),
                new ArrayList<>(),
                "Offline report: " + description,
                new ArrayList<>(),
                additionalData,
                Instant.now()
        );
    }

    private PlayerSnapshot capturePlayerSnapshot(Player player) {
        PlayerSnapshot snapshot = new PlayerSnapshot();
        snapshot.setName(player.getName());
        snapshot.setUniqueId(player.getUniqueId().toString());
        snapshot.setHealth(player.getHealth());
        snapshot.setFoodLevel(player.getFoodLevel());
        snapshot.setSaturation(player.getSaturation());
        snapshot.setGameMode(player.getGameMode().name());
        snapshot.setXpLevel(player.getLevel());
        snapshot.setExhaustion(player.getExhaustion());
        return snapshot;
    }

    private LocationSnapshot captureLocationSnapshot(Location location) {
        LocationSnapshot snapshot = new LocationSnapshot();
        snapshot.setWorld(location.getWorld().getName());
        snapshot.setX(location.getX());
        snapshot.setY(location.getY());
        snapshot.setZ(location.getZ());
        snapshot.setYaw(location.getYaw());
        snapshot.setPitch(location.getPitch());
        return snapshot;
    }

    private String captureHeldItem(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            item = player.getInventory().getItemInOffHand();
        }
        if (item != null && item.getType() != Material.AIR) {
            return item.getType().name() + " x" + item.getAmount();
        }
        return "AIR";
    }

    private List<EntitySnapshot> captureNearbyEntities(Player target) {
        List<EntitySnapshot> entities = new ArrayList<>();

        target.getNearbyEntities(NEARBY_RADIUS, NEARBY_RADIUS, NEARBY_RADIUS)
                .forEach(entity -> {
                    EntitySnapshot snapshot = new EntitySnapshot();
                    snapshot.setType(entity.getType().name());
                    snapshot.setName(entity.getName());
                    snapshot.setX(entity.getLocation().getX());
                    snapshot.setY(entity.getLocation().getY());
                    snapshot.setZ(entity.getLocation().getZ());
                    snapshot.setAdditionalInfo(getEntityAdditionalInfo(entity));
                    entities.add(snapshot);
                });

        return entities;
    }

    private String getEntityAdditionalInfo(Entity entity) {
        if (entity instanceof Player player) {
            return "Health: " + player.getHealth() + "/" + player.getMaxHealth();
        }
        return "";
    }

    private List<String> captureRecentChat(Player target) {
        return new ArrayList<>();
    }

    private String generateActionSummary(Player reporter, Player target, String description) {
        StringBuilder summary = new StringBuilder();
        summary.append("Reporter: ").append(reporter.getName()).append("\n");
        summary.append("Target: ").append(target.getName()).append("\n");
        summary.append("Description: ").append(description).append("\n");
        summary.append("Target Location: ").append(formatLocation(target.getLocation())).append("\n");
        summary.append("Target GameMode: ").append(target.getGameMode().name()).append("\n");
        return summary.toString();
    }

    private String formatLocation(Location location) {
        return String.format("%s (%.1f, %.1f, %.1f)",
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ());
    }

    private boolean shouldQueryCoreProtect(String type) {
        return type.equalsIgnoreCase("GRIEFING")
                && plugin.getCoreProtectIntegration().isEnabled();
    }

    private List<BlockChange> queryCoreProtect(Player target) {
        if (!plugin.getCoreProtectIntegration().isEnabled()) {
            return new ArrayList<>();
        }

        return plugin.getCoreProtectIntegration().lookup(
                target.getWorld().getName(),
                target.getLocation().getBlockX(),
                target.getLocation().getBlockY(),
                target.getLocation().getBlockZ(),
                plugin.getConfigManager().getCoreProtectQueryRadius(),
                plugin.getConfigManager().getCoreProtectQueryMinutes()
        );
    }

    public String evidenceToJson(Evidence evidence) {
        return gson.toJson(evidence);
    }

    public Evidence jsonToEvidence(String json) {
        return gson.fromJson(json, Evidence.class);
    }
}
