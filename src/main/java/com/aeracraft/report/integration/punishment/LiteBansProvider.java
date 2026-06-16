package com.aeracraft.report.integration.punishment;

import com.aeracraft.report.AeracraftReport;
import litebans.api.Events;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LiteBansProvider implements PunishmentProvider {

    private final AeracraftReport plugin;
    private final ExecutorService executor;

    public LiteBansProvider(AeracraftReport plugin) {
        this.plugin = plugin;
        this.executor = Executors.newCachedThreadPool();
    }

    @Override
    public CompletableFuture<String> ban(CommandSender operator, String targetName, String reason, long durationMinutes, String reportId) {
        return CompletableFuture.supplyAsync(() -> {
            String banReason = reason + " [举报#" + reportId + "]";
            String command = durationMinutes > 0 
                    ? "/tempban " + targetName + " " + durationMinutes + "m " + banReason
                    : "/ban " + targetName + " " + banReason;
            executeCommand(operator, command);
            return UUID.randomUUID().toString();
        }, executor);
    }

    @Override
    public CompletableFuture<String> tempBan(CommandSender operator, String targetName, String reason, long durationMinutes, String reportId) {
        return ban(operator, targetName, reason, durationMinutes, reportId);
    }

    @Override
    public CompletableFuture<String> mute(CommandSender operator, String targetName, String reason, long durationMinutes) {
        return CompletableFuture.supplyAsync(() -> {
            String command = durationMinutes > 0
                    ? "/tempmute " + targetName + " " + durationMinutes + "m " + reason
                    : "/mute " + targetName + " " + reason;
            executeCommand(operator, command);
            return UUID.randomUUID().toString();
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> kick(CommandSender operator, String targetName, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            org.bukkit.entity.Player target = Bukkit.getPlayer(targetName);
            if (target != null) {
                target.kickPlayer(reason);
                return true;
            }
            return false;
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> warn(CommandSender operator, String targetName, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            String command = "/warn " + targetName + " " + reason;
            executeCommand(operator, command);
            return true;
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> isBanned(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            return isPlayerBanned(playerName);
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> isMuted(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            return isPlayerMuted(playerName);
        }, executor);
    }

    @Override
    public CompletableFuture<Void> unban(String targetName, String reason) {
        return CompletableFuture.runAsync(() -> {
            executeCommand(null, "/unban " + targetName);
        }, executor);
    }

    @Override
    public CompletableFuture<Void> unmute(String targetName) {
        return CompletableFuture.runAsync(() -> {
            executeCommand(null, "/unmute " + targetName);
        }, executor);
    }

    @Override
    public String getProviderName() {
        return "LiteBans";
    }

    private void executeCommand(CommandSender sender, String command) {
        if (sender != null) {
            Bukkit.dispatchCommand(sender, command);
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    private boolean isPlayerBanned(String playerName) {
        org.bukkit.entity.Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            for (org.bukkit.permissions.PermissionAttachmentInfo perm : player.getEffectivePermissions()) {
                if (perm.getPermission().startsWith("litebans.banned.")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isPlayerMuted(String playerName) {
        org.bukkit.entity.Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            for (org.bukkit.permissions.PermissionAttachmentInfo perm : player.getEffectivePermissions()) {
                if (perm.getPermission().startsWith("litebans.muted.")) {
                    return true;
                }
            }
        }
        return false;
    }
}