package com.mindscape.app.util;

import java.util.Locale;

/**
 * Чистые текстовые утилиты, не зависящие от Activity/Context.
 * Источник: вынесено из MainActivity.java (sanitizeInput, formatTimeAgo, formatFileSize).
 */
public final class Texts {

    private Texts() {}

    /** Санитизация пользовательского ввода: обрезка, лимит длины, удаление опасных символов. */
    public static String sanitizeInput(String input, int maxLength) {
        if (input == null) return "";
        String sanitized = input.trim();
        if (sanitized.length() > maxLength) sanitized = sanitized.substring(0, maxLength);
        sanitized = sanitized.replaceAll("[<>\"'`]", "");
        sanitized = sanitized.replace("..", "").replace("./", "");
        return sanitized;
    }

    /** «5 мин назад» / «5m ago» — относительное время. {@code ru}=true → русская форма. */
    public static String formatTimeAgo(long time, boolean ru, String justNowLabel) {
        long diff = System.currentTimeMillis() - time;
        if (diff < 60000) return justNowLabel;
        if (diff < 3600000) {
            long m = diff / 60000;
            return ru ? m + " мин назад" : m + "m ago";
        }
        if (diff < 86400000) {
            long h = diff / 3600000;
            return ru ? h + " ч назад" : h + "h ago";
        }
        long d = diff / 86400000;
        return ru ? d + " д назад" : d + "d ago";
    }

    /** «12.3 KB» / «Unknown size» — форматирование размера файла. */
    public static String formatFileSize(long bytes, String unknownSizeLabel) {
        if (bytes < 0) return unknownSizeLabel;
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb);
        return String.format(Locale.US, "%.1f GB", mb / 1024.0);
    }
}
