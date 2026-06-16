package com.aeracraft.report.integration.punishment;

import com.aeracraft.report.AeracraftReport;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

public class FallbackProvider implements PunishmentProvider {

    private final AeracraftReport plugin;
    private final Map<String, BanRecord> bans;
    private final Map<String, MuteRecord> mutes;

    public FallbackProvider(AeracraftReport plugin) {
        this.plugin = plugin;
        this.bans = new ConcurrentHashMap<>();
        this.mutes = new ConcurrentHashMap<>();

        plugin.getLogger().warning("===========================================");
        plugin.getLogger().warning("警告：使用内存存储作为处罚提供程序！");
        plugin.getLogger().warning("此模式仅适用于测试环境！");
        plugin.getLogger().warning("在生产环境中请安装 LiteBans");
        plugin.getLogger().warning("===========================================");
    }

    @Override
    public CompletableFuture<String> ban(CommandSender operator, String targetName, String reason, long durationMinutes, String reportId) {
        return CompletableFuture.supplyAsync(() -> {
            String banId = UUID.randomUUID().toString();
            long expiryTime = durationMinutes > 0
                    ? System.currentTimeMillis() + (durationMinutes * 60 * 1000)
                    : 0;

            BanRecord record = new BanRecord(banId, targetName, reason, operator != null ? operator.getName() : "Console", expiryTime);
            bans.put(targetName.toLowerCase(), record);

            Player target = Bukkit.getPlayer(targetName);
            if (target != null) {
                target.kickPlayer(reason + "\n[举报#" + reportId + "]");
            }

            return banId;
        });
    }

    @Override
    public CompletableFuture<String> tempBan(CommandSender operator, String targetName, String reason, long durationMinutes, String reportId) {
        return ban(operator, targetName, reason + " [举报#" + reportId + "]", durationMinutes, reportId);
    }

    @Override
    public CompletableFuture<String> mute(CommandSender operator, String targetName, String reason, long durationMinutes) {
        return CompletableFuture.supplyAsync(() -> {
            String muteId = UUID.randomUUID().toString();
            long expiryTime = durationMinutes > 0
                    ? System.currentTimeMillis() + (durationMinutes * 60 * 1000)
                    : 0;

            MuteRecord record = new MuteRecord(muteId, targetName, reason, operator != null ? operator.getName() : "Console", expiryTime);
            mutes.put(targetName.toLowerCase(), record);

            return muteId;
        });
    }

    @Override
    public CompletableFuture<Boolean> kick(CommandSender operator, String targetName, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            Player target = Bukkit.getPlayer(targetName);
            if (target != null) {
                target.kickPlayer(reason);
                return true;
            }
            return false;
        });
    }

    @Override
    public CompletableFuture<Boolean> warn(CommandSender operator, String targetName, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            Player target = Bukkit.getPlayer(targetName);
            if (target != null) {
                target.sendMessage(plugin.getLanguageManager().getMessage("warn.received", Map.of(
                        "reason", reason,
                        "operator", operator != null ? operator.getName() : "Console"
                )));
                return true;
            }
            return false;
        });
    }

    @Override
    public CompletableFuture<Boolean> isBanned(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            BanRecord record = bans.get(playerName.toLowerCase());
            if (record == null) {
                return false;
            }
            if (record.expiryTime > 0 && record.expiryTime < System.currentTimeMillis()) {
                bans.remove(playerName.toLowerCase());
                return false;
            }
            return true;
        });
    }

    @Override
    public CompletableFuture<Boolean> isMuted(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            MuteRecord record = mutes.get(playerName.toLowerCase());
            if (record == null) {
                return false;
            }
            if (record.expiryTime > 0 && record.expiryTime < System.currentTimeMillis()) {
                mutes.remove(playerName.toLowerCase());
                return false;
            }
            return true;
        });
    }

    @Override
    public CompletableFuture<Void> unban(String targetName, String reason) {
        return CompletableFuture.runAsync(() -> {
            bans.remove(targetName.toLowerCase());
        });
    }

    @Override
    public CompletableFuture<Void> unmute(String targetName) {
        return CompletableFuture.runAsync(() -> {
            mutes.remove(targetName.toLowerCase());
        });
    }

    @Override
    public String getProviderName() {
        return "Fallback (Memory)";
    }

    private static class BanRecord {
        final String banId;
        final String playerName;
        final String reason;
        final String operator;
        final long expiryTime;

        BanRecord(String banId, String playerName, String reason, String operator, long expiryTime) {
            this.banId = banId;
            this.playerName = playerName;
            this.reason = reason;
            this.operator = operator;
            this.expiryTime = expiryTime;
        }
    }

    private static class MuteRecord {
        final String muteId;
        final String playerName;
        final String reason;
        final String operator;
        final long expiryTime;

        MuteRecord(String muteId, String playerName, String reason, String operator, long expiryTime) {
            this.muteId = muteId;
            this.playerName = playerName;
            this.reason = reason;
            this.operator = operator;
            this.expiryTime = expiryTime;
        }
    }
}