package com.mindscape.app.ui;

import android.app.AlertDialog;
import android.graphics.Color;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.mindscape.app.MainActivity;
import com.mindscape.app.R;

public class CoreDialogs {

    public static void showVisibilityQuickDialog(MainActivity host, Object entity) {
        boolean hidden = host.isNodeHidden(entity);
        LinearLayout layout = new LinearLayout(host);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(host.dp(18), host.dp(12), host.dp(18), host.dp(8));

        final AlertDialog[] dialogRef = new AlertDialog[1];
        layout.addView(host.actionDialogRow(
                hidden ? host.str(R.string.str_menu_show_on_map) : host.str(R.string.str_menu_hide_from_map),
                hidden ? host.str(R.string.str_menu_show_on_map_sub) : host.str(R.string.str_menu_hide_from_map_sub),
                hidden ? "ic_eye" : "ic_eye_off",
                v -> {
                    if (dialogRef[0] != null) dialogRef[0].dismiss();
                    host.toggleNodeHidden(entity);
                    host.pushGraphDataToWebView();
                    host.renderContent();
                }));

        AlertDialog dialog = new AlertDialog.Builder(host)
                .setTitle(host.entityTitle(entity))
                .setView(layout)
                .setNegativeButton(host.str(R.string.str_close), null)
                .create();
        dialogRef[0] = dialog;
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(host.roundedBg(host.surface(), 16, 0, Color.TRANSPARENT));
        }
    }

    public static void showStyledChoiceDialog(MainActivity host, String title, String[] items, String iconName, MainActivity.StyledChoiceHandler handler) {
        LinearLayout layout = new LinearLayout(host);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(host.dp(20), host.dp(12), host.dp(20), host.dp(8));

        TextView header = host.text(title, 18, host.primaryText(), true);
        layout.addView(header);
        layout.addView(host.spacer(8));

        AlertDialog[] dialogRef = new AlertDialog[1];
        for (int i = 0; i < items.length; i++) {
            final int index = i;
            layout.addView(host.actionDialogRow(items[i], "", iconName, v -> {
                if (dialogRef[0] != null) {
                    dialogRef[0].dismiss();
                }
                handler.onChoice(index);
            }));
        }

        AlertDialog dialog = new AlertDialog.Builder(host)
                .setView(host.scrollableDialogContent(layout, 520))
                .create();
        dialogRef[0] = dialog;
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(host.roundedBg(host.surface(), 16, 0, Color.TRANSPARENT));
        }
    }

    public static void showFileTypeFilterDialog(MainActivity host) {
        final String[] keys = {"all", "notes", "files", "pdf", "word", "sheets", "presentations", "images", "other"};
        final String[] labels = {"Все", "Только заметки", "Все файлы", "PDF", "Word", "Таблицы", "Презентации", "Изображения", "Другие форматы"};
        LinearLayout layout = new LinearLayout(host);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(host.dp(12), host.dp(10), host.dp(12), host.dp(10));
        final AlertDialog[] dialogRef = new AlertDialog[1];
        for (int i = 0; i < keys.length; i++) {
            final String key = keys[i];
            TextView row = host.text(labels[i], 15, host.primaryText(), key.equals(host.activeFileTypeFilter));
            row.setPadding(host.dp(14), host.dp(12), host.dp(14), host.dp(12));
            row.setBackground(host.roundedBg(key.equals(host.activeFileTypeFilter) ? Color.rgb(235, 241, 255) : Color.TRANSPARENT, 10, key.equals(host.activeFileTypeFilter) ? 1 : 0, host.accentColor()));
            row.setOnClickListener(v -> {
                host.activeFileTypeFilter = key;
                if (dialogRef[0] != null) dialogRef[0].dismiss();
                host.renderContent();
            });
            layout.addView(row);
        }
        AlertDialog dialog = new AlertDialog.Builder(host)
                .setTitle("Фильтр файлов")
                .setView(layout)
                .setNegativeButton(host.str(R.string.str_cancel), null)
                .create();
        dialogRef[0] = dialog;
        dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(host.roundedBg(host.surface(), 16, 0, Color.TRANSPARENT));
    }
}
