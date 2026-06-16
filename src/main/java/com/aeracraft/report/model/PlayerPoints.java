package com.aeracraft.report.model;

import java.time.Instant;
import java.util.UUID;

public class PlayerPoints {

    private UUID id;
    private String playerName;
    private UUID playerUniqueId;
    private int points;
    private int totalValidReports;
    private int totalReports;
    private Instant lastDecayAt;
    private Instant updatedAt;

    public PlayerPoints() {
    }

    public PlayerPoints(UUID id, String playerName, UUID playerUniqueId, int points,
                        int totalValidReports, int totalReports, Instant lastDecayAt, Instant updatedAt) {
        this.id = id;
        this.playerName = playerName;
        this.playerUniqueId = playerUniqueId;
        this.points = points;
        this.totalValidReports = totalValidReports;
        this.totalReports = totalReports;
        this.lastDecayAt = lastDecayAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public UUID getPlayerUniqueId() {
        return playerUniqueId;
    }

    public void setPlayerUniqueId(UUID playerUniqueId) {
        this.playerUniqueId = playerUniqueId;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getTotalValidReports() {
        return totalValidReports;
    }

    public void setTotalValidReports(int totalValidReports) {
        this.totalValidReports = totalValidReports;
    }

    public int getTotalReports() {
        return totalReports;
    }

    public void setTotalReports(int totalReports) {
        this.totalReports = totalReports;
    }

    public Instant getLastDecayAt() {
        return lastDecayAt;
    }

    public void setLastDecayAt(Instant lastDecayAt) {
        this.lastDecayAt = lastDecayAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void addPoints(int points) {
        this.points += points;
        this.totalValidReports++;
    }

    public void removePoints(int points) {
        this.points = Math.max(0, this.points - points);
    }

    public void applyDecay(int decayAmount) {
        this.points = Math.max(0, this.points - decayAmount);
        this.lastDecayAt = Instant.now();
    }
}
