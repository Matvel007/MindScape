package com.mindscape.app.ai;

import android.content.Context;

import androidx.annotation.Nullable;

import com.mindscape.app.AiContextBuilder;
import com.mindscape.app.AiClient;
import com.mindscape.app.Category;
import com.mindscape.app.LocalFileLink;
import com.mindscape.app.Note;
import com.mindscape.app.files.FileFormats;
import com.mindscape.app.files.LocalFileReader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Чистые хелперы AI-контекста и обработки: сводки категорий/заметок/файлов,
 * форматирование ошибок, нормализация целей, поиск файлов.
 * Не зависит от Activity. Источник: вынесено из MainActivity.java.
 */
public final class AiHelpers {

    private AiHelpers() {}

    public static String categoriesSummary(List<Category> categories, List<Note> notes, String noCategoriesLabel) {
        return AiContextBuilder.categoriesSummary(categories, notes, noCategoriesLabel);
    }

    public static String notesSummary(List<Note> notes, String noNotesLabel) {
        return AiContextBuilder.notesSummary(notes, noNotesLabel);
    }

    public static String localFilesSummary(Context ctx, List<LocalFileLink> localFiles, String userQuery,
                                           java.util.function.Function<LocalFileLink, String> mimeLabel,
                                           java.util.function.Function<Long, String> sizeFormatter) {
        if (localFiles.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("\nДОСТУПНЫЕ ФАЙЛЫ:\n");
        for (LocalFileLink file : localFiles) {
            String sizeStr = sizeFormatter.apply(file.size);
            sb.append("- ").append(file.title)
                    .append(" [").append(mimeLabel.apply(file)).append("]");
            if (!sizeStr.isEmpty()) sb.append(" (").append(sizeStr).append(")");
            sb.append(" | path: ").append(file.uri).append("\n");

            if (isAiReadable(file)) {
                String content = readForAi(ctx, file, 4000);
                if (content != null && !content.isEmpty()) {
                    sb.append("  СОДЕРЖИМОЕ:\n").append(content).append("\n\n");
                }
            }
        }
        return sb.toString();
    }

    public static String fileCommandPrompt() {
        return "\n\nКОМАНДЫ (выводи в конце ответа строго по одной на строке):\n"
                + "||CREATE_CENTER||Название|| — создать новый центр только если пользователь прямо попросил новый центр; если центр уже существует, используй его путь и не создавай дубликат\n"
                + "||CREATE_CATEGORY||Название||Путь родителя|| — создать папку\n"
                + "||CREATE_CONNECTION||Источник||Цель|| — создать связь\n"
                + "||CREATE_NOTE||Заголовок||Путь||Текст|| — создать заметку\n"
                + "||HIDE_NODE||ID узла|| — скрыть узел\n"
                + "||SHOW_NODE||ID узла|| — показать узел\n"
                + "||MOVE_CENTER||Название||UP или DOWN|| — переместить центр\n"
                + "||DELETE_NODE||ID узла|| — удалить узел\n";
    }

    public static java.util.List<String> extractAiCommands(String text, java.util.function.Function<Integer, String> str) {
        java.util.List<String> cmds = new java.util.ArrayList<>();
        for (com.mindscape.app.AiCommandParser.Command command : com.mindscape.app.AiCommandParser.parse(text)) {
            String type = command.type;
            String[] args = command.args;
            String name = command.firstArg();
            if (type.equals("CREATE_CENTER")) cmds.add(str.apply(com.mindscape.app.R.string.str_cmd_create_center) + name);
            else if (type.equals("MOVE_CENTER")) cmds.add(str.apply(com.mindscape.app.R.string.str_cmd_move_center) + name + (args.length > 1 ? " → " + args[1] : ""));
            else if (type.equals("CREATE_CATEGORY")) cmds.add(str.apply(com.mindscape.app.R.string.str_cmd_create_category) + name);
            else if (type.equals("CREATE_NOTE")) cmds.add(str.apply(com.mindscape.app.R.string.str_cmd_create_note) + name);
            else if (type.equals("CREATE_CONNECTION")) cmds.add(str.apply(com.mindscape.app.R.string.str_create_connection) + ": " + name);
            else if (type.equals("DELETE_NODE")) cmds.add(str.apply(com.mindscape.app.R.string.str_cmd_delete) + name);
            else if (type.equals("HIDE_NODE")) cmds.add(str.apply(com.mindscape.app.R.string.str_cmd_hide) + name);
            else if (type.equals("SHOW_NODE")) cmds.add(str.apply(com.mindscape.app.R.string.str_cmd_show) + name);
            else if (type.equals("DELETE_FILE_LINK")) cmds.add(str.apply(com.mindscape.app.R.string.str_cmd_delete) + str.apply(com.mindscape.app.R.string.str_menu_local_file) + ": " + name);
            else if (type.equals("MOVE_FILE_LINK")) cmds.add(str.apply(com.mindscape.app.R.string.str_menu_move) + ": " + name + (args.length > 1 ? " → " + args[1] : ""));
            else if (type.equals("COPY_FILE_LINK")) cmds.add(str.apply(com.mindscape.app.R.string.str_menu_duplicate) + ": " + name + (args.length > 1 ? " → " + args[1] : ""));
        }
        return cmds;
    }

    public static String formatAiHttpError(int code, String responseBody, String errorLabel) {
        if (code == 429) {
            return errorLabel + "429: Слишком много запросов. Попробуйте позже.";
        } else if (code == 401 || code == 403) {
            return "Доступно после авторизации. Если вы уже вошли, выйдите и войдите снова.";
        } else if (code >= 500) {
            return errorLabel + code + ": Ошибка сервера. Попробуйте позже.";
        }
        return "Не удалось получить ответ. Попробуйте позже.";
    }

    public static String normalizeAiFolderTarget(String rawTarget, String noFolderLabel) {
        if (rawTarget == null) return "";
        String t = rawTarget.trim();
        if (t.isEmpty() || t.equalsIgnoreCase(noFolderLabel)) return "";
        return t;
    }

    public static @Nullable LocalFileLink findLocalFileLink(List<LocalFileLink> localFiles, String rawName,
                                                            java.util.function.Function<String, String> sanitizer) {
        if (rawName == null) return null;
        String name = sanitizer.apply(rawName);
        for (LocalFileLink file : localFiles) {
            if (file.title.equalsIgnoreCase(name)) return file;
        }
        // Also match by node ID or URI substring
        for (LocalFileLink file : localFiles) {
            if (file.nodeId().equalsIgnoreCase(name) || file.uri.contains(name)) return file;
        }
        return null;
    }

    public static boolean isAiReadable(LocalFileLink file) {
        return FileFormats.isAiReadable(file.mimeType, file.title);
    }

    public static @Nullable String readForAi(Context ctx, LocalFileLink file, int maxChars) {
        return LocalFileReader.readText(ctx, file, maxChars);
    }

    public static String readAll(InputStream stream) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line).append("\n");
        return sb.toString();
    }
}
