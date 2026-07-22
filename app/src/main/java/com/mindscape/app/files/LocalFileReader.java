package com.mindscape.app.files;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.mindscape.app.LocalFileLink;
import com.mindscape.app.data.MindScapeArchive;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Извлечение читаемого текста из локального/облачного файла для контекста ИИ.
 * Поддерживает PDF (pdfbox-android), DOCX (word/document.xml), текст.
 * Источник: вынесено из MainActivity.java (readLocalFileText, extractReadableText,
 * extractDocxText, limitText, readStreamToBytes).
 */
public final class LocalFileReader {

    private LocalFileReader() {}

    /** Считывает содержимое файла в строку (до maxChars). {@code null} если не удалось прочитать. */
    @Nullable
    public static String readText(Context ctx, LocalFileLink file, int maxChars) {
        try {
            byte[] bytes = readBytes(ctx, file, 5L * 1024L * 1024L);
            if (bytes == null) return null;
            return extractReadableText(file, bytes, maxChars);
        } catch (Exception e) {
            return null;
        }
    }

    /** Читает байты URI с ограничением размера. Бросает исключение при превышении лимита. */
    public static byte[] readBytes(Context ctx, LocalFileLink file, long limit) throws Exception {
        if (file.uri == null || file.uri.isEmpty()) return null;
        if (MindScapeArchive.isSqlarUri(file.uri)) {
            return MindScapeArchive.readSqlarBytes(ctx, MindScapeArchive.sqlarArchiveUri(file.uri), MindScapeArchive.sqlarEntryName(file.uri), limit);
        }
        Uri uri = Uri.parse(file.uri);
        try (InputStream stream = ctx.getContentResolver().openInputStream(uri)) {
            if (stream == null) return null;
            return readStreamToBytes(stream, limit);
        }
    }

    /** Читает байты из произвольного потока с ограничением. */
    public static byte[] readStreamToBytes(InputStream stream, long limit) throws Exception {
        if (limit > Integer.MAX_VALUE - 8L) {
            throw new Exception("File is too large to keep in memory");
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        long total = 0;
        while ((read = stream.read(buffer)) != -1) {
            total += read;
            if (total > limit) {
                throw new Exception("File too large to read for AI context");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    @Nullable
    public static String extractReadableText(LocalFileLink file, byte[] bytes, int maxChars) throws Exception {
        if (bytes == null || bytes.length == 0) return "";
        if (FileFormats.isPdf(file.mimeType, file.title)) {
            try (PDDocument document = PDDocument.load(bytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                return limitText(stripper.getText(document), maxChars);
            }
        }
        if (FileFormats.isDocx(file.mimeType, file.title)) {
            return limitText(extractDocxText(bytes), maxChars);
        }
        if (FileFormats.isXlsx(file.mimeType, file.title)) {
            return limitText(extractXlsxText(bytes), maxChars);
        }
        if (FileFormats.isPptx(file.mimeType, file.title)) {
            return limitText(extractPptxText(bytes), maxChars);
        }
        if (FileFormats.isRtf(file.mimeType, file.title)) {
            return limitText(stripRtf(new String(bytes, StandardCharsets.UTF_8)), maxChars);
        }
        return limitText(new String(bytes, StandardCharsets.UTF_8), maxChars);
    }

    public static String extractDocxText(byte[] bytes) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!"word/document.xml".equals(entry.getName())) continue;
                String xml = new String(readStreamToBytes(zip, 5L * 1024L * 1024L), StandardCharsets.UTF_8);
                return xml.replaceAll("<w:tab\\s*/>", "\t")
                        .replaceAll("</w:p>", "\n")
                        .replaceAll("<[^>]+>", "")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .replace("&amp;", "&")
                        .replace("&quot;", "\"")
                        .replace("&apos;", "'")
                        .trim();
            }
        }
        return "";
    }

    /**
     * Извлечение текста из XLSX (OOXML): общие строки (sharedStrings) + листы.
     * Ячейки с t="s" ссылаются на индекс в sharedStrings; прочие берут <v> как есть.
     * Возвращает текст табличного вида (ячейки через таб, строки через перевод).
     */
    public static String extractXlsxText(byte[] bytes) throws Exception {
        List<String> sharedStrings = new ArrayList<>();
        List<String> sheetXmls = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.equals("xl/sharedStrings.xml")) {
                    String xml = new String(readStreamToBytes(zip, 5L * 1024L * 1024L), StandardCharsets.UTF_8);
                    Matcher m = Pattern.compile("<si>(.*?)</si>", Pattern.DOTALL).matcher(xml);
                    while (m.find()) {
                        sharedStrings.add(decodeXmlEntities(m.group(1).replaceAll("<[^>]+>", "")));
                    }
                } else if (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml")) {
                    sheetXmls.add(new String(readStreamToBytes(zip, 5L * 1024L * 1024L), StandardCharsets.UTF_8));
                }
            }
        }
        if (sheetXmls.isEmpty()) {
            return String.join("\n", sharedStrings);
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < sheetXmls.size(); i++) {
            if (sheetXmls.size() > 1) out.append("[Sheet ").append(i + 1).append("]\n");
            String sheetXml = sheetXmls.get(i);
            String[] rows = sheetXml.split("</row>");
            for (String row : rows) {
                StringBuilder rowOut = new StringBuilder();
                Matcher cm = Pattern.compile("<c\\b[^>]*>(.*?)</c>", Pattern.DOTALL).matcher(row);
                while (cm.find()) {
                    String cTag = cm.group(0);
                    String inner = cm.group(1);
                    boolean isShared = cTag.contains("t=\"s\"");
                    Matcher vm = Pattern.compile("<v>(.*?)</v>", Pattern.DOTALL).matcher(inner);
                    String value = null;
                    if (vm.find()) {
                        value = vm.group(1);
                        if (isShared) {
                            try {
                                int idx = Integer.parseInt(value.trim());
                                if (idx >= 0 && idx < sharedStrings.size()) value = sharedStrings.get(idx);
                            } catch (NumberFormatException ignored) {
                                // expected: XLSX cells can contain inline or malformed shared-string indexes.
                            }
                        }
                    } else {
                        Matcher im = Pattern.compile("<is>.*?<t[^>]*>(.*?)</t>.*?</is>", Pattern.DOTALL).matcher(inner);
                        if (im.find()) value = decodeXmlEntities(im.group(1));
                    }
                    if (value != null) rowOut.append(value).append("\t");
                }
                if (rowOut.length() > 0) out.append(rowOut.toString().trim()).append("\n");
            }
        }
        return out.toString();
    }

    /**
     * Извлечение текста из PPTX (OOXML): текстовые_runs <a:t> по слайдам.
     */
    public static String extractPptxText(byte[] bytes) throws Exception {
        StringBuilder out = new StringBuilder();
        int slideIdx = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (!name.startsWith("ppt/slides/slide") || !name.endsWith(".xml")) continue;
                slideIdx++;
                String xml = new String(readStreamToBytes(zip, 5L * 1024L * 1024L), StandardCharsets.UTF_8);
                out.append("[Slide ").append(slideIdx).append("]\n");
                Matcher m = Pattern.compile("<a:t>(.*?)</a:t>", Pattern.DOTALL).matcher(xml);
                StringBuilder slideText = new StringBuilder();
                while (m.find()) {
                    if (slideText.length() > 0) slideText.append(" ");
                    slideText.append(decodeXmlEntities(m.group(1).replaceAll("<[^>]+>", "")));
                }
                out.append(slideText).append("\n\n");
            }
        }
        return out.toString().trim();
    }

    /** Грубая очистка RTF-разметки до читаемого текста. */
    public static String stripRtf(String rtf) {
        if (rtf == null) return "";
        String s = rtf;
        s = s.replaceAll("\\\\par\\b", "\n");
        s = s.replaceAll("\\\\line\\b", "\n");
        s = s.replaceAll("\\\\tab\\b", "\t");
        s = s.replaceAll("\\\\u-?\\d{1,6}\\??", " ");
        s = s.replaceAll("\\\\'[0-9a-fA-F]{2}", " ");
        s = s.replaceAll("\\\\[a-zA-Z]+-?\\d*\\s?", " ");
        s = s.replaceAll("[{}]", "");
        s = s.replaceAll("\\s{2,}", " ");
        return s.trim();
    }

    private static String decodeXmlEntities(String s) {
        if (s == null) return "";
        return s.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&#13;", "")
                .replace("&#10;", "\n")
                .replace("&#9;", "\t");
    }

    public static String limitText(String text, int maxChars) {
        if (text == null) return "";
        if (maxChars <= 0 || text.length() <= maxChars) return text;
        return text.substring(0, maxChars) + "\n\n...";
    }
}
