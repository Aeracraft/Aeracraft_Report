package com.aeracraft.report.language;

import org.bukkit.ChatColor;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MessageFormatter {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)\\}");
    private static final Pattern COLOR_PATTERN = Pattern.compile("&([0-9a-fk-orA-FK-OR])");

    public static String format(String message) {
        if (message == null) {
            return "";
        }

        message = translateColorCodes(message);
        return message;
    }

    public static String format(String message, Map<String, String> placeholders) {
        if (message == null) {
            return "";
        }

        message = translateColorCodes(message);

        if (placeholders != null) {
            Matcher matcher = PLACEHOLDER_PATTERN.matcher(message);
            StringBuilder sb = new StringBuilder();

            while (matcher.find()) {
                String key = matcher.group(1);
                String value = placeholders.getOrDefault(key, matcher.group(0));
                matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
            }
            matcher.appendTail(sb);

            return sb.toString();
        }

        return message;
    }

    public static List<String> format(List<String> messages) {
        if (messages == null) {
            return List.of();
        }

        return messages.stream()
                .map(MessageFormatter::format)
                .collect(Collectors.toList());
    }

    public static List<String> format(List<String> messages, Map<String, String> placeholders) {
        if (messages == null) {
            return List.of();
        }

        return messages.stream()
                .map(msg -> format(msg, placeholders))
                .collect(Collectors.toList());
    }

    private static String translateColorCodes(String message) {
        if (message == null) {
            return "";
        }

        return message.replace("&", "§");
    }

    public static String stripColor(String message) {
        if (message == null) {
            return "";
        }

        return ChatColor.stripColor(message);
    }

    public static String stripPlaceholders(String message) {
        if (message == null) {
            return "";
        }

        return message.replaceAll("\\{[^}]+\\}", "");
    }
}
