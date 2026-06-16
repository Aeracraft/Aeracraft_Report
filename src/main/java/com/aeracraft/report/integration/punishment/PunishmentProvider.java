package com.aeracraft.report.integration.punishment;

import org.bukkit.command.CommandSender;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PunishmentProvider {

    CompletableFuture<String> ban(CommandSender operator, String targetName, String reason, long durationMinutes, String reportId);

    CompletableFuture<String> tempBan(CommandSender operator, String targetName, String reason, long durationMinutes, String reportId);

    CompletableFuture<String> mute(CommandSender operator, String targetName, String reason, long durationMinutes);

    CompletableFuture<Boolean> kick(CommandSender operator, String targetName, String reason);

    CompletableFuture<Boolean> warn(CommandSender operator, String targetName, String reason);

    CompletableFuture<Boolean> isBanned(String playerName);

    CompletableFuture<Boolean> isMuted(String playerName);

    CompletableFuture<Void> unban(String targetName, String reason);

    CompletableFuture<Void> unmute(String targetName);

    String getProviderName();
}
