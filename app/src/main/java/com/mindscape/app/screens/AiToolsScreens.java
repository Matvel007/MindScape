package com.mindscape.app.screens;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.mindscape.app.MainActivity;
import com.mindscape.app.R;

public final class AiToolsScreens {
    private AiToolsScreens() {}

    public static View aiToolsScreen(MainActivity host) {
        boolean ru = "ru".equals(host.activeLanguage());
        ScrollView scroll = new ScrollView(host);
        LinearLayout body = new LinearLayout(host);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(host.dp(18), host.dp(12), host.dp(18), host.dp(18));
        scroll.addView(body);

        body.addView(host.text(ru ? "ИИ-инструменты" : "AI tools", 24, host.primaryText(), true));
        body.addView(host.text(ru ? "Файл будет отправлен выбранному вами AI-провайдеру, а результат сохранится в приложении." : "The file is sent to your selected AI provider and the result is saved in the app.", 13, host.secondaryText(), false));
        body.addView(host.spacer(10));

        body.addView(toolCard(
                host,
                ru ? "Транскрибация" : "Transcription",
                ru ? "Прикрепите аудио. Текст будет добавлен в центр «Транскрибация»." : "Attach audio. Text is added to “Transcription”.",
                ru ? "Выбрать аудио" : "Choose audio",
                "transcription"
        ));
        body.addView(host.spacer(12));
        body.addView(toolCard(
                host,
                "OCR",
                ru ? "Прикрепите изображение. Текст будет добавлен в центр «OCR»." : "Attach an image. Text is added to “OCR”.",
                ru ? "Выбрать изображение" : "Choose image",
                "ocr"
        ));
        body.addView(host.spacer(16));

        Button back = host.createStyledButton(host.str(R.string.str_back));
        back.setOnClickListener(v -> {
            host.selectedSection("Главная");
            host.renderContent();
            host.updateBottomNav();
        });
        body.addView(back);
        return scroll;
    }

    private static View toolCard(MainActivity host, String title, String description, String action, String feature) {
        LinearLayout card = host.card(host.surface());
        card.setPadding(host.dp(16), host.dp(14), host.dp(16), host.dp(14));
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(host.roundedBg(host.surface(), 16, 1, host.strokeColor()));
        LinearLayout titleRow = new LinearLayout(host);
        titleRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(host);
        icon.setImageResource("ocr".equals(feature) ? R.drawable.ic_ocr : R.drawable.ic_transcription);
        icon.setColorFilter(host.accentColor());
        icon.setPadding(host.dp(8), host.dp(8), host.dp(8), host.dp(8));
        icon.setBackground(host.roundedBg(host.isDarkTheme() ? Color.rgb(45, 54, 68) : host.softSurface(), 14, 0, Color.TRANSPARENT));
        titleRow.addView(icon, new LinearLayout.LayoutParams(host.dp(42), host.dp(42)));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(host.dp(10), 0, 0, 0);
        titleRow.addView(host.text(title, 18, host.primaryText(), true), titleParams);
        card.addView(titleRow);
        card.addView(host.spacer(8));
        card.addView(host.text(description, 13, host.secondaryText(), false));
        card.addView(host.spacer(10));
        card.addView(toolActionRow(host, action, feature));
        return card;
    }

    private static View toolActionRow(MainActivity host, String title, String feature) {
        LinearLayout row = new LinearLayout(host);
        row.setGravity(android.view.Gravity.CENTER);
        row.setPadding(host.dp(14), host.dp(12), host.dp(14), host.dp(12));
        row.setBackground(host.roundedBg(host.softSurface(), 16, 1, host.strokeColor()));
        row.setOnClickListener(v -> host.pickAiToolFile(feature));

        TextView label = host.text(title, 15, host.primaryText(), true);
        label.setGravity(android.view.Gravity.CENTER);
        row.addView(label, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }
}
