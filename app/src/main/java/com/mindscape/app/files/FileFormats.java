package com.mindscape.app.files;

import android.graphics.Color;

import java.util.Locale;

/**
 * Определение формата локального файла: имя drawable-иконки, цвет иконки,
 * проверки расширений/MIME. Не зависит от Context/Activity.
 * Источник: вынесено из MainActivity.java (fileIconName, fileIconColor, hasFileExtension).
 */
public final class FileFormats {

    private FileFormats() {}

    public static String iconName(String mimeType, String title) {
        String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        String t = title == null ? "" : title.toLowerCase(Locale.ROOT);
        if (mime.equals("application/pdf") || t.endsWith(".pdf")) return "ic_file_pdf";
        if (mime.contains("word") || mime.contains("msword") || t.endsWith(".doc") || t.endsWith(".docx")) return "ic_file_word";
        if (mime.contains("spreadsheet") || mime.contains("excel") || t.endsWith(".xls") || t.endsWith(".xlsx") || t.endsWith(".csv")) return "ic_file_sheet";
        if (mime.contains("presentation") || mime.contains("powerpoint") || t.endsWith(".ppt") || t.endsWith(".pptx")) return "ic_file_presentation";
        if (mime.contains("android.package-archive") || hasExtension(t, ".apk")) return "ic_file_apk";
        if (hasExtension(t, ".exe", ".msi", ".app", ".dmg")) return "ic_file_app";
        if (mime.startsWith("image/") || hasExtension(t, ".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp", ".heic", ".heif", ".svg")) return "ic_file_image";
        if (mime.startsWith("audio/") || hasExtension(t, ".mp3", ".wav", ".ogg", ".m4a", ".flac", ".aac")) return "ic_file_audio";
        if (mime.startsWith("video/") || hasExtension(t, ".mp4", ".mkv", ".avi", ".mov", ".webm", ".3gp")) return "ic_file_video";
        if (mime.contains("zip") || mime.contains("rar") || mime.contains("7z") || mime.contains("tar")
                || hasExtension(t, ".zip", ".rar", ".7z", ".tar", ".gz", ".bz2")) return "ic_file_archive";
        if (mime.contains("json") || mime.contains("xml")
                || hasExtension(t, ".py", ".js", ".ts", ".java", ".kt", ".html", ".css", ".json", ".xml", ".yml", ".yaml", ".sh", ".sql")) return "ic_file_code";
        if (mime.startsWith("text/") || hasExtension(t, ".txt", ".md", ".rtf", ".log")) return "ic_file_text";
        return "ic_local_file";
    }

    public static int iconColor(String icon) {
        switch (icon) {
            case "ic_file_pdf":         return Color.rgb(232, 81, 106);
            case "ic_file_word":        return Color.rgb(78, 168, 255);
            case "ic_file_sheet":       return Color.rgb(72, 190, 118);
            case "ic_file_presentation":return Color.rgb(237, 156, 55);
            case "ic_file_image":       return Color.rgb(77, 190, 214);
            case "ic_file_apk":         return Color.rgb(61, 190, 96);
            case "ic_file_app":         return Color.rgb(126, 146, 178);
            case "ic_file_text":        return Color.rgb(133, 151, 173);
            case "ic_file_code":        return Color.rgb(32, 184, 166);
            case "ic_file_archive":     return Color.rgb(216, 154, 43);
            case "ic_file_audio":       return Color.rgb(140, 107, 232);
            case "ic_file_video":       return Color.rgb(225, 106, 78);
            default:                    return Color.rgb(95, 126, 234);
        }
    }

    public static boolean hasExtension(String title, String... extensions) {
        for (String ext : extensions) {
            if (title.endsWith(ext)) return true;
        }
        return false;
    }

    public static boolean isImage(String mimeType, String title) {
        String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        String t = title == null ? "" : title.toLowerCase(Locale.ROOT);
        return mime.startsWith("image/") || hasExtension(t, ".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp", ".heic", ".heif", ".svg");
    }

    public static boolean isPdf(String mimeType, String title) {
        String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        String t = title == null ? "" : title.toLowerCase(Locale.ROOT);
        return mime.equals("application/pdf") || t.endsWith(".pdf");
    }

    public static boolean isDocx(String mimeType, String title) {
        String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        String t = title == null ? "" : title.toLowerCase(Locale.ROOT);
        return mime.contains("word") || mime.contains("msword") || t.endsWith(".doc") || t.endsWith(".docx");
    }

    public static boolean isTextReadable(String mimeType, String title) {
        String icon = iconName(mimeType, title);
        return icon.equals("ic_file_text") || icon.equals("ic_file_code")
                || icon.equals("ic_file_pdf") || icon.equals("ic_file_word")
                || isCsv(mimeType, title);
    }

    public static boolean isCsv(String mimeType, String title) {
        String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        String t = title == null ? "" : title.toLowerCase(Locale.ROOT);
        return mime.contains("csv") || t.endsWith(".csv") || t.endsWith(".tsv");
    }

    /** Современные OOXML-таблицы (zip). Старый бинарный .xls не поддерживается. */
    public static boolean isXlsx(String mimeType, String title) {
        String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        String t = title == null ? "" : title.toLowerCase(Locale.ROOT);
        return t.endsWith(".xlsx") || t.endsWith(".xlsm") || t.endsWith(".xltx")
                || mime.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                || mime.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.template");
    }

    /** Современные OOXML-презентации (zip). Старый бинарный .ppt не поддерживается. */
    public static boolean isPptx(String mimeType, String title) {
        String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        String t = title == null ? "" : title.toLowerCase(Locale.ROOT);
        return t.endsWith(".pptx") || t.endsWith(".pptm")
                || mime.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation");
    }

    public static boolean isRtf(String mimeType, String title) {
        String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        String t = title == null ? "" : title.toLowerCase(Locale.ROOT);
        return mime.contains("rtf") || t.endsWith(".rtf");
    }

    /** Всё, из чего LocalFileReader может извлечь читаемый текст для AI-контекста. */
    public static boolean isAiReadable(String mimeType, String title) {
        if (isTextReadable(mimeType, title)
                || isPdf(mimeType, title)
                || isDocx(mimeType, title)
                || isXlsx(mimeType, title)
                || isPptx(mimeType, title)
                || isRtf(mimeType, title)) {
            return true;
        }
        String icon = iconName(mimeType, title);
        return !icon.equals("ic_file_image")
                && !icon.equals("ic_file_audio")
                && !icon.equals("ic_file_video")
                && !icon.equals("ic_file_archive")
                && !icon.equals("ic_file_apk")
                && !icon.equals("ic_file_app");
    }

    public static boolean isKnownBinaryDocument(String mime, String title) {
        String m = mime == null ? "" : mime.toLowerCase(Locale.ROOT);
        String t = title == null ? "" : title.toLowerCase(Locale.ROOT);
        return isPdf(m, t) || isDocx(m, t)
                || m.startsWith("application/vnd.openxmlformats-officedocument")
                || m.contains("spreadsheet") || m.contains("excel")
                || m.contains("presentation") || m.contains("powerpoint")
                || m.contains("zip") || m.contains("rar") || m.contains("7z")
                || hasExtension(t, ".xlsx", ".pptx", ".zip", ".rar", ".7z", ".gz");
    }
}
