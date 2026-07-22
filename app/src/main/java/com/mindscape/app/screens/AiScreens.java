package com.mindscape.app.screens;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.mindscape.app.ChatMessage;
import com.mindscape.app.ChatSession;
import com.mindscape.app.MainActivity;
import com.mindscape.app.R;

import java.util.ArrayList;

public final class AiScreens {
    private AiScreens() {}

    public static View aiScreen(MainActivity host) {
        boolean ru = "ru".equals(host.activeLanguage());
        LinearLayout layout = new LinearLayout(host);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(host.surface());

        LinearLayout topBar = new LinearLayout(host);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(host.dp(16), host.dp(12), host.dp(16), host.dp(12));
        topBar.setBackgroundColor(host.surface());
        topBar.setElevation(host.dp(4));

        android.widget.ImageView backBtn = host.iconAction("ic_arrow_back", host.accentColor());
        backBtn.setOnClickListener(v -> {
            host.selectedSection("Главная");
            host.renderContent();
            host.updateBottomNav();
        });
        topBar.addView(backBtn);

        TextView title = host.text(host.str(R.string.str_ai_assistant_title), 20, host.primaryText(), true);
        topBar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        android.widget.ImageView sessionsIcon = host.iconAction("ic_nav_notes", host.accentColor());
        sessionsIcon.setOnClickListener(v -> showAiSessionsDialog(host));
        topBar.addView(sessionsIcon);

        android.widget.ImageView settingsIcon = host.iconAction("ic_nav_settings", host.accentColor());
        settingsIcon.setOnClickListener(v -> {
            host.previousSection("AI");
            host.selectedSection("Настройки");
            host.openAiSettingsNext(true);
            host.renderContent();
            host.updateBottomNav();
        });
        topBar.addView(settingsIcon);

        layout.addView(topBar);

        ScrollView scroll = new ScrollView(host);
        LinearLayout chatHistory = new LinearLayout(host);
        chatHistory.setOrientation(LinearLayout.VERTICAL);
        chatHistory.setPadding(host.dp(16), host.dp(16), host.dp(16), host.dp(16));
        scroll.addView(chatHistory);


        for (ChatMessage msg : host.currentChatMessages()) {
            chatHistory.addView(renderChatMessage(host, msg));
        }

        layout.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout inputArea = new LinearLayout(host);
        inputArea.setOrientation(LinearLayout.HORIZONTAL);
        inputArea.setGravity(Gravity.CENTER_VERTICAL);
        inputArea.setPadding(host.dp(14), host.dp(6), host.dp(14), host.dp(7));
        inputArea.setBackgroundColor(host.surface());
        inputArea.setElevation(host.dp(8));
        ViewCompat.setOnApplyWindowInsetsListener(inputArea, (view, insets) -> {
            int bottomInset = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars()).bottom;
            int bottomPadding = host.dp(7) + bottomInset;
            if (inputArea.getPaddingBottom() != bottomPadding) {
                inputArea.setPadding(host.dp(14), host.dp(6), host.dp(14), bottomPadding);
            }
            return insets;
        });

        EditText input = host.createStyledInput("", host.str(R.string.str_message_hint));
        input.setMinHeight(host.dp(44));
        input.setMinimumHeight(host.dp(44));
        input.setPadding(host.dp(14), host.dp(6), host.dp(14), host.dp(6));
        input.setSingleLine(true);
        input.setMaxLines(1);
        input.setHorizontallyScrolling(true);
        input.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        inputArea.addView(input, new LinearLayout.LayoutParams(0, host.dp(44), 1f));


        ImageView sendBtn = host.iconAction("ic_send", Color.WHITE);
        sendBtn.setContentDescription(host.str(R.string.str_send));
        sendBtn.setBackground(host.roundedBg(host.accentColor(), 13, 0, Color.TRANSPARENT));
        sendBtn.setOnClickListener(v -> {
            String q = input.getText().toString().trim();
            if (q.isEmpty()) return;
            input.setText("");
            ChatMessage userMsg = new ChatMessage(false, q);
            host.currentChatMessages().add(userMsg);
            chatHistory.addView(renderChatMessage(host, userMsg));

            ChatMessage aiMsg = new ChatMessage(true, "");
            host.currentChatMessages().add(aiMsg);
            View aiMsgView = renderChatMessage(host, aiMsg);
            chatHistory.addView(aiMsgView);

            if (host.aiSaveChatEnabled()) {
                if (host.currentAiSession() == null) {
                    host.currentAiSession(new ChatSession(q.length() > 20 ? q.substring(0, 20) + "..." : q));
                    host.aiSessions().add(host.currentAiSession());
                }
                host.currentAiSession().messages = new ArrayList<>(host.currentChatMessages());
            }

            scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
            new Thread(() -> host.streamAiResponse(q, aiMsg, aiMsgView, scroll)).start();
        });

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(host.dp(44), host.dp(44));
        btnParams.setMargins(host.dp(8), 0, 0, 0);
        inputArea.addView(sendBtn, btnParams);

        layout.addView(inputArea);
        inputArea.post(() -> {
            WindowInsetsCompat rootInsets = ViewCompat.getRootWindowInsets(inputArea);
            if (rootInsets == null) return;
            int bottomInset = rootInsets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars()).bottom;
            inputArea.setPadding(host.dp(14), host.dp(6), host.dp(14), host.dp(7) + bottomInset);
        });
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
        return layout;
    }


    public static View renderChatMessage(MainActivity host, ChatMessage msg) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, host.dp(4), 0, host.dp(4));
        row.setGravity(msg.isAi ? Gravity.LEFT : Gravity.RIGHT);

        LinearLayout bubble = new LinearLayout(host);
        bubble.setOrientation(LinearLayout.VERTICAL);
        int bg = msg.isAi ? host.surface() : host.accentColor();
        int textColor = msg.isAi ? host.primaryText() : Color.WHITE;

        bubble.setBackground(host.roundedBg(bg, 16, msg.isAi ? 1 : 0, msg.isAi ? host.strokeColor() : Color.TRANSPARENT));
        bubble.setPadding(host.dp(16), host.dp(12), host.dp(16), host.dp(12));
        bubble.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        bubble.setOnLongClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) host.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("MindScape chat", host.filterAiCommands(msg.text)));
                Toast.makeText(host, "ru".equals(host.activeLanguage()) ? "Сообщение скопировано" : "Message copied", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        String display = host.filterAiCommands(msg.text);
        TextView tv = host.text(display.isEmpty() && msg.isAi ? "..." : display, 15, textColor, false);
        int maxWidth = (int) (host.getResources().getDisplayMetrics().widthPixels * 0.86f);
        tv.setMaxWidth(Math.min(maxWidth, host.dp(620)) - host.dp(32));
        bubble.addView(tv);

        if (msg.isAi) {
            java.util.List<String> cmds = host.extractAiCommands(msg.text);
            if (!cmds.isEmpty()) {
                LinearLayout cmdContainer = new LinearLayout(host);
                cmdContainer.setOrientation(LinearLayout.VERTICAL);
                cmdContainer.setPadding(0, host.dp(8), 0, 0);
                for (String cmd : cmds) {
                    LinearLayout cmdRow = new LinearLayout(host);
                    cmdRow.setOrientation(LinearLayout.HORIZONTAL);
                    cmdRow.setGravity(Gravity.CENTER_VERTICAL);
                    cmdRow.setPadding(0, host.dp(3), 0, host.dp(3));

                    View icon = host.createCustomCheckbox(true);
                    LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(host.dp(18), host.dp(18));
                    iconParams.setMargins(0, 0, host.dp(8), 0);
                    cmdRow.addView(icon, iconParams);

                    TextView cmdText = host.text(cmd, 14, textColor, false);
                    cmdText.setSingleLine(true);
                    cmdText.setEllipsize(TextUtils.TruncateAt.END);
                    cmdRow.addView(cmdText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    cmdContainer.addView(cmdRow);
                }

                TextView doneTv = host.text(host.str(R.string.str_done), 12, host.secondaryText(), true);
                doneTv.setPadding(0, host.dp(6), 0, 0);
                cmdContainer.addView(doneTv);
                bubble.addView(cmdContainer);
            }
        }

        row.addView(bubble, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    public static void updateAiMsgOnUi(MainActivity host, ChatMessage msg, View msgView, ScrollView scroll, String rawText, boolean isDone) {
        msg.text = rawText;
        final String display = host.filterAiCommands(rawText);
        final java.util.List<String> cmds = host.extractAiCommands(rawText);

        host.runOnUiThread(() -> {
            if (msgView instanceof LinearLayout) {
                LinearLayout bubble = (LinearLayout) ((LinearLayout) msgView).getChildAt(0);
                if (bubble != null && bubble.getChildAt(0) instanceof TextView) {
                    String finalDisplay = display;
                    if (isDone && finalDisplay.isEmpty() && cmds.isEmpty()) {
                        finalDisplay = host.str(R.string.str_process_completed);
                    }
                    ((TextView) bubble.getChildAt(0)).setText(finalDisplay.isEmpty() && !isDone ? "..." : finalDisplay);

                    LinearLayout cmdContainer = null;
                    if (bubble.getChildCount() > 1) {
                        cmdContainer = (LinearLayout) bubble.getChildAt(1);
                        cmdContainer.removeAllViews();
                    } else if (!cmds.isEmpty()) {
                        cmdContainer = new LinearLayout(host);
                        cmdContainer.setOrientation(LinearLayout.VERTICAL);
                        cmdContainer.setPadding(0, host.dp(8), 0, 0);
                        bubble.addView(cmdContainer);
                    }

                    if (cmdContainer != null) {
                        int textColor = msg.isAi ? host.primaryText() : Color.WHITE;
                        for (String cmd : cmds) {
                            LinearLayout cmdRow = new LinearLayout(host);
                            cmdRow.setOrientation(LinearLayout.HORIZONTAL);
                            cmdRow.setGravity(Gravity.CENTER_VERTICAL);
                            cmdRow.setPadding(0, host.dp(3), 0, host.dp(3));

                            View icon = host.createCustomCheckbox(isDone);
                            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(host.dp(18), host.dp(18));
                            iconParams.setMargins(0, 0, host.dp(8), 0);
                            cmdRow.addView(icon, iconParams);

                            cmdRow.addView(host.text(cmd, 14, textColor, false));
                            cmdContainer.addView(cmdRow);
                        }
                        if (isDone && !cmds.isEmpty()) {
                            TextView doneTv = host.text(host.str(R.string.str_done), 12, host.secondaryText(), true);
                            doneTv.setPadding(0, host.dp(6), 0, 0);
                            cmdContainer.addView(doneTv);
                        }
                    }
                }
            }
            scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
        });
    }

    public static void showAiSessionsDialog(MainActivity host) {
        LinearLayout layout = new LinearLayout(host);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(host.dp(20), host.dp(20), host.dp(20), host.dp(20));

        layout.addView(host.text(host.str(R.string.str_chat_sessions), 18, host.primaryText(), true));
        layout.addView(host.spacer(12));

        final android.app.AlertDialog[] dialogRef = new android.app.AlertDialog[1];

        if (host.aiSessions().isEmpty()) {
            layout.addView(host.text(host.str(R.string.str_no_saved_sessions), 14, host.secondaryText(), false));
        } else {
            ScrollView scroll = new ScrollView(host);
            LinearLayout list = new LinearLayout(host);
            list.setOrientation(LinearLayout.VERTICAL);

            for (ChatSession session : host.aiSessions()) {
                LinearLayout row = new LinearLayout(host);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, host.dp(8), 0, host.dp(8));

                TextView sessionTitle = host.text(session.title, 16, host.primaryText(), session == host.currentAiSession());
                row.addView(sessionTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                android.widget.ImageView del = host.iconAction("ic_trash", Color.rgb(200, 80, 80));
                del.setOnClickListener(v -> {
                    host.aiSessions().remove(session);
                    if (host.currentAiSession() == session) {
                        host.currentAiSession(null);
                        host.currentChatMessages().clear();
                        host.aiScreenView(null);
                        host.renderContent();
                    }
                    if (dialogRef[0] != null) dialogRef[0].dismiss();
                    showAiSessionsDialog(host);
                });
                row.addView(del);

                row.setOnClickListener(v -> {
                    host.currentAiSession(session);
                    host.currentChatMessages(new ArrayList<>(session.messages));
                    if (dialogRef[0] != null) dialogRef[0].dismiss();
                    host.aiScreenView(null);
                    host.renderContent();
                });

                list.addView(row);
                View divider = new View(host);
                divider.setBackgroundColor(Color.argb(30, 150, 150, 150));
                list.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, host.dp(1)));
            }
            scroll.addView(list);
            layout.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, host.dp(200)));
        }

        Button newSession = host.createStyledButton(host.str(R.string.str_new_session));
        newSession.setOnClickListener(v -> {
            host.currentAiSession(null);
            host.currentChatMessages(new ArrayList<>());
            if (dialogRef[0] != null) dialogRef[0].dismiss();
            host.aiScreenView(null);
            host.renderContent();
        });
        layout.addView(host.spacer(12));
        layout.addView(newSession);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(host)
                .setView(layout)
                .setNegativeButton(host.str(R.string.str_close), null)
                .create();
        dialogRef[0] = dialog;
        dialog.getWindow().setBackgroundDrawable(host.roundedBg(host.surface(), 16, 0, Color.TRANSPARENT));
        dialog.show();
    }
}
