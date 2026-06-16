package com.aeracraft.report.util;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class TimeUtil {

    private static final DateTimeFormatter DEFAULT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    public static String format(Instant instant) {
        if (instant == null) {
            return "";
        }
        return DEFAULT_FORMATTER.format(instant);
    }

    public static String formatDate(Instant instant) {
        if (instant == null) {
            return "";
        }
        return DATE_FORMATTER.format(instant);
    }

    public static String formatDuration(Duration duration) {
        if (duration == null) {
            return "0s";
        }

        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");

        return sb.toString().trim();
    }

    public static String formatDuration(long minutes) {
        return formatDuration(Duration.ofMinutes(minutes));
    }

    public static String formatRelativeTime(Instant instant) {
        if (instant == null) {
            return "unknown";
        }

        Duration duration = Duration.between(instant, Instant.now());

        if (duration.isNegative()) {
            return "in the future";
        }

        if (duration.toDays() > 30) {
            return formatDate(instant);
        }

        if (duration.toDays() > 0) {
            return duration.toDays() + " day(s) ago";
        }

        if (duration.toHours() > 0) {
            return duration.toHours() + " hour(s) ago";
        }

        if (duration.toMinutes() > 0) {
            return duration.toMinutes() + " minute(s) ago";
        }

        return "just now";
    }

    public static long minutesToMillis(long minutes) {
        return TimeUnit.MINUTES.toMillis(minutes);
    }

    public static long hoursToMillis(long hours) {
        return TimeUnit.HOURS.toMillis(hours);
    }

    public static long daysToMillis(long days) {
        return TimeUnit.DAYS.toMillis(days);
    }

    public static Instant millisToInstant(long millis) {
        return Instant.ofEpochMilli(millis);
    }

    public static boolean isExpired(Instant instant) {
        if (instant == null) {
            return false;
        }
        return instant.isBefore(Instant.now());
    }

    public static boolean isWithinLastMinutes(Instant instant, int minutes) {
        if (instant == null) {
            return false;
        }
        Instant threshold = Instant.now().minusSeconds(minutes * 60L);
        return instant.isAfter(threshold);
    }
}
