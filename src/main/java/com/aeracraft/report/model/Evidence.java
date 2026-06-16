package com.aeracraft.report.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Evidence {

    private UUID reportId;
    private PlayerSnapshot reporterSnapshot;
    private PlayerSnapshot targetSnapshot;
    private LocationSnapshot location;
    private String heldItem;
    private List<EntitySnapshot> nearbyEntities;
    private List<String> recentChat;
    private String actionSummary;
    private List<BlockChange> coreProtectLogs;
    private Map<String, Object> additionalData;
    private Instant collectedAt;

    public Evidence() {
    }

    public Evidence(UUID reportId, PlayerSnapshot reporterSnapshot, PlayerSnapshot targetSnapshot,
                   LocationSnapshot location, String heldItem, List<EntitySnapshot> nearbyEntities,
                   List<String> recentChat, String actionSummary, List<BlockChange> coreProtectLogs,
                   Map<String, Object> additionalData, Instant collectedAt) {
        this.reportId = reportId;
        this.reporterSnapshot = reporterSnapshot;
        this.targetSnapshot = targetSnapshot;
        this.location = location;
        this.heldItem = heldItem;
        this.nearbyEntities = nearbyEntities;
        this.recentChat = recentChat;
        this.actionSummary = actionSummary;
        this.coreProtectLogs = coreProtectLogs;
        this.additionalData = additionalData;
        this.collectedAt = collectedAt;
    }

    public UUID getReportId() {
        return reportId;
    }

    public void setReportId(UUID reportId) {
        this.reportId = reportId;
    }

    public PlayerSnapshot getReporterSnapshot() {
        return reporterSnapshot;
    }

    public void setReporterSnapshot(PlayerSnapshot reporterSnapshot) {
        this.reporterSnapshot = reporterSnapshot;
    }

    public PlayerSnapshot getTargetSnapshot() {
        return targetSnapshot;
    }

    public void setTargetSnapshot(PlayerSnapshot targetSnapshot) {
        this.targetSnapshot = targetSnapshot;
    }

    public LocationSnapshot getLocation() {
        return location;
    }

    public void setLocation(LocationSnapshot location) {
        this.location = location;
    }

    public String getHeldItem() {
        return heldItem;
    }

    public void setHeldItem(String heldItem) {
        this.heldItem = heldItem;
    }

    public List<EntitySnapshot> getNearbyEntities() {
        return nearbyEntities;
    }

    public void setNearbyEntities(List<EntitySnapshot> nearbyEntities) {
        this.nearbyEntities = nearbyEntities;
    }

    public List<String> getRecentChat() {
        return recentChat;
    }

    public void setRecentChat(List<String> recentChat) {
        this.recentChat = recentChat;
    }

    public String getActionSummary() {
        return actionSummary;
    }

    public void setActionSummary(String actionSummary) {
        this.actionSummary = actionSummary;
    }

    public List<BlockChange> getCoreProtectLogs() {
        return coreProtectLogs;
    }

    public void setCoreProtectLogs(List<BlockChange> coreProtectLogs) {
        this.coreProtectLogs = coreProtectLogs;
    }

    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(Map<String, Object> additionalData) {
        this.additionalData = additionalData;
    }

    public Instant getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(Instant collectedAt) {
        this.collectedAt = collectedAt;
    }

    public static class PlayerSnapshot {
        private String name;
        private String uniqueId;
        private double health;
        private int foodLevel;
        private float saturation;
        private String gameMode;
        private int xpLevel;
        private float exhaustion;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUniqueId() {
            return uniqueId;
        }

        public void setUniqueId(String uniqueId) {
            this.uniqueId = uniqueId;
        }

        public double getHealth() {
            return health;
        }

        public void setHealth(double health) {
            this.health = health;
        }

        public int getFoodLevel() {
            return foodLevel;
        }

        public void setFoodLevel(int foodLevel) {
            this.foodLevel = foodLevel;
        }

        public float getSaturation() {
            return saturation;
        }

        public void setSaturation(float saturation) {
            this.saturation = saturation;
        }

        public String getGameMode() {
            return gameMode;
        }

        public void setGameMode(String gameMode) {
            this.gameMode = gameMode;
        }

        public int getXpLevel() {
            return xpLevel;
        }

        public void setXpLevel(int xpLevel) {
            this.xpLevel = xpLevel;
        }

        public float getExhaustion() {
            return exhaustion;
        }

        public void setExhaustion(float exhaustion) {
            this.exhaustion = exhaustion;
        }
    }

    public static class LocationSnapshot {
        private String world;
        private double x;
        private double y;
        private double z;
        private float yaw;
        private float pitch;

        public String getWorld() {
            return world;
        }

        public void setWorld(String world) {
            this.world = world;
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        public double getZ() {
            return z;
        }

        public void setZ(double z) {
            this.z = z;
        }

        public float getYaw() {
            return yaw;
        }

        public void setYaw(float yaw) {
            this.yaw = yaw;
        }

        public float getPitch() {
            return pitch;
        }

        public void setPitch(float pitch) {
            this.pitch = pitch;
        }
    }

    public static class EntitySnapshot {
        private String type;
        private String name;
        private double x;
        private double y;
        private double z;
        private String additionalInfo;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        public double getZ() {
            return z;
        }

        public void setZ(double z) {
            this.z = z;
        }

        public String getAdditionalInfo() {
            return additionalInfo;
        }

        public void setAdditionalInfo(String additionalInfo) {
            this.additionalInfo = additionalInfo;
        }
    }

    public static class BlockChange {
        private long timestamp;
        private String player;
        private String action;
        private String block;
        private int x;
        private int y;
        private int z;
        private String world;

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public String getPlayer() {
            return player;
        }

        public void setPlayer(String player) {
            this.player = player;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getBlock() {
            return block;
        }

        public void setBlock(String block) {
            this.block = block;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getZ() {
            return z;
        }

        public void setZ(int z) {
            this.z = z;
        }

        public String getWorld() {
            return world;
        }

        public void setWorld(String world) {
            this.world = world;
        }
    }
}
