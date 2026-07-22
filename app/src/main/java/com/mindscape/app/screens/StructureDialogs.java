package com.mindscape.app.screens;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.mindscape.app.Category;
import com.mindscape.app.Connection;
import com.mindscape.app.MainActivity;
import com.mindscape.app.Note;
import com.mindscape.app.R;

import androidx.annotation.Nullable;

public final class StructureDialogs {
    private StructureDialogs() {}

    public static void showAddContentDialog(MainActivity host, @Nullable String folderPath) {
        LinearLayout layout = new LinearLayout(host);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(host.dp(18), host.dp(12), host.dp(18), host.dp(8));

        final android.app.AlertDialog[] dialogRef = new android.app.AlertDialog[1];

        if (folderPath != null) {
            layout.addView(host.actionDialogRow(host.str(R.string.str_menu_folder), host.str(R.string.str_menu_folder_sub), "ic_create_category", v -> {
                dismiss(dialogRef);
                host.showCreateFolderDialog(folderPath);
            }));
            layout.addView(host.actionDialogRow(host.str(R.string.str_menu_note), host.str(R.string.str_menu_note_sub), "ic_create_note", v -> {
                dismiss(dialogRef);
                host.showCreateStructureNoteDialog(folderPath);
            }));
            boolean ru = "ru".equals(host.activeLanguage());
            layout.addView(host.actionDialogRow(
                    ru ? "Добавить из контейнера" : "Add from container",
                    ru ? "Перенести быструю заметку сюда" : "Move a quick note here",
                    "ic_container", Color.rgb(65, 120, 220), v -> {
                dismiss(dialogRef);
                host.showAddFromContainerDialog(folderPath);
            }));
            layout.addView(host.actionDialogRow(host.str(R.string.str_menu_local_file), host.str(R.string.str_menu_local_file_sub), "ic_local_file", v -> {
                dismiss(dialogRef);
                host.pickLocalFile(folderPath);
            }));
        } else {
            layout.addView(host.actionDialogRow(host.str(R.string.str_create_center), host.str(R.string.str_create_center_sub), "ic_person_root", v -> {
                dismiss(dialogRef);
                host.showCreateCenterDialog();
            }));
        }

        showDialog(host, dialogRef, folderPath, layout);
    }

    public static void showFolderActionsDialog(MainActivity host, @Nullable String folderPath) {
        LinearLayout layout = new LinearLayout(host);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(host.dp(18), host.dp(12), host.dp(18), host.dp(8));

        final android.app.AlertDialog[] dialogRef = new android.app.AlertDialog[1];

        if (folderPath != null) {
            boolean isCenter = false;
            Category currentCat = null;
            for (Category c : host.categoriesList()) {
                if (c.fullPath().equals(folderPath)) {
                    isCenter = c.isCenter;
                    currentCat = c;
                    break;
                }
            }

            if (isCenter) {
                boolean ru = "ru".equals(host.activeLanguage());
                layout.addView(host.actionDialogRow(
                        ru ? "Открыть содержимое" : "Open contents",
                        ru ? "Перейти внутрь папки" : "Open this folder",
                        "ic_chevron_right", v -> {
                    dismiss(dialogRef);
                    host.currentStructurePath(folderPath);
                    host.renderContent();
                }));
                layout.addView(host.actionDialogRow(host.str(R.string.str_menu_rename), host.str(R.string.str_menu_rename_folder_sub), "ic_wrench", v -> {
                    dismiss(dialogRef);
                    host.showRenameFolderDialog(folderPath);
                }));

                final Category finalCenter = currentCat;
                layout.addView(host.actionDialogRow(
                        "ru".equals(host.activeLanguage()) ? "Создать копию центра" : "Create center copy",
                        host.str(R.string.str_menu_duplicate_sub),
                        "ic_create_category", v -> {
                    dismiss(dialogRef);
                    host.duplicateCenter(finalCenter);
                }));

                layout.addView(host.actionDialogRow(host.str(R.string.str_menu_delete_center), host.str(R.string.str_menu_delete_center_sub), "ic_trash", v -> {
                    dismiss(dialogRef);
                    host.deleteFolder(folderPath);
                }));
            } else {
                layout.addView(host.actionDialogRow(host.str(R.string.str_menu_folder), host.str(R.string.str_menu_folder_sub), "ic_create_category", v -> {
                    dismiss(dialogRef);
                    host.showCreateFolderDialog(folderPath);
                }));
                boolean ru = "ru".equals(host.activeLanguage());
                layout.addView(host.actionDialogRow(
                        ru ? "Открыть содержимое" : "Open contents",
                        ru ? "Перейти внутрь папки" : "Open this folder",
                        "ic_chevron_right", v -> {
                    dismiss(dialogRef);
                    host.currentStructurePath(folderPath);
                    host.renderContent();
                }));
                layout.addView(host.actionDialogRow(host.str(R.string.str_menu_note), host.str(R.string.str_menu_note_sub), "ic_create_note", v -> {
                    dismiss(dialogRef);
                    host.showCreateStructureNoteDialog(folderPath);
                }));
                layout.addView(host.actionDialogRow(host.str(R.string.str_menu_local_file), host.str(R.string.str_menu_local_file_sub), "ic_local_file", v -> {
                    dismiss(dialogRef);
                    host.pickLocalFile(folderPath);
                }));
                layout.addView(host.actionDialogRow(host.str(R.string.str_connections), host.str(R.string.str_menu_connections_sub), "ic_node_link", v -> {
                    dismiss(dialogRef);
                    host.showConnectionsDialog("folder:" + folderPath, true);
                }));
                layout.addView(host.actionDialogRow(host.str(R.string.str_menu_rename), host.str(R.string.str_menu_rename_folder_sub), "ic_wrench", v -> {
                    dismiss(dialogRef);
                    host.showRenameFolderDialog(folderPath);
                }));
                boolean folderHidden = host.isNodeHidden(folderPath);
                layout.addView(host.actionDialogRow(
                        folderHidden ? host.str(R.string.str_menu_show_on_map) : host.str(R.string.str_menu_hide_from_map),
                        folderHidden ? host.str(R.string.str_menu_show_on_map_sub) : host.str(R.string.str_menu_hide_from_map_sub),
                        folderHidden ? "ic_eye" : "ic_eye_off", v -> {
                            dismiss(dialogRef);
                            host.toggleNodeHidden(folderPath);
                            host.pushGraphDataToWebView();
                            host.renderContent();
                        }));
                layout.addView(host.actionDialogRow(host.str(R.string.str_menu_delete_folder), host.str(R.string.str_menu_delete_folder_sub), "ic_trash", v -> {
                    dismiss(dialogRef);
                    host.deleteFolder(folderPath);
                }));
            }
        } else {
            layout.addView(host.actionDialogRow(host.str(R.string.str_create_center), host.str(R.string.str_create_center_sub), "ic_person_root", v -> {
                dismiss(dialogRef);
                host.showCreateCenterDialog();
            }));
            layout.addView(host.actionDialogRow(host.str(R.string.str_import_json), host.str(R.string.str_menu_import_sub), "ic_io", v -> {
                dismiss(dialogRef);
                host.importDialog();
            }));
        }

        showDialog(host, dialogRef, folderPath, layout);
    }

    public static void showCreateFolderDialog(MainActivity host, @Nullable String parent) {
        EditText input = host.createStyledInput("", host.str(R.string.str_category_title));
        LinearLayout container = new LinearLayout(host);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(host.dp(20), host.dp(18), host.dp(20), host.dp(4));
        container.addView(dialogTitle(host, host.str(R.string.str_menu_new_folder)));
        container.addView(host.spacer(14));
        container.addView(input);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(host)
                .setView(container)
                .setPositiveButton(host.str(R.string.str_create), (d, w) -> {
                    String title = input.getText().toString().trim();
                    if (!title.isEmpty()) {
                        if (host.categoryExists(title, parent)) {
                            Toast.makeText(host, host.str(R.string.str_category_already_exists), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Category created = new Category(title, "", Color.rgb(65, 120, 220), parent);
                        host.categoriesList().add(created);
                        if (parent != null) host.expandedFolders().add(parent);
                        host.expandedFolders().add(created.fullPath());
                        host.renderContent();
                    }
                })
                .setNegativeButton(host.str(R.string.str_cancel), null)
                .create();
        dialog.getWindow().setBackgroundDrawable(host.roundedBg(host.surface(), 16, 0, Color.TRANSPARENT));
        dialog.show();
    }

    public static void showCreateCenterDialog(MainActivity host) {
        EditText input = host.createStyledInput("", host.str(R.string.str_center_title));
        LinearLayout container = new LinearLayout(host);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(host.dp(20), host.dp(18), host.dp(20), host.dp(4));
        container.addView(dialogTitle(host, host.str(R.string.str_create_center)));
        container.addView(host.spacer(14));
        container.addView(input);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(host)
                .setView(container)
                .setPositiveButton(host.str(R.string.str_create), (d, w) -> {
                    String title = input.getText().toString().trim();
                    if (title.isEmpty()) return;
                    if (host.categoryExists(title, null)) {
                        Toast.makeText(host, host.str(R.string.str_category_already_exists), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Category center = new Category(title, "", Color.rgb(65, 120, 220), null, true);
                    host.categoriesList().add(center);
                    host.expandedFolders().add(center.fullPath());
                    host.pushGraphDataToWebView();
                    host.renderContent();
                    Toast.makeText(host, host.str(R.string.str_center_created), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(host.str(R.string.str_cancel), null)
                .create();
        dialog.getWindow().setBackgroundDrawable(host.roundedBg(host.surface(), 16, 0, Color.TRANSPARENT));
        dialog.show();
    }

    private static TextView dialogTitle(MainActivity host, String title) {
        TextView view = host.text(title, 20, host.primaryText(), true);
        view.setGravity(Gravity.START);
        view.setPadding(host.dp(24), host.dp(20), host.dp(24), host.dp(8));
        return view;
    }

    public static void showNoteActionsDialog(MainActivity host, Note note) {
        LinearLayout layout = new LinearLayout(host);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(host.dp(18), host.dp(12), host.dp(18), host.dp(8));

        final android.app.AlertDialog[] dialogRef = new android.app.AlertDialog[1];

        layout.addView(host.actionDialogRow(host.str(R.string.str_menu_open_note), host.str(R.string.str_menu_open_note_sub), "ic_create_note", v -> {
            dismiss(dialogRef);
            host.editNoteDialog(note);
        }));

        layout.addView(host.actionDialogRow(host.str(R.string.str_connections), host.str(R.string.str_menu_connections_note_sub), "ic_node_link", v -> {
            dismiss(dialogRef);
            host.showConnectionsDialog("note:" + note.fullPath(), false);
        }));
        layout.addView(host.actionDialogRow(host.str(R.string.str_menu_move), host.str(R.string.str_menu_move_sub), "ic_create_category", v -> {
            dismiss(dialogRef);
            host.showMoveNoteDialog(note);
        }));
        layout.addView(host.actionDialogRow(host.str(R.string.str_menu_duplicate), host.str(R.string.str_menu_duplicate_sub), "ic_create_template", v -> {
            dismiss(dialogRef);
            Note copy = new Note(host.uniqueNoteCopyTitle(note.title, note.categoryPath), note.categoryPath, note.content);
            host.notesList().add(copy);
            host.connectionsList().add(new Connection("note:" + note.fullPath(), "note:" + copy.fullPath()));
            host.savePersistentData();
            host.pushGraphDataToWebView();
            host.renderContent();
        }));
        boolean noteHid = host.isNodeHidden(note);
        layout.addView(host.actionDialogRow(
                noteHid ? host.str(R.string.str_menu_show_on_map) : host.str(R.string.str_menu_hide_from_map),
                noteHid ? host.str(R.string.str_menu_show_note_sub) : host.str(R.string.str_menu_hide_note_sub),
                noteHid ? "ic_eye" : "ic_eye_off", v -> {
                    dismiss(dialogRef);
                    host.toggleNodeHidden(note);
                    host.pushGraphDataToWebView();
                    host.renderContent();
                }));
        layout.addView(host.actionDialogRow(host.str(R.string.str_delete), host.str(R.string.str_menu_delete_note_sub), "ic_trash", v -> {
            dismiss(dialogRef);
            host.deleteNote(note);
            host.selectedNotes().clear();
            host.renderContent();
        }));

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(host)
                .setTitle(note.title)
                .setView(layout)
                .setNegativeButton(host.str(R.string.str_close), null)
                .create();
        dialogRef[0] = dialog;
        dialog.getWindow().setBackgroundDrawable(host.roundedBg(host.surface(), 16, 0, Color.TRANSPARENT));
        dialog.show();
    }

    public static void showConnectionsDialog(MainActivity host, String sourceId, boolean isFolder) {
        final String normalizedSourceId = host.normalizeConnectionNodeId(sourceId);
        LinearLayout layout = new LinearLayout(host);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(host.dp(18), host.dp(12), host.dp(18), host.dp(8));

        final android.app.AlertDialog[] dialogRef = new android.app.AlertDialog[1];

        boolean hasConnections = false;
        for (Connection connection : host.connectionsList()) {
            String connSource = host.normalizeConnectionNodeId(connection.source);
            String connTarget = host.normalizeConnectionNodeId(connection.target);
            if (connSource.equalsIgnoreCase(normalizedSourceId) || connTarget.equalsIgnoreCase(normalizedSourceId)) {
                hasConnections = true;
                String otherId = connSource.equalsIgnoreCase(normalizedSourceId) ? connTarget : connSource;
                String direction = connSource.equalsIgnoreCase(normalizedSourceId) ? "→" : "←";
                String otherDisplayName = host.displayNodeId(otherId);

                LinearLayout row = new LinearLayout(host);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(host.dp(8), host.dp(12), host.dp(8), host.dp(12));

                android.widget.ImageView icon = new android.widget.ImageView(host);
                icon.setImageResource(host.getResources().getIdentifier("ic_node_link", "drawable", host.getPackageName()));
                icon.setColorFilter(host.secondaryText());
                row.addView(icon, new LinearLayout.LayoutParams(host.dp(24), host.dp(24)));

                TextView titleView = host.text("  " + direction + " " + otherDisplayName, 15, host.primaryText(), false);
                row.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                final Connection connToDelete = connection;
                android.widget.ImageView trashBtn = new android.widget.ImageView(host);
                trashBtn.setImageResource(host.getResources().getIdentifier("ic_trash", "drawable", host.getPackageName()));
                trashBtn.setColorFilter(Color.rgb(200, 80, 80));
                trashBtn.setPadding(host.dp(8), host.dp(4), host.dp(4), host.dp(4));
                trashBtn.setOnClickListener(v -> {
                    String srcDisplay = host.displayNodeId(normalizedSourceId);
                    String otherDisplay = host.displayNodeId(otherId);
                    String srcName = srcDisplay.contains(" > ") ? srcDisplay.substring(srcDisplay.lastIndexOf(" > ") + 3) : srcDisplay;
                    String otherName = otherDisplay.contains(" > ") ? otherDisplay.substring(otherDisplay.lastIndexOf(" > ") + 3) : otherDisplay;
                    new android.app.AlertDialog.Builder(host)
                            .setTitle(host.str(R.string.str_delete_connection_title))
                            .setMessage(srcName + " ↔ " + otherName)
                            .setPositiveButton(host.str(R.string.str_delete), (d2, w) -> {
                                host.connectionsList().remove(connToDelete);
                                host.savePersistentData();
                                host.pushGraphDataToWebView();
                                dismiss(dialogRef);
                                showConnectionsDialog(host, normalizedSourceId, isFolder);
                            })
                            .setNegativeButton(host.str(R.string.str_cancel), null)
                            .show();
                });
                row.addView(trashBtn, new LinearLayout.LayoutParams(host.dp(36), host.dp(36)));

                layout.addView(row);

                View divider = new View(host);
                divider.setBackgroundColor(Color.argb(30, 150, 150, 150));
                layout.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, host.dp(1)));
            }
        }

        if (!hasConnections) {
            TextView empty = host.text(host.str(R.string.str_no_connections_yet), 14, host.secondaryText(), false);
            empty.setPadding(host.dp(8), host.dp(8), host.dp(8), host.dp(16));
            layout.addView(empty);
        }

        LinearLayout addRow = new LinearLayout(host);
        addRow.setOrientation(LinearLayout.HORIZONTAL);
        addRow.setGravity(Gravity.CENTER_VERTICAL);
        addRow.setPadding(host.dp(8), host.dp(16), host.dp(8), host.dp(12));

        android.widget.ImageView plus = new android.widget.ImageView(host);
        plus.setImageResource(host.getResources().getIdentifier("ic_nav_create", "drawable", host.getPackageName()));
        plus.setColorFilter(Color.rgb(65, 120, 220));
        addRow.addView(plus, new LinearLayout.LayoutParams(host.dp(24), host.dp(24)));

        addRow.addView(host.text("  " + host.str(R.string.str_create_connection), 15, Color.rgb(65, 120, 220), true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        addRow.setOnClickListener(v -> {
            dismiss(dialogRef);
            host.showLinkDialog(normalizedSourceId, isFolder);
        });
        layout.addView(addRow);

        ScrollView scroll = new ScrollView(host);
        scroll.addView(layout);

        String srcDisplay = host.displayNodeId(normalizedSourceId);
        String dialogTitle = srcDisplay.contains(" > ") ? srcDisplay.substring(srcDisplay.lastIndexOf(" > ") + 3) : srcDisplay;
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(host)
                .setTitle(host.str(R.string.str_connections_of) + dialogTitle)
                .setView(scroll)
                .setNegativeButton(host.str(R.string.str_close), null)
                .create();
        dialogRef[0] = dialog;
        dialog.getWindow().setBackgroundDrawable(host.roundedBg(host.surface(), 16, 0, Color.TRANSPARENT));
        dialog.show();
    }

    public static void showRenameFolderDialog(MainActivity host, String folderPath) {
        android.widget.EditText input = host.createStyledInput(host.folderName(folderPath), host.str(R.string.str_category_title));
        android.widget.LinearLayout container = new android.widget.LinearLayout(host);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(host.dp(20), host.dp(12), host.dp(20), host.dp(4));
        container.addView(input);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(host)
                .setTitle(host.str(R.string.str_rename))
                .setView(container)
                .setPositiveButton(host.str(R.string.str_save), (d, w) -> {
                    String rawTitle = input.getText().toString().trim();
                    String title = host.sanitizeInput(rawTitle, 100);
                    if (title.isEmpty() || title.contains("/")) {
                        android.widget.Toast.makeText(host, host.str(R.string.str_invalid_name), android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    com.mindscape.app.Category target = null;
                    for (com.mindscape.app.Category folder : host.categoriesList()) {
                        if (folder.fullPath().equals(folderPath)) { target = folder; break; }
                    }
                    if (target == null) return;
                    if (!title.equalsIgnoreCase(target.title) && host.categoryExists(title, target.parent)) {
                        android.widget.Toast.makeText(host, host.str(R.string.str_category_already_exists), android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    for (com.mindscape.app.Category folder : host.categoriesList()) {
                        if (folder.fullPath().equals(folderPath)) {
                            String old = folder.fullPath();
                            folder.title = title;
                            String updated = folder.fullPath();
                            String oldPrefix = old + "/";
                            for (com.mindscape.app.Category child : host.categoriesList()) {
                                if (old.equals(child.parent)) {
                                    child.parent = updated;
                                } else if (child.parent != null && child.parent.startsWith(oldPrefix)) {
                                    child.parent = updated + child.parent.substring(old.length());
                                }
                            }
                            for (com.mindscape.app.Note note : host.notesList()) {
                                if (old.equals(note.categoryPath)) {
                                    note.categoryPath = updated;
                                } else if (note.categoryPath != null && note.categoryPath.startsWith(oldPrefix)) {
                                    note.categoryPath = updated + note.categoryPath.substring(old.length());
                                }
                            }
                            for (com.mindscape.app.LocalFileLink file : host.localFilesList()) {
                                if (old.equals(file.folderPath)) {
                                    file.folderPath = updated;
                                } else if (file.folderPath != null && file.folderPath.startsWith(oldPrefix)) {
                                    file.folderPath = updated + file.folderPath.substring(old.length());
                                }
                            }
                            String folderOldPrefix = "folder:" + old;
                            String folderOldSlash = "folder:" + old + "/";
                            String folderNewPrefix = "folder:" + updated;
                            String folderNewSlash = "folder:" + updated + "/";
                            String noteOldSlash = "note:" + old + "/";
                            String noteNewSlash = "note:" + updated + "/";

                            for (com.mindscape.app.Connection conn : host.connectionsList()) {
                                if (old.equals(conn.source)) conn.source = updated;
                                else if (conn.source.startsWith(oldPrefix)) conn.source = updated + conn.source.substring(old.length());
                                else if (folderOldPrefix.equals(conn.source)) conn.source = folderNewPrefix;
                                else if (conn.source.startsWith(folderOldSlash)) conn.source = folderNewSlash + conn.source.substring(folderOldSlash.length());
                                else if (conn.source.startsWith(noteOldSlash)) conn.source = noteNewSlash + conn.source.substring(noteOldSlash.length());

                                if (old.equals(conn.target)) conn.target = updated;
                                else if (conn.target.startsWith(oldPrefix)) conn.target = updated + conn.target.substring(old.length());
                                else if (folderOldPrefix.equals(conn.target)) conn.target = folderNewPrefix;
                                else if (conn.target.startsWith(folderOldSlash)) conn.target = folderNewSlash + conn.target.substring(folderOldSlash.length());
                                else if (conn.target.startsWith(noteOldSlash)) conn.target = noteNewSlash + conn.target.substring(noteOldSlash.length());
                            }
                            java.util.Set<String> newHiddenNodes = new java.util.HashSet<>();
                            for (String h : host.hiddenNodes()) {
                                if (old.equals(h)) newHiddenNodes.add(updated);
                                else if (h.startsWith(oldPrefix)) newHiddenNodes.add(updated + h.substring(old.length()));
                                else if (folderOldPrefix.equals(h)) newHiddenNodes.add(folderNewPrefix);
                                else if (h.startsWith(folderOldSlash)) newHiddenNodes.add(folderNewSlash + h.substring(folderOldSlash.length()));
                                else if (h.startsWith(noteOldSlash)) newHiddenNodes.add(noteNewSlash + h.substring(noteOldSlash.length()));
                                else newHiddenNodes.add(h);
                            }
                            host.hiddenNodes().clear();
                            host.hiddenNodes().addAll(newHiddenNodes);
                            host.expandedFolders().remove(old);
                            host.expandedFolders().add(updated);
                            break;
                        }
                    }
                    host.renderContent();
                })
                .setNegativeButton(host.str(R.string.str_cancel), null)
                .create();
        dialog.getWindow().setBackgroundDrawable(host.roundedBg(host.surface(), 16, 0, Color.TRANSPARENT));
        dialog.show();
    }

    public static void showMoveNoteDialog(MainActivity host, com.mindscape.app.Note note) {
        java.util.List<String> paths = host.getAllCategoryPaths();
        String noFolderLabel = host.str(R.string.str_no_folder);
        paths.add(0, noFolderLabel);

        android.widget.ScrollView scroll = new android.widget.ScrollView(host);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(host);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(host.dp(18), host.dp(12), host.dp(18), host.dp(8));
        scroll.addView(layout);

        final android.app.AlertDialog[] dialogRef = new android.app.AlertDialog[1];

        for (int i = 0; i < paths.size(); i++) {
            final String path = paths.get(i);
            final int index = i;
            android.widget.LinearLayout row = new android.widget.LinearLayout(host);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, host.dp(12), 0, host.dp(12));
            row.addView(host.iconAction("ic_create_category", Color.rgb(65, 120, 220)), new android.widget.LinearLayout.LayoutParams(host.dp(36), host.dp(36)));
            row.addView(host.text("  " + (index == 0 ? noFolderLabel : path), 15, host.primaryText(), false), new android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.setOnClickListener(v -> {
                String targetPath = index == 0 ? null : path;
                if (host.noteExists(note.title, targetPath)) {
                    android.widget.Toast.makeText(host, host.str(R.string.str_note_already_exists), android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                dismiss(dialogRef);
                String oldPath = note.fullPath();
                note.categoryPath = targetPath;
                String newPath = note.fullPath();

                if (!oldPath.equalsIgnoreCase(newPath)) {
                    String oldKey = "note:" + oldPath;
                    String newKey = "note:" + newPath;
                    for (com.mindscape.app.Connection conn : host.connectionsList()) {
                        if (conn.source.equalsIgnoreCase(oldKey)) conn.source = newKey;
                        else if (conn.source.equalsIgnoreCase(oldPath)) conn.source = newPath;
                        if (conn.target.equalsIgnoreCase(oldKey)) conn.target = newKey;
                        else if (conn.target.equalsIgnoreCase(oldPath)) conn.target = newPath;
                    }
                    if (host.hiddenNodes().contains(oldKey)) { host.hiddenNodes().remove(oldKey); host.hiddenNodes().add(newKey); }
                    if (host.hiddenNodes().contains(oldPath)) { host.hiddenNodes().remove(oldPath); host.hiddenNodes().add(newPath); }
                }
                host.renderContent();
            });
            layout.addView(row);
        }

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(host)
                .setTitle(host.str(R.string.str_menu_move))
                .setView(scroll)
                .setNegativeButton(host.str(R.string.str_cancel), null)
                .create();
        dialogRef[0] = dialog;
        dialog.getWindow().setBackgroundDrawable(host.roundedBg(host.surface(), 16, 0, Color.TRANSPARENT));
        dialog.show();
    }

    public static void showLinkDialog(MainActivity host, String sourceId, boolean isFolder) {
        final String normalizedSourceId = host.normalizeConnectionNodeId(sourceId);
        java.util.List<String[]> folderCandidates = new java.util.ArrayList<>();
        for (String path : host.getAllCategoryPaths()) {
            String folderId = "folder:" + path;
            if (!folderId.equalsIgnoreCase(normalizedSourceId)) {
                folderCandidates.add(new String[]{folderId, path.replace("/", " > "), "ic_create_category", String.valueOf(Color.rgb(65, 120, 220))});
            }
        }
        java.util.List<String[]> noteCandidates = new java.util.ArrayList<>();
        for (com.mindscape.app.Note n : host.notesList()) {
            String noteId = "note:" + n.fullPath();
            if (!noteId.equalsIgnoreCase(normalizedSourceId)) {
                String noteLocation = n.isUnbound() ? "" : " [" + n.displayCategory() + "]";
                noteCandidates.add(new String[]{noteId, n.title + noteLocation + " (" + host.str(R.string.str_note) + ")", "ic_create_note", String.valueOf(Color.rgb(65, 120, 220))});
            }
        }
        java.util.List<String[]> fileCandidates = new java.util.ArrayList<>();
        for (com.mindscape.app.LocalFileLink file : host.localFilesList()) {
            String fileId = file.nodeId();
            if (!fileId.equalsIgnoreCase(normalizedSourceId)) {
                fileCandidates.add(new String[]{fileId, file.title + " [" + file.displayFolder() + "]", host.fileIconName(file), String.valueOf(host.fileIconColor(file))});
            }
        }
        if (folderCandidates.isEmpty() && noteCandidates.isEmpty() && fileCandidates.isEmpty()) {
            android.widget.Toast.makeText(host, host.str(R.string.str_no_other_nodes_to_link), android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        java.util.List<String[]> sortedCandidates = new java.util.ArrayList<>();
        if (isFolder) {
            sortedCandidates.addAll(folderCandidates);
            sortedCandidates.addAll(noteCandidates);
            sortedCandidates.addAll(fileCandidates);
        } else {
            sortedCandidates.addAll(noteCandidates);
            sortedCandidates.addAll(fileCandidates);
            sortedCandidates.addAll(folderCandidates);
        }
        android.widget.ScrollView scroll = new android.widget.ScrollView(host);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(host);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(host.dp(18), host.dp(12), host.dp(18), host.dp(8));
        scroll.addView(layout);

        final android.app.AlertDialog[] dialogRef = new android.app.AlertDialog[1];

        for (String[] c : sortedCandidates) {
            String id = c[0];
            String display = c[1];
            String icon = c[2];
            int color = Integer.parseInt(c[3]);
            android.widget.LinearLayout row = new android.widget.LinearLayout(host);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, host.dp(12), 0, host.dp(12));
            row.addView(host.iconAction(icon, color), new android.widget.LinearLayout.LayoutParams(host.dp(36), host.dp(36)));
            row.addView(host.text("  " + display, 15, host.primaryText(), false), new android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.setOnClickListener(v -> {
                dismiss(dialogRef);
                String targetId = host.normalizeConnectionNodeId(id);
                boolean exists = false;
                for (com.mindscape.app.Connection conn : host.connectionsList()) {
                    String connSource = host.normalizeConnectionNodeId(conn.source);
                    String connTarget = host.normalizeConnectionNodeId(conn.target);
                    if ((connSource.equalsIgnoreCase(normalizedSourceId) && connTarget.equalsIgnoreCase(targetId)) ||
                        (connSource.equalsIgnoreCase(targetId) && connTarget.equalsIgnoreCase(normalizedSourceId))) {
                        exists = true;
                        break;
                    }
                }
                if (exists) {
                    android.widget.Toast.makeText(host, host.str(R.string.str_connection_already_exists), android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    host.connectionsList().add(new com.mindscape.app.Connection(normalizedSourceId, targetId));
                    host.savePersistentData();
                    host.pushGraphDataToWebView();
                    host.renderContent();
                }
                showConnectionsDialog(host, normalizedSourceId, isFolder);
            });
            layout.addView(row);
        }

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(host)
                .setTitle(host.str(R.string.str_link_with))
                .setView(scroll)
                .setNegativeButton(host.str(R.string.str_cancel), null)
                .create();
        dialogRef[0] = dialog;
        dialog.getWindow().setBackgroundDrawable(host.roundedBg(host.surface(), 16, 0, Color.TRANSPARENT));
        dialog.show();
    }

    public static void showDeleteFolderDialog(MainActivity host, String folderPath) {
        boolean isCenter = false;
        com.mindscape.app.Category target = null;
        for (com.mindscape.app.Category c : host.categoriesList()) {
            if (c.fullPath().equals(folderPath)) { target = c; isCenter = c.isCenter; break; }
        }
        android.widget.LinearLayout dialogView = new android.widget.LinearLayout(host);
        dialogView.setOrientation(android.widget.LinearLayout.VERTICAL);
        dialogView.setPadding(host.dp(24), host.dp(10), host.dp(24), host.dp(0));

        android.widget.TextView msg = new android.widget.TextView(host);
        String name = target != null ? target.title : host.folderName(folderPath);
        boolean ru = "ru".equals(host.activeLanguage());
        msg.setText(isCenter
                ? (ru ? "Удалить центр «" + name + "»?" : "Delete center \"" + name + "\"?")
                : (ru ? "Удалить папку «" + name + "»?" : "Delete folder \"" + name + "\"?"));
        msg.setTextColor(host.primaryText());
        msg.setTextSize(16);
        dialogView.addView(msg);

        dialogView.addView(host.spacer(12));
        View deleteContentsSwitch = host.createStyledSwitch(
                ru ? "Очистить всё" : "Delete all contents",
                true
        );
        dialogView.addView(deleteContentsSwitch);
        dialogView.addView(host.text(
                ru ? "Если включено, вместе с папкой/центром удалятся все вложенные заметки и файлы. Если выключено, они останутся нераспределёнными."
                        : "When enabled, all nested notes and files are deleted with the folder/center. When disabled, they remain uncategorized.",
                12,
                host.secondaryText(),
                false));

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(host)
                .setTitle(isCenter ? host.str(R.string.str_delete_center) : host.str(R.string.str_delete))
                .setView(dialogView)
                .setPositiveButton(host.str(R.string.str_delete), (d, w) -> {
                    boolean deleteContents = true;
                    Object tag = deleteContentsSwitch.getTag();
                    if (tag instanceof com.mindscape.app.ui.StyledToggleState) {
                        deleteContents = ((com.mindscape.app.ui.StyledToggleState) tag).isChecked();
                    }
                    host.deleteFolderSilent(folderPath, deleteContents);
                    host.pushGraphDataToWebView();
                    host.renderContent();
                })
                .setNegativeButton(host.str(R.string.str_cancel), null)
                .create();
        dialog.getWindow().setBackgroundDrawable(host.roundedBg(host.surface(), 16, 0, Color.TRANSPARENT));
        dialog.show();
    }

    private static void showDialog(MainActivity host, android.app.AlertDialog[] dialogRef, @Nullable String folderPath, View layout) {
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(host)
                .setCustomTitle(dialogTitle(host, folderPath == null ? host.str(R.string.str_add) : host.folderName(folderPath)))
                .setView(layout)
                .setNegativeButton(host.str(R.string.str_close), null)
                .create();
        dialogRef[0] = dialog;
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(host.roundedBg(host.surface(), 16, 0, Color.TRANSPARENT));
        }
    }

    private static void dismiss(android.app.AlertDialog[] dialogRef) {
        if (dialogRef[0] != null) dialogRef[0].dismiss();
    }
}
