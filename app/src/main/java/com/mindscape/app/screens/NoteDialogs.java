package com.mindscape.app.screens;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.mindscape.app.Connection;
import com.mindscape.app.MainActivity;
import com.mindscape.app.Note;
import com.mindscape.app.R;
import com.mindscape.app.ui.StyledToggleState;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;

import androidx.annotation.Nullable;

public final class NoteDialogs {
    private NoteDialogs() {}

    public interface LocationSelected {
        void onSelected(String path);
    }

    public static View reminderEditor(MainActivity host, MainActivity.ReminderDraft draft) {
        LinearLayout container = host.card(host.surface());
        container.setPadding(host.dp(14), host.dp(12), host.dp(14), host.dp(12));

        TextView timeLabel = host.text(reminderTimeLabel(host, draft), 13, host.secondaryText(), false);

        LinearLayout settings = new LinearLayout(host);
        settings.setOrientation(LinearLayout.VERTICAL);
        settings.setVisibility(draft.enabled ? View.VISIBLE : View.GONE);

        View enabledSwitchView = host.createStyledSwitch(host.str(R.string.str_reminder), draft.enabled);
        StyledToggleState enabledSwitch = (StyledToggleState) enabledSwitchView.getTag();
        enabledSwitch.setOnCheckedChangeListener(isChecked -> {
            draft.enabled = isChecked;
            settings.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            timeLabel.setText(reminderTimeLabel(host, draft));
        });
        container.addView(enabledSwitchView);

        settings.addView(labelValueRow(host, host.str(R.string.str_reminder_time), timeLabel));
        Button timeButton = host.createStyledButton(host.str(R.string.str_pick_reminder_time));
        timeButton.setOnClickListener(v -> showReminderDateTimePicker(host, draft, timeLabel));
        settings.addView(timeButton);

        container.addView(settings);
        return container;
    }

    public static View labelValueRow(MainActivity host, String label, TextView value) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, host.dp(8), 0, host.dp(4));
        row.addView(host.text(label, 12, host.secondaryText(), false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(value);
        return row;
    }

    public static void showReminderDateTimePicker(MainActivity host, MainActivity.ReminderDraft draft, TextView timeLabel) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(draft.at > 0 ? draft.at : MainActivity.defaultReminderTime());

        new android.app.DatePickerDialog(host, (view, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            new android.app.TimePickerDialog(host, (tv, hour, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                draft.at = calendar.getTimeInMillis();
                timeLabel.setText(reminderTimeLabel(host, draft));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    public static String reminderTimeLabel(MainActivity host, MainActivity.ReminderDraft draft) {
        if (!draft.enabled) return host.str(R.string.str_reminder_disabled);
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        return dateFormat.format(new java.util.Date(draft.at));
    }

    public static void applyReminderDraft(Note note, MainActivity.ReminderDraft draft) {
        note.reminderEnabled = draft.enabled;
        note.reminderAt = draft.enabled ? draft.at : 0L;
        note.reminderTriggered = false;
    }

    public static void showCreateStructureNoteDialog(MainActivity host, @Nullable String folderPath) {
        LinearLayout layout = new LinearLayout(host);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(host.dp(20), host.dp(12), host.dp(20), host.dp(4));
        EditText title = host.createStyledInput("", host.str(R.string.str_note_title));
        EditText content = host.createStyledInput("", host.str(R.string.str_note_text_1));
        host.configureLargeTextInput(content, 150);
        layout.addView(title);
        layout.addView(content);
        MainActivity.ReminderDraft reminderDraft = new MainActivity.ReminderDraft();
        layout.addView(reminderEditor(host, reminderDraft));

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(host)
                .setTitle(host.str(R.string.str_new_note))
                .setView(host.scrollableDialogContent(layout, 520))
                .setPositiveButton(host.str(R.string.str_create), (d, w) -> {
                    String t = title.getText().toString().trim();
                    if (!t.isEmpty()) {
                        if (host.noteExists(t, folderPath)) {
                            Toast.makeText(host, host.str(R.string.str_note_already_exists), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (reminderDraft.enabled && reminderDraft.at <= System.currentTimeMillis()) {
                            Toast.makeText(host, host.str(R.string.str_reminder_time_invalid), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Note note = new Note(t, folderPath, content.getText().toString().trim());
                        applyReminderDraft(note, reminderDraft);
                        host.notesList().add(note);
                        host.scheduleNoteReminder(note);
                        host.renderContent();
                    }
                })
                .setNegativeButton(host.str(R.string.str_cancel), null)
                .create();
        dialog.getWindow().setBackgroundDrawable(host.roundedBg(host.surface(), 16, 0, Color.TRANSPARENT));
        dialog.show();
    }

    public static void createNoteDialog(MainActivity host) {
        LinearLayout layout = new LinearLayout(host);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(host.dp(20), host.dp(20), host.dp(20), host.dp(20));

        layout.addView(host.text(host.str(R.string.str_new_note), 18, host.primaryText(), true));
        layout.addView(host.spacer(8));

        EditText titleInput = host.createStyledInput("", host.str(R.string.str_note_title));
        layout.addView(titleInput);

        EditText contentInput = host.createStyledInput("", host.str(R.string.str_note_text_1));
        host.configureLargeTextInput(contentInput, 160);
        layout.addView(contentInput);
        MainActivity.ReminderDraft reminderDraft = new MainActivity.ReminderDraft();
        layout.addView(reminderEditor(host, reminderDraft));

        layout.addView(host.spacer(6));
        layout.addView(host.text(host.str(R.string.str_bind_to_category_optional), 13, host.secondaryText(), false));
        layout.addView(host.spacer(4));

        List<String> paths = host.getAllCategoryPaths();
        if (paths.isEmpty()) {
            layout.addView(host.text(host.str(R.string.str_create_categories_first), 12, Color.rgb(200, 80, 80), false));
        }

        ScrollView catScroll = new ScrollView(host);
        LinearLayout catList = new LinearLayout(host);
        catList.setOrientation(LinearLayout.VERTICAL);
        final String[] selectedPath = {null};

        TextView noBindRow = host.text(host.str(R.string.str_no_category_send_to_container), 13, host.primaryText(), true);
        noBindRow.setPadding(host.dp(10), host.dp(8), host.dp(10), host.dp(8));
        noBindRow.setBackground(host.roundedBg(Color.rgb(235, 244, 255), 10, 1, host.accentColor()));
        noBindRow.setOnClickListener(v -> {
            selectedPath[0] = null;
            host.refreshCategoryPickerSelection(catList, null);
            host.applyTextWeight(noBindRow, true);
            noBindRow.setBackground(host.roundedBg(Color.rgb(235, 244, 255), 10, 1, host.accentColor()));
        });
        catList.addView(noBindRow);

        for (int i = 0; i < paths.size(); i++) {
            final String path = paths.get(i);
            LinearLayout row = new LinearLayout(host);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(host.dp(10), host.dp(8), host.dp(10), host.dp(8));
            row.setBackground(host.roundedBg(Color.TRANSPARENT, 10, 0, Color.TRANSPARENT));
            row.setTag(path);
            row.addView(host.text(path.replace("/", " > "), 13, host.primaryText(), false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.setOnClickListener(v -> {
                selectedPath[0] = path;
                host.applyTextWeight(noBindRow, false);
                noBindRow.setBackground(host.roundedBg(Color.TRANSPARENT, 10, 0, Color.TRANSPARENT));
                for (int j = 0; j < catList.getChildCount(); j++) {
                    View child = catList.getChildAt(j);
                    Object tag = child.getTag();
                    if (!(tag instanceof String)) continue;
                    String p = (String) tag;
                    boolean sel = p.equals(path);
                    child.setBackground(host.roundedBg(sel ? Color.rgb(235, 244, 255) : Color.TRANSPARENT, 10, sel ? 1 : 0, sel ? host.accentColor() : Color.TRANSPARENT));
                    if (child instanceof LinearLayout) {
                        for (int k = 0; k < ((LinearLayout) child).getChildCount(); k++) {
                            View inner = ((LinearLayout) child).getChildAt(k);
                            if (inner instanceof TextView) {
                                host.applyTextWeight((TextView) inner, sel);
                            }
                        }
                    }
                }
            });
            catList.addView(row);
        }
        catScroll.addView(catList);
        layout.addView(catScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, host.dp(180)));

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(host);
        builder.setView(host.scrollableDialogContent(layout, 620));
        builder.setPositiveButton(host.str(R.string.str_create), (dialog, which) -> {
            String title = titleInput.getText().toString().trim();
            String content = contentInput.getText().toString().trim();
            if (!title.isEmpty()) {
                if (host.noteExists(title, selectedPath[0])) {
                    Toast.makeText(host, host.str(R.string.str_note_already_exists), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (reminderDraft.enabled && reminderDraft.at <= System.currentTimeMillis()) {
                    Toast.makeText(host, host.str(R.string.str_reminder_time_invalid), Toast.LENGTH_SHORT).show();
                    return;
                }
                Note note = new Note(title, selectedPath[0], content.isEmpty() ? host.str(R.string.str_note_text_1) : content);
                applyReminderDraft(note, reminderDraft);
                host.notesList().add(note);
                host.scheduleNoteReminder(note);
                host.renderContent();
            } else {
                Toast.makeText(host, host.str(R.string.str_enter_note_title), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(host.str(R.string.str_cancel), null);

        android.app.AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(host.roundedBg(host.surface(), 16, 0, Color.TRANSPARENT));
        dialog.show();
    }

    public static void editNoteDialog(MainActivity host, Note note) {
        LinearLayout layout = new LinearLayout(host);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(host.dp(20), host.dp(20), host.dp(20), host.dp(20));

        layout.addView(host.text(host.str(R.string.str_edit_note), 18, host.primaryText(), true));
        layout.addView(host.spacer(10));

        EditText titleInput = host.createStyledInput(note.title, host.str(R.string.str_title));
        layout.addView(titleInput);

        EditText contentInput = host.createStyledInput(note.content, host.str(R.string.str_content));
        host.configureLargeTextInput(contentInput, 180);
        layout.addView(contentInput);
        MainActivity.ReminderDraft reminderDraft = new MainActivity.ReminderDraft(note);
        layout.addView(reminderEditor(host, reminderDraft));

        layout.addView(host.spacer(4));
        layout.addView(host.text(host.str(R.string.str_category_path), 12, host.secondaryText(), false));
        final String[] editSelectedPath = {note.categoryPath};
        TextView editPathLabel = host.text(note.categoryPath != null ? note.categoryPath.replace("/", " > ") : "-", 12, host.secondaryText(), false);
        editPathLabel.setPadding(0, host.dp(4), 0, host.dp(4));
        layout.addView(editPathLabel);

        Button pickEditCat = host.createStyledButton(host.str(R.string.str_change_category));
        pickEditCat.setOnClickListener(v -> {
            List<String> paths = host.getAllCategoryPaths();
            if (paths.isEmpty()) {
                Toast.makeText(host, host.str(R.string.str_no_categories), Toast.LENGTH_SHORT).show();
                return;
            }
            showLocationPickerDialog(host, paths, editSelectedPath[0], selected -> {
                editSelectedPath[0] = selected;
                editPathLabel.setText(selected.replace("/", " > "));
            });
        });
        layout.addView(pickEditCat);
        layout.addView(host.spacer(4));

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(host);
        builder.setView(host.scrollableDialogContent(layout, 640));
        builder.setPositiveButton(host.str(R.string.str_save), (dialog, which) -> {
            String title = titleInput.getText().toString().trim();
            String content = contentInput.getText().toString().trim();
            if (!title.isEmpty()) {
                if (reminderDraft.enabled && reminderDraft.at <= System.currentTimeMillis()) {
                    Toast.makeText(host, host.str(R.string.str_reminder_time_invalid), Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean pathChanged = (note.categoryPath == null && editSelectedPath[0] != null) ||
                                      (note.categoryPath != null && !note.categoryPath.equals(editSelectedPath[0]));
                boolean titleChanged = !note.title.equalsIgnoreCase(title);

                if (titleChanged || pathChanged) {
                    if (host.noteExists(title, editSelectedPath[0])) {
                        Toast.makeText(host, host.str(R.string.str_note_already_exists), Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                String oldPath = note.fullPath();
                host.cancelNoteReminder(oldPath);
                note.title = title;
                note.categoryPath = editSelectedPath[0];
                note.quickContainerNote = editSelectedPath[0] == null || editSelectedPath[0].trim().isEmpty() ? note.quickContainerNote : false;
                note.content = content;
                note.updatedAt = System.currentTimeMillis();
                applyReminderDraft(note, reminderDraft);

                String newPath = note.fullPath();
                if (!oldPath.equalsIgnoreCase(newPath)) {
                    String oldKey = "note:" + oldPath;
                    String newKey = "note:" + newPath;
                    for (Connection conn : host.connectionsList()) {
                        if (conn.source.equalsIgnoreCase(oldKey)) conn.source = newKey;
                        else if (conn.source.equalsIgnoreCase(oldPath)) conn.source = newPath;
                        if (conn.target.equalsIgnoreCase(oldKey)) conn.target = newKey;
                        else if (conn.target.equalsIgnoreCase(oldPath)) conn.target = newPath;
                    }
                    if (host.hiddenNodes().contains(oldKey)) { host.hiddenNodes().remove(oldKey); host.hiddenNodes().add(newKey); }
                    if (host.hiddenNodes().contains(oldPath)) { host.hiddenNodes().remove(oldPath); host.hiddenNodes().add(newPath); }
                }
                host.scheduleNoteReminder(note);
                host.renderContent();
            }
        });
        builder.setNegativeButton(host.str(R.string.str_delete), (dialog, which) -> {
            host.cancelNoteReminder(note.fullPath());
            host.deleteNotesSilent(java.util.Collections.singletonList(note));
            host.selectedMapEntity(null);
            host.renderContent();
        });
        builder.setNeutralButton(host.str(R.string.str_cancel), null);

        android.app.AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(host.roundedBg(host.surface(), 16, 0, Color.TRANSPARENT));
        dialog.show();
    }

    public static void showLocationPickerDialog(MainActivity host, List<String> paths, @Nullable String currentPath, LocationSelected callback) {
        LinearLayout layout = new LinearLayout(host);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(host.dp(18), host.dp(14), host.dp(18), host.dp(8));
        layout.addView(host.text(host.str(R.string.str_select_category), 18, host.primaryText(), true));
        layout.addView(host.spacer(8));

        final android.app.AlertDialog[] dialogRef = new android.app.AlertDialog[1];
        for (String path : paths) {
            TextView row = host.text(path.replace("/", " > "), 14, host.primaryText(), path.equals(currentPath));
            row.setPadding(host.dp(12), host.dp(10), host.dp(12), host.dp(10));
            row.setBackground(host.roundedBg(path.equals(currentPath) ? Color.rgb(235, 241, 255) : Color.TRANSPARENT, 10, path.equals(currentPath) ? 1 : 0, host.accentColor()));
            row.setOnClickListener(v -> {
                if (dialogRef[0] != null) dialogRef[0].dismiss();
                callback.onSelected(path);
            });
            layout.addView(row);
            View divider = new View(host);
            divider.setBackgroundColor(host.strokeColor());
            layout.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, host.dp(1)));
        }

        Button back = host.createStyledButton(host.str(R.string.str_back));
        back.setOnClickListener(v -> {
            if (dialogRef[0] != null) dialogRef[0].dismiss();
        });
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, host.dp(48));
        backParams.setMargins(0, host.dp(10), 0, 0);
        layout.addView(back, backParams);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(host)
                .setView(host.scrollableDialogContent(layout, 520))
                .create();
        dialogRef[0] = dialog;
        dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(host.roundedBg(host.surface(), 16, 0, Color.TRANSPARENT));
    }
}
