package com.aeracraft.report.util;

import net.md_5.bungee.api.ChatColor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ColorUtil {

    private static final Pattern COLOR_PATTERN = Pattern.compile("&([0-9a-fk-orA-FK-OR])");
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static String colorize(String message) {
        if (message == null) {
            return "";
        }

        message = translateHexColors(message);
        message = translateAltColors(message);

        return message;
    }

    public static String translateAltColors(String message) {
        if (message == null) {
            return "";
        }

        Matcher matcher = COLOR_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder();

        while (matcher.find()) {
            String colorCode = matcher.group(1);
            String replacement = String.valueOf(ChatColor.COLOR_CHAR) + colorCode.toLowerCase();
            matcher.appendReplacement(buffer, replacement);
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    public static String translateHexColors(String message) {
        if (message == null) {
            return "";
        }

        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder();

        while (matcher.find()) {
            String hex = matcher.group(1);
            String replacement = String.valueOf(ChatColor.COLOR_CHAR) + "x" +
                    ChatColor.COLOR_CHAR + hex.charAt(0) +
                    ChatColor.COLOR_CHAR + hex.charAt(1) +
                    ChatColor.COLOR_CHAR + hex.charAt(2) +
                    ChatColor.COLOR_CHAR + hex.charAt(3) +
                    ChatColor.COLOR_CHAR + hex.charAt(4) +
                    ChatColor.COLOR_CHAR + hex.charAt(5);
            matcher.appendReplacement(buffer, replacement);
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    public static List<String> colorize(List<String> messages) {
        if (messages == null) {
            return List.of();
        }

        return messages.stream()
                .map(ColorUtil::colorize)
                .collect(Collectors.toList());
    }

    public static String stripColor(String message) {
        if (message == null) {
            return "";
        }

        return ChatColor.stripColor(message);
    }

    public static String stripColorAndHex(String message) {
        if (message == null) {
            return "";
        }

        message = message.replaceAll(HEX_PATTERN.pattern(), "");
        return stripColor(message);
    }

    public static boolean isColorCode(char c) {
        return "0123456789abcdefklmnor".indexOf(Character.toLowerCase(c)) != -1;
    }

    public static String getLastColorCode(String message) {
        if (message == null || message.isEmpty()) {
            return null;
        }

        for (int i = message.length() - 1; i >= 0; i--) {
            char c = message.charAt(i);
            if (c == ChatColor.COLOR_CHAR && i + 1 < message.length()) {
                char next = message.charAt(i + 1);
                if (isColorCode(next)) {
                    return String.valueOf(next);
                }
            }
        }

        return null;
    }
}
