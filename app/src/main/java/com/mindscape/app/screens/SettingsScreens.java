package com.mindscape.app.screens;

import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.mindscape.app.MainActivity;
import com.mindscape.app.R;
import com.mindscape.app.ai.AiProviders;
import com.mindscape.app.ui.StyledToggleState;

import java.util.Locale;

public final class SettingsScreens {
    private SettingsScreens() {
    }

    private static String tr(MainActivity host, String ru, String en) {
        return "ru".equals(host.activeLanguage()) ? ru : en;
    }

    public static View settingsTabScreen(MainActivity host) {
        ScrollView scroll = new ScrollView(host);
        LinearLayout body = new LinearLayout(host);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(host.dp(18), host.dp(8), host.dp(18), host.dp(18));
        scroll.addView(body);

        if (host.openAiSettingsNext()) {
            host.openAiSettingsNext(false);
            showSubSettingsAi(host, body);
            return scroll;
        }
        if (host.openInterfaceSettingsNext()) {
            host.openInterfaceSettingsNext(false);
            showSubSettingsInterface(host, body);
            return scroll;
        }

        body.addView(host.text(host.str(R.string.str_settings), 22, host.primaryText(), true));
        body.addView(host.spacer(14));
        String theme = host.themeMode() == 0 ? host.str(R.string.str_theme_light)
                : host.themeMode() == 1 ? host.str(R.string.str_theme_dark) : host.str(R.string.str_theme_system);
        body.addView(settingsRow(host, host.str(R.string.str_ai_providers), host.aiProviderName() + " • " + host.aiProviderModel(), "ic_ai", v -> showSubSettingsAi(host, body)));
        body.addView(settingsRow(host, host.str(R.string.str_interface), theme + " • " + ("ru".equals(host.activeLanguage()) ? "Русский" : "English"), "ic_interface", v -> showSubSettingsInterface(host, body)));
        body.addView(settingsRow(host, host.str(R.string.str_graph_visualization), tr(host, "Анимации и отображение", "Animations and display"), "ic_graph", v -> showSubSettingsGraph(host, body)));
        body.addView(settingsRow(host, host.str(R.string.str_import_export), tr(host, "SQLAR бэкап и восстановление", "SQLAR backup and restore"), "ic_io", v -> showSubSettingsIO(host, body)));
        body.addView(settingsRow(host, host.str(R.string.str_notifications), tr(host, "Напоминания заметок", "Note reminders"), "ic_notifications", v -> showSubSettingsNotifications(host, body)));
        body.addView(settingsRow(host, host.str(R.string.str_about_app), tr(host, "Открытое Android-приложение", "Open-source Android app"), "ic_about", v -> showSubSettingsAbout(host, body)));
        return scroll;
    }

    private static View settingsRow(MainActivity host, String title, String subtitle, String icon, View.OnClickListener listener) {
        LinearLayout card = host.card(host.surface());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(host.dp(16), host.dp(14), host.dp(16), host.dp(14));
        card.setGravity(Gravity.CENTER_VERTICAL);
        android.widget.ImageView iconView = new android.widget.ImageView(host);
        int iconId = host.getResources().getIdentifier(icon, "drawable", host.getPackageName());
        if (iconId != 0) iconView.setImageResource(iconId);
        iconView.setColorFilter(host.accentColor());
        iconView.setPadding(0, 0, host.dp(14), 0);
        card.addView(iconView, new LinearLayout.LayoutParams(host.dp(38), host.dp(38)));
        LinearLayout labels = new LinearLayout(host);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(host.text(title, 15, host.primaryText(), true));
        labels.addView(host.text(subtitle, 12, host.secondaryText(), false));
        card.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, host.dp(8));
        card.setLayoutParams(params);
        return card;
    }

    private static Button settingsButton(MainActivity host, String text) {
        Button button = host.createStyledButton(text);
        button.setBackground(host.roundedBg(host.surface(), 16, 1, host.strokeColor()));
        button.setTextColor(host.primaryText());
        return button;
    }

    private static Button back(MainActivity host) {
        Button button = settingsButton(host, "← " + host.str(R.string.str_back));
        host.applyTextWeight(button, true);
        button.setOnClickListener(v -> {
            host.editingAiProviderIdx(-1);
            host.selectedSection("Настройки");
            host.renderContent();
        });
        return button;
    }

    public static void showSubSettingsAi(MainActivity host, LinearLayout body) {
        body.removeAllViews();
        body.addView(back(host));
        body.addView(host.spacer(14));
        body.addView(host.text(host.str(R.string.str_ai_providers), 20, host.primaryText(), true));
        body.addView(host.spacer(10));

        int selected = host.getProviderIndex(host.aiProviderBaseUrl());
        body.addView(host.text(host.str(R.string.str_provider_name), 12, host.secondaryText(), true));
        body.addView(host.createStyledDropdown(AiProviders.NAMES[selected], AiProviders.NAMES, selected, choice -> {
            host.editingAiProviderIdx(choice);
            host.aiProviderName(AiProviders.NAMES[choice]);
            if (choice < AiProviders.URLS.length - 1) host.aiProviderBaseUrl(AiProviders.URLS[choice]);
            showSubSettingsAi(host, body);
        }));

        EditText baseUrl = host.createStyledInput(host.aiProviderBaseUrl(), "https://...");
        body.addView(host.text("Base URL", 12, host.secondaryText(), true));
        body.addView(baseUrl);
        EditText apiKey = host.createStyledInput(host.aiProviderApiKey(), host.str(R.string.str_api_key));
        apiKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        body.addView(host.text(host.str(R.string.str_api_key), 12, host.secondaryText(), true));
        body.addView(apiKey);
        EditText chatModel = host.createStyledInput(host.aiProviderModel(), tr(host, "Модель чата", "Chat model"));
        body.addView(host.text(host.str(R.string.str_model), 12, host.secondaryText(), true));
        body.addView(chatModel);
        body.addView(host.spacer(8));
        body.addView(host.text(tr(host, "Модели инструментов", "Tool models"), 14, host.primaryText(), true));
        EditText ocr = host.createStyledInput(host.ocrModel(), tr(host, "Модель OCR", "OCR model"));
        EditText transcription = host.createStyledInput(host.transcriptionModel(), tr(host, "Модель транскрибации", "Transcription model"));
        body.addView(ocr);
        body.addView(transcription);
        body.addView(host.text(tr(host,
                "OCR и транскрибация используют тот же Base URL и API key.",
                "OCR and transcription use the same Base URL and API key."), 12, host.secondaryText(), false));
        body.addView(host.spacer(14));
        Button save = settingsButton(host, host.str(R.string.str_save));
        save.setOnClickListener(v -> {
            String url = baseUrl.getText().toString().trim();
            host.aiProviderBaseUrl(url);
            host.aiProviderApiKey(apiKey.getText().toString().trim());
            host.aiProviderModel(chatModel.getText().toString().trim());
            host.ocrModel(ocr.getText().toString().trim());
            host.transcriptionModel(transcription.getText().toString().trim());
            host.providerApiKeys().put(url, host.aiProviderApiKey());
            host.editingAiProviderIdx(-1);
            host.savePersistentSettings();
            Toast.makeText(host, host.str(R.string.str_settings_saved), Toast.LENGTH_SHORT).show();
        });
        body.addView(save);
    }

    public static void showSubSettingsInterface(MainActivity host, LinearLayout body) {
        body.removeAllViews();
        body.addView(back(host));
        body.addView(host.spacer(14));
        body.addView(host.text(host.str(R.string.str_interface), 20, host.primaryText(), true));
        body.addView(host.spacer(14));
        body.addView(host.text(host.str(R.string.str_app_theme), 12, host.secondaryText(), true));
        body.addView(choiceRow(host, new String[]{host.str(R.string.str_theme_light), host.str(R.string.str_theme_dark), host.str(R.string.str_theme_system)}, host.themeMode(), choice -> {
            host.themeMode(choice);
            host.saveThemeMode();
            host.savePersistentSettings();
            host.applySystemBars();
            host.openInterfaceSettingsNext(true);
            host.setContentView(host.buildUi());
        }));
        body.addView(host.spacer(14));
        body.addView(host.text(host.str(R.string.str_language), 12, host.secondaryText(), true));
        body.addView(choiceRow(host, new String[]{host.str(R.string.str_russian), "English"}, "ru".equals(host.activeLanguage()) ? 0 : 1, choice -> {
            host.activeLanguage(choice == 0 ? "ru" : "en");
            host.updateLocalizedContext();
            host.savePersistentSettings();
            host.applySystemBars();
            host.setContentView(host.buildUi());
        }));
        body.addView(host.spacer(14));
        body.addView(host.text(host.str(R.string.str_text_size), 12, host.secondaryText(), true));
        body.addView(host.createStyledSlider(200, (int) (host.textSizeRatio() * 100), "%", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) { host.textSizeRatio(Math.max(0.7f, progress / 100f)); }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { host.savePersistentSettings(); showSubSettingsInterface(host, body); }
        }));
        body.addView(host.spacer(12));
        View bold = host.createStyledSwitch(tr(host, "Жирный текст", "Bold text"), host.boldTextEnabled());
        ((StyledToggleState) bold.getTag()).setOnCheckedChangeListener(checked -> {
            host.boldTextEnabled(checked);
            host.savePersistentSettings();
            host.openInterfaceSettingsNext(true);
            host.setContentView(host.buildUi());
        });
        body.addView(bold);
    }

    public static void showSubSettingsGraph(MainActivity host, LinearLayout body) {
        body.removeAllViews();
        body.addView(back(host));
        body.addView(host.spacer(14));
        body.addView(host.text(host.str(R.string.str_graph_visualization), 20, host.primaryText(), true));
        body.addView(host.spacer(14));
        body.addView(host.text(host.str(R.string.str_animation_smoothness), 12, host.secondaryText(), true));
        body.addView(choiceRow(host, new String[]{host.str(R.string.str_smoothness_low), host.str(R.string.str_smoothness_medium), host.str(R.string.str_smoothness_high)}, host.animationSmoothness(), choice -> {
            host.animationSmoothness(choice);
            host.savePersistentSettings();
            host.pushGraphDataToWebView();
            showSubSettingsGraph(host, body);
        }));
        body.addView(host.spacer(14));
        addGraphSwitch(host, body, host.str(R.string.str_smooth_zoom), host.smoothZoom(), checked -> host.smoothZoom(checked));
        addGraphSwitch(host, body, host.str(R.string.str_connection_animation), host.connectionAnimation(), checked -> host.connectionAnimation(checked));
        addGraphSwitch(host, body, host.str(R.string.str_node_glow), host.nodeGlow(), checked -> host.nodeGlow(checked));
        body.addView(host.spacer(8));
        body.addView(host.text(host.str(R.string.str_node_size) + String.format(Locale.US, " %.1fx", host.nodeSizeValue()), 12, host.secondaryText(), true));
        body.addView(host.createStyledSlider(200, (int) (host.nodeSizeValue() * 100), "x", graphSlider(host, value -> host.nodeSizeValue(Math.max(0.5f, value / 100f)))));
        body.addView(host.spacer(14));
        body.addView(host.text(host.str(R.string.str_connection_density) + String.format(Locale.US, " %.1fx", host.connectionDensity()), 12, host.secondaryText(), true));
        body.addView(host.createStyledSlider(200, (int) (host.connectionDensity() * 100), "x", graphSlider(host, value -> host.connectionDensity(Math.max(0.1f, value / 100f)))));
    }

    private interface ChoiceListener { void onChoice(int choice); }
    private interface CheckedListener { void onChecked(boolean checked); }
    private interface ProgressListener { void onProgress(int progress); }

    private static LinearLayout choiceRow(MainActivity host, String[] options, int selected, ChoiceListener listener) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < options.length; i++) {
            int choice = i;
            Button button = new Button(host);
            button.setText(options[i]);
            button.setTextSize(11);
            button.setAllCaps(false);
            boolean isSelected = selected == i;
            host.applyTextWeight(button, isSelected);
            button.setTextColor(isSelected ? host.accentColor() : host.primaryText());
            button.setBackground(host.roundedBg(host.surface(), 10, isSelected ? 2 : 1, isSelected ? host.accentColor() : host.strokeColor()));
            button.setOnClickListener(v -> listener.onChoice(choice));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            params.setMargins(host.dp(2), 0, host.dp(2), 0);
            row.addView(button, params);
        }
        return row;
    }

    private static void addGraphSwitch(MainActivity host, LinearLayout body, String label, boolean value, CheckedListener listener) {
        View toggle = host.createStyledSwitch(label, value);
        ((StyledToggleState) toggle.getTag()).setOnCheckedChangeListener(checked -> {
            listener.onChecked(checked);
            host.savePersistentSettings();
            host.pushGraphDataToWebView();
        });
        body.addView(toggle);
        body.addView(host.spacer(10));
    }

    private static SeekBar.OnSeekBarChangeListener graphSlider(MainActivity host, ProgressListener listener) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) { listener.onProgress(progress); host.pushGraphDataToWebView(); }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { host.savePersistentSettings(); }
        };
    }

    public static void showSubSettingsIO(MainActivity host, LinearLayout body) {
        body.removeAllViews();
        body.addView(back(host));
        body.addView(host.spacer(14));
        body.addView(host.text(host.str(R.string.str_import_export), 20, host.primaryText(), true));
        body.addView(host.spacer(14));
        Button create = settingsButton(host, tr(host, "Создать SQLAR бэкап", "Create SQLAR backup"));
        create.setOnClickListener(v -> host.createArchiveBackup());
        body.addView(create);
        body.addView(host.spacer(10));
        Button restore = settingsButton(host, tr(host, "Восстановить SQLAR бэкап", "Restore SQLAR backup"));
        restore.setOnClickListener(v -> host.openArchiveBackup());
        body.addView(restore);
        body.addView(host.spacer(14));
        View auto = host.createStyledSwitch(host.str(R.string.str_autosave), host.autosaveEnabled());
        ((StyledToggleState) auto.getTag()).setOnCheckedChangeListener(checked -> {
            host.autosaveEnabled(checked);
            host.savePersistentSettings();
            host.configureLocalAutoBackup();
        });
        auto.setBackground(host.roundedBg(host.surface(), 16, 1, host.strokeColor()));
        body.addView(auto);
        body.addView(host.spacer(10));
        body.addView(host.text(host.str(R.string.str_autosave_interval_min), 12, host.secondaryText(), true));
        body.addView(host.createStyledSlider(60, host.autosaveInterval(), "min", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) { host.autosaveInterval(Math.max(1, progress)); }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { host.savePersistentSettings(); host.configureLocalAutoBackup(); showSubSettingsIO(host, body); }
        }));
    }

    public static void showSubSettingsNotifications(MainActivity host, LinearLayout body) {
        body.removeAllViews();
        body.addView(back(host));
        body.addView(host.spacer(14));
        body.addView(host.text(host.str(R.string.str_notifications), 20, host.primaryText(), true));
        body.addView(host.spacer(14));
        LinearLayout card = host.card(host.surface());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(host.dp(14), host.dp(14), host.dp(14), host.dp(14));
        View reminders = host.createStyledSwitch(tr(host, "Уведомление для напоминания", "Reminder notification"), host.notifReminders());
        ((StyledToggleState) reminders.getTag()).setOnCheckedChangeListener(checked -> {
            host.notifReminders(checked);
            host.savePersistentSettings();
            if (checked) host.rescheduleActiveReminders();
        });
        card.addView(reminders);
        card.addView(host.spacer(8));
        card.addView(host.text(tr(host, "Включает системные уведомления только для напоминаний, заданных в заметках.", "Enables system notifications only for reminders set in notes."), 13, host.secondaryText(), false));
        body.addView(card);
    }

    public static void showSubSettingsAbout(MainActivity host, LinearLayout body) {
        body.removeAllViews();
        body.addView(back(host));
        body.addView(host.spacer(14));
        body.addView(host.text("MindScape", 24, host.primaryText(), true));
        body.addView(host.spacer(10));
        Button information = settingsButton(host, tr(host, "Информация", "Information"));
        information.setOnClickListener(v -> showAboutInformation(host));
        body.addView(information);
        body.addView(host.spacer(10));
        Button github = settingsButton(host, "GitHub");
        github.setOnClickListener(v -> host.startActivity(new android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://github.com/mindscape-app/mindscape")
        )));
        body.addView(github);
        body.addView(host.spacer(14));
        body.addView(host.text(
                tr(host, "Версия ", "Version ") + com.mindscape.app.BuildConfig.VERSION_NAME,
                14,
                host.secondaryText(),
                false
        ));
    }

    private static void showAboutInformation(MainActivity host) {
        LinearLayout content = new LinearLayout(host);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(host.dp(18), host.dp(16), host.dp(18), host.dp(10));
        content.addView(host.text("MindScape", 22, host.primaryText(), true));
        content.addView(host.spacer(8));
        content.addView(host.text(tr(host,
                "MindScape - локальное Android-приложение для карт знаний. Создавайте заметки, папки и связи, добавляйте файлы, ищите по материалам и сохраняйте резервные копии в формате SQLAR. AI подключается напрямую к выбранному вами совместимому провайдеру с вашими ключами.",
                "MindScape is a local Android app for knowledge maps. Create notes, folders, and links, add files, search your material, and save SQLAR backups. AI connects directly to a compatible provider you choose with your own credentials."),
                14,
                host.secondaryText(),
                false));
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(host)
                .setView(host.scrollableDialogContent(content, 520))
                .setPositiveButton(host.str(R.string.str_close), null)
                .create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(host.roundedBg(host.surface(), 16, 0, Color.TRANSPARENT));
        }
    }
}
