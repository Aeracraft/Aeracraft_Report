package com.aeracraft.report.util;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class GUIUtils {

    public static void handleAsyncOperation(Player player, CompletableFuture<?> future, 
            String successMessage, String errorMessage, String noPermissionMessage) {
        future.thenAccept(result -> {
            if (successMessage != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', successMessage));
            }
        }).exceptionally(ex -> {
            String message = errorMessage != null ? errorMessage : "操作失败";
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            return null;
        });
    }

    public static String formatMessage(String template, Map<String, String> placeholders) {
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return ChatColor.translateAlternateColorCodes('&', result);
    }

    public static boolean hasPermission(Player player, String permission) {
        return player.hasPermission(permission);
    }

    public static void sendNoPermissionMessage(Player player, String permission) {
        player.sendMessage(ChatColor.RED + "您没有权限执行此操作: " + permission);
    }

    public static boolean isValidSlot(int slot, int inventorySize) {
        return slot >= 0 && slot < inventorySize;
    }

    public static void sendErrorMessage(Player player, String message) {
        player.sendMessage(ChatColor.RED + message);
    }

    public static void sendSuccessMessage(Player player, String message) {
        player.sendMessage(ChatColor.GREEN + message);
    }

    public static void sendWarningMessage(Player player, String message) {
        player.sendMessage(ChatColor.YELLOW + message);
    }
}