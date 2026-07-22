package com.mindscape.app.ai;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ScrollView;

import com.mindscape.app.AiClient;
import com.mindscape.app.AiCommandParser;
import com.mindscape.app.ChatMessage;
import com.mindscape.app.MainActivity;
import com.mindscape.app.R;

import java.util.List;
import java.util.Locale;

public final class AiStreamRunner {
    private AiStreamRunner() {}

    public static void stream(MainActivity host, String query, ChatMessage aiMsg, View aiMsgView, ScrollView scroll) {
        long now = System.currentTimeMillis();
        if (now - host.lastAiRequestTime() < host.aiRequestCooldownMs()) {
            host.updateAiMsgOnUi(aiMsg, aiMsgView, scroll, "Пожалуйста, подождите немного перед следующим запросом.", true);
            return;
        }
        host.lastAiRequestTime(now);

        final boolean[] isResponding = {false};

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable animateDots = new Runnable() {
            int count = 0;
            @Override
            public void run() {
                if (isResponding[0]) return;
                count = (count + 1) % 4;
                StringBuilder dots = new StringBuilder();
                for (int i = 0; i < count; i++) dots.append(".");
                host.updateAiMsgOnUi(aiMsg, aiMsgView, scroll, dots.toString(), false);
                handler.postDelayed(this, 300);
            }
        };
        handler.post(animateDots);

        try {
            if (android.text.TextUtils.isEmpty(host.aiProviderBaseUrl()) || android.text.TextUtils.isEmpty(host.aiProviderApiKey()) || android.text.TextUtils.isEmpty(host.aiProviderModel())) {
                isResponding[0] = true;
                host.updateAiMsgOnUi(aiMsg, aiMsgView, scroll, host.str(R.string.str_err_api_key), true);
                return;
            }

            String sysPrompt = String.format(host.str(R.string.str_ai_sys_prompt), host.categoriesSummary(), host.notesSummary() + "\n" + host.localFilesSummary(fileContextQuery(host, query)))
                    + host.fileCommandPrompt();
            String response = AiClient.streamChatCompletion(
                    host.aiProviderBaseUrl(),
                    host.aiProviderApiKey(),
                    host.aiProviderModel(),
                    host.aiMaxTokens(),
                    sysPrompt,
                    host.currentChatMessages(),
                    aiMsg,
                    accumulated -> {
                        isResponding[0] = true;
                        host.updateAiMsgOnUi(aiMsg, aiMsgView, scroll, accumulated, false);
                    }
            );
            host.consecutiveAiErrors(0);
            List<AiCommandParser.Command> commands = AiCommandParser.parse(response);
            boolean hasCommands = !commands.isEmpty();
            boolean shouldExecute = shouldExecuteCommands(query, response);
            if (hasCommands && !shouldExecute) {
                String visible = AiCommandParser.stripCommands(response).trim();
                String plan = describePendingCommands(host, commands);
                String prompt = "ru".equals(host.activeLanguage())
                        ? "Напишите \"Выполни\", если хотите применить эти изменения."
                        : "Reply \"Execute\" if you want to apply these changes.";
                response = (visible.isEmpty() ? plan : visible + "\n\n" + plan) + "\n\n" + prompt;
            }
            host.updateAiMsgOnUi(aiMsg, aiMsgView, scroll, response, true);
            host.maybeNotifyAiResponse(response);
            if (shouldExecute) {
                host.executeAiCommands(response);
            }
        } catch (AiClient.HttpException error) {
            if (com.mindscape.app.BuildConfig.DEBUG) android.util.Log.e("AI_DEBUG", "Stream HTTP error", error);
            isResponding[0] = true;
            host.consecutiveAiErrors(host.consecutiveAiErrors() + 1);
            host.updateAiMsgOnUi(aiMsg, aiMsgView, scroll, host.formatAiHttpError(error.code, error.responseBody), true);
        } catch (Exception error) {
            if (com.mindscape.app.BuildConfig.DEBUG) android.util.Log.e("AI_DEBUG", "Stream error", error);
            isResponding[0] = true;
            host.updateAiMsgOnUi(aiMsg, aiMsgView, scroll, host.userFriendlyError(error), true);
        }
    }

    private static String fileContextQuery(MainActivity host, String query) {
        StringBuilder combined = new StringBuilder(query == null ? "" : query);
        List<ChatMessage> messages = host.currentChatMessages();
        int start = Math.max(0, messages.size() - 8);
        for (int i = start; i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            if (message == null || message.text == null) continue;
            combined.append('\n').append(message.text);
        }
        return combined.toString();
    }

    private static boolean shouldExecuteCommands(String query, String response) {
        if (AiCommandParser.parse(response).isEmpty()) return false;
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT);
        return q.contains("выполни")
                || q.contains("подтверждаю")
                || q.contains("подтверждаю выполнение")
                || q.contains("применить изменения")
                || q.contains("примени изменения")
                || q.contains("применяй")
                || q.contains("выполняй")
                || q.contains("делай")
                || q.contains("сделай это")
                || q.contains("давай")
                || q.equals("да")
                || q.equals("ок")
                || q.equals("окей")
                || q.contains("согласен")
                || q.contains("согласна")
                || q.contains("можно")
                || q.contains("execute")
                || q.contains("confirm")
                || q.contains("go ahead")
                || q.contains("do it")
                || q.contains("ok ")
                || q.contains("apply changes");
    }

    private static String describePendingCommands(MainActivity host, List<AiCommandParser.Command> commands) {
        boolean ru = "ru".equals(host.activeLanguage());
        StringBuilder plan = new StringBuilder(ru ? "План изменений:" : "Change plan:");
        for (AiCommandParser.Command command : commands) {
            String target = command.firstArg();
            if (target == null || target.trim().isEmpty()) {
                target = ru ? "без названия" : "untitled";
            }
            plan.append("\n• ").append(commandLabel(command.type, ru)).append(": ").append(target.trim());
        }
        return plan.toString();
    }

    private static String commandLabel(String type, boolean ru) {
        if ("CREATE_CENTER".equals(type)) return ru ? "создать центр" : "create center";
        if ("MOVE_CENTER".equals(type)) return ru ? "переместить центр" : "move center";
        if ("CREATE_CATEGORY".equals(type)) return ru ? "создать папку" : "create folder";
        if ("CREATE_NOTE".equals(type)) return ru ? "создать заметку" : "create note";
        if ("CREATE_CONNECTION".equals(type)) return ru ? "создать связь" : "create connection";
        if ("DELETE_NODE".equals(type)) return ru ? "удалить узел" : "delete node";
        if ("HIDE_NODE".equals(type)) return ru ? "скрыть узел" : "hide node";
        if ("SHOW_NODE".equals(type)) return ru ? "показать узел" : "show node";
        if ("DELETE_FILE_LINK".equals(type)) return ru ? "удалить файл" : "delete file";
        if ("MOVE_FILE_LINK".equals(type)) return ru ? "переместить файл" : "move file";
        if ("COPY_FILE_LINK".equals(type)) return ru ? "скопировать файл" : "copy file";
        return ru ? "изменение" : "change";
    }
}
