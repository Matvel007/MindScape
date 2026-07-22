package com.mindscape.app.screens;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.mindscape.app.Category;
import com.mindscape.app.LocalFileLink;
import com.mindscape.app.MainActivity;
import com.mindscape.app.Note;
import com.mindscape.app.R;
import com.mindscape.app.ui.StyledToggleState;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StructureScreens {
    private StructureScreens() {}

    public static View createTabScreen(MainActivity host) {
        FrameLayout frame = new FrameLayout(host);
        ScrollView scroll = new ScrollView(host);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, 0, 0, host.dp(18));
        LinearLayout body = new LinearLayout(host);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(host.dp(18), host.dp(8), host.dp(18), host.dp(18));
        scroll.addView(body);
        frame.addView(scroll);

        LinearLayout listContainer = new LinearLayout(host);
        listContainer.setOrientation(LinearLayout.VERTICAL);

        if (host.currentStructurePath() == null) {
            host.addStructureHeader(body, listContainer);
            host.renderStructureRows(listContainer);
        } else {
            host.addDrillDownHeader(body, listContainer);
            host.renderDrillDownRows(listContainer);
        }

        body.addView(listContainer);

        return frame;
    }

    public static void addStructureHeader(MainActivity host, LinearLayout body, LinearLayout listContainer) {
        LinearLayout top = new LinearLayout(host);
        top.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titles = new LinearLayout(host);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(host.text(host.str(R.string.str_structure), 24, host.primaryText(), true));
        titles.addView(host.text(host.str(R.string.str_your_knowledge_base), 12, host.secondaryText(), false));
        top.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (host.structureSelectionMode()) {
            host.renderStructureSelectionBar(top);
        }
        body.addView(top);

        EditText searchBar = host.createStyledInput("", host.str(R.string.str_search_hint));
        searchBar.setSingleLine(true);
        if (host.structureSearchQuery() != null && !host.structureSearchQuery().isEmpty()) {
            searchBar.setText(host.structureSearchQuery());
            searchBar.setSelection(host.structureSearchQuery().length());
        }
        searchBar.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                String newQuery = s.toString();
                if (!newQuery.equals(host.structureSearchQuery())) {
                    host.structureSearchQuery(newQuery);
                    listContainer.removeAllViews();
                    host.renderStructureRows(listContainer);
                }
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, host.dp(10), 0, host.dp(10));
        body.addView(searchBar, params);

        android.widget.HorizontalScrollView hScroll = new android.widget.HorizontalScrollView(host);
        hScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chipsRow = new LinearLayout(host);
        chipsRow.setOrientation(LinearLayout.HORIZONTAL);
        chipsRow.setGravity(Gravity.CENTER_VERTICAL);
        chipsRow.setPadding(0, 0, 0, host.dp(4));
        boolean ru = "ru".equals(host.activeLanguage());
        addStructureChip(chipsRow, structureActionChip(host, ru ? "Контейнер" : "Inbox", "ic_container", host.quickContainerNoteCount(), v -> host.showQuickNotesContainerDialog()), host);
        addStructureChip(chipsRow, structureActionChip(host, ru ? "Центр" : "Center", "ic_person_root", 0, v -> host.showCreateCenterDialog()), host);
        addStructureChip(chipsRow, structureActionChip(host, ru ? "Свернуть" : "Collapse", "ic_chevron_right", 0, v -> {
            host.expandedFolders().clear();
            host.renderContent();
        }), host);
        hScroll.addView(chipsRow);
        body.addView(hScroll);
    }

    private static void addStructureChip(LinearLayout row, View chip, MainActivity host) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, host.dp(36));
        params.setMargins(0, 0, host.dp(8), 0);
        row.addView(chip, params);
    }

    private static View structureActionChip(MainActivity host, String label, String iconName, int badgeCount, View.OnClickListener listener) {
        LinearLayout chip = new LinearLayout(host);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setMinimumHeight(host.dp(34));
        chip.setPadding(host.dp(12), host.dp(6), host.dp(12), host.dp(6));
        chip.setBackground(host.roundedBg(host.isDarkTheme() ? Color.rgb(32, 37, 46) : host.softSurface(), 12, 1, host.strokeColor()));
        chip.setOnClickListener(listener);

        ImageView icon = new ImageView(host);
        icon.setImageResource(host.getResources().getIdentifier(iconName, "drawable", host.getPackageName()));
        icon.setColorFilter(host.secondaryText());
        chip.addView(icon, new LinearLayout.LayoutParams(host.dp(18), host.dp(18)));

        TextView title = host.text(label, 13, host.secondaryText(), true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(host.dp(6), 0, 0, 0);
        chip.addView(title, titleParams);

        if (badgeCount > 0) {
            TextView badge = host.text(String.valueOf(Math.min(badgeCount, 99)), 11, Color.WHITE, true);
            badge.setGravity(Gravity.CENTER);
            badge.setBackground(host.roundedBg(Color.rgb(230, 90, 86), 18, 0, Color.TRANSPARENT));
            LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(host.dp(24), host.dp(22));
            badgeParams.setMargins(host.dp(7), 0, 0, 0);
            chip.addView(badge, badgeParams);
        }
        return chip;
    }

    public static void addDrillDownHeader(MainActivity host, LinearLayout body, LinearLayout listContainer) {
        LinearLayout top = new LinearLayout(host);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(0, 0, 0, host.dp(10));

        LinearLayout backAndTitle = new LinearLayout(host);
        backAndTitle.setOrientation(LinearLayout.HORIZONTAL);
        backAndTitle.setGravity(Gravity.CENTER_VERTICAL);

        TextView backArrow = host.text("←  ", 24, host.primaryText(), true);
        backArrow.setPadding(0, host.dp(6), host.dp(10), host.dp(6));
        backArrow.setOnClickListener(v -> {
            String path = host.currentStructurePath();
            if (path.isEmpty()) {
                host.currentStructurePath(null);
            } else {
                host.currentStructurePath(host.parentPath(path));
            }
            host.renderContent();
        });
        backAndTitle.addView(backArrow);

        String path = host.currentStructurePath();
        String titleText = path.isEmpty() ? "MindScape" : host.folderName(path);
        TextView title = host.text(titleText, 22, host.primaryText(), true);
        backAndTitle.addView(title);

        top.addView(backAndTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (host.structureSelectionMode()) {
            host.renderStructureSelectionBar(top);
        } else {
            TextView menu = host.actionText("⋮");
            menu.setOnClickListener(v -> host.showFolderActionsDialog(path.isEmpty() ? null : path));
            top.addView(menu);
        }
        body.addView(top);
    }

    public static void renderStructureSelectionBar(MainActivity host, LinearLayout top) {
        top.removeAllViews();

        int count = host.structureSelectedFolders().size() + host.structureSelectedNotes().size() + host.structureSelectedFiles().size();
        String title = host.str(R.string.str_selected) + count;

        TextView titleView = host.text(title, 18, host.primaryText(), true);
        top.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView selectAllText = host.text(host.str(R.string.str_all), 15, host.accentColor(), true);
        selectAllText.setPadding(host.dp(12), host.dp(8), host.dp(12), host.dp(8));
        selectAllText.setOnClickListener(v -> {
            host.structureSelectedFolders().clear();
            host.structureSelectedNotes().clear();
            for (Category c : host.categoriesList()) host.structureSelectedFolders().add(c.fullPath());
            host.structureSelectedNotes().addAll(host.notesList());
            host.structureSelectedFiles().addAll(host.localFilesList());
            host.renderContent();
        });
        top.addView(selectAllText);

        android.widget.ImageView deleteBtn = host.iconAction("ic_trash", Color.rgb(205, 80, 78));
        deleteBtn.setOnClickListener(v -> {
            LinearLayout dialogView = new LinearLayout(host);
            dialogView.setOrientation(LinearLayout.VERTICAL);
            dialogView.setPadding(host.dp(24), host.dp(10), host.dp(24), host.dp(0));

            TextView msg = new TextView(host);
            msg.setText(host.str(R.string.str_delete_selected_items) + count + ")?");
            msg.setTextColor(host.primaryText());
            msg.setTextSize(16);
            dialogView.addView(msg);

            dialogView.addView(host.spacer(12));
            boolean ru = "ru".equals(host.activeLanguage());
            View deleteContentsSwitch = host.createStyledSwitch(
                    ru ? "Очистить всё" : "Delete all contents",
                    true
            );
            dialogView.addView(deleteContentsSwitch);
            dialogView.addView(host.text(
                    ru ? "Если включено, содержимое выбранных папок и центров будет удалено полностью."
                            : "When enabled, contents of selected folders and centers will be deleted completely.",
                    12,
                    host.secondaryText(),
                    false));

            new android.app.AlertDialog.Builder(host)
                    .setTitle(host.str(R.string.str_delete))
                    .setView(dialogView)
                    .setPositiveButton(host.str(R.string.str_delete), (d, w) -> {
                        boolean deleteContents = true;
                        Object tag = deleteContentsSwitch.getTag();
                        if (tag instanceof StyledToggleState) {
                            deleteContents = ((StyledToggleState) tag).isChecked();
                        }
                        for (String p : new ArrayList<>(host.structureSelectedFolders())) {
                            host.deleteFolderSilent(p, deleteContents);
                        }
                        List<Note> toDelete = new ArrayList<>();
                        for (Note n : host.structureSelectedNotes()) {
                            if (host.notesList().contains(n)) toDelete.add(n);
                        }
                        if (!toDelete.isEmpty()) host.deleteNotes(toDelete);
                        if (!host.structureSelectedFiles().isEmpty()) {
                            host.confirmRemoveLocalFiles(new ArrayList<>(host.structureSelectedFiles()));
                        }

                        host.structureSelectionMode(false);
                        host.structureSelectedFolders().clear();
                        host.structureSelectedNotes().clear();
                        host.structureSelectedFiles().clear();
                        host.savePersistentData();
                        host.renderContent();
                    })
                    .setNegativeButton(host.str(R.string.str_cancel), null)
                    .show();
        });

        TextView cancelBtn = host.text(host.str(R.string.str_cancel), 15, host.secondaryText(), true);
        cancelBtn.setPadding(host.dp(12), host.dp(8), host.dp(12), host.dp(8));
        cancelBtn.setOnClickListener(v -> {
            host.structureSelectionMode(false);
            host.structureSelectedFolders().clear();
            host.structureSelectedNotes().clear();
            host.structureSelectedFiles().clear();
            host.renderContent();
        });

        top.addView(deleteBtn);
        top.addView(cancelBtn);
    }

    public static void renderStructureRows(MainActivity host, LinearLayout container) {
        LinearLayout list = host.card(host.surface());
        list.setPadding(0, host.dp(6), 0, host.dp(6));

        if (host.structureSearchQuery() != null && !host.structureSearchQuery().trim().isEmpty()) {
            String q = host.structureSearchQuery().trim().toLowerCase(Locale.ROOT);
            boolean found = false;
            for (Category folder : host.categoriesList()) {
                if (folder.title.toLowerCase(Locale.ROOT).contains(q) || folder.description.toLowerCase(Locale.ROOT).contains(q)) {
                    list.addView(host.drillDownFolderRow(folder));
                    found = true;
                }
            }
            for (Note note : host.notesList()) {
                if (note.title.toLowerCase(Locale.ROOT).contains(q) || note.content.toLowerCase(Locale.ROOT).contains(q)) {
                    list.addView(host.drillDownNoteRow(note));
                    found = true;
                }
            }
            for (LocalFileLink file : host.localFilesList()) {
                if (host.fileMatchesSearch(file, q)) {
                    list.addView(host.drillDownFileRow(file));
                    found = true;
                }
            }
            if (!found) {
                TextView empty = host.text(host.str(R.string.str_nothing_found), 14, host.secondaryText(), false);
                empty.setPadding(host.dp(16), host.dp(16), host.dp(16), host.dp(16));
                empty.setGravity(Gravity.CENTER);
                list.addView(empty);
            }
        } else {
            List<Category> centers = host.getCenters();
            List<Category> legacy = host.getLegacyTopLevel();
            for (int i = 0; i < centers.size(); i++) {
                if (i > 0) addTopLevelSeparator(host, list);
                host.addCenterRootRow(list, centers.get(i), i, centers.size(), true);
            }
            for (int i = 0; i < legacy.size(); i++) {
                if (!centers.isEmpty() || i > 0) addTopLevelSeparator(host, list);
                host.addCenterRootRow(list, legacy.get(i), i, legacy.size(), false);
            }
            if (centers.isEmpty() && legacy.isEmpty()) {
                TextView empty = host.text(host.str(R.string.str_no_centers_hint), 14, host.secondaryText(), false);
                empty.setPadding(host.dp(16), host.dp(28), host.dp(16), host.dp(28));
                empty.setGravity(Gravity.CENTER);
                list.addView(empty);
            }
        }

        container.addView(list);
    }

    public static void renderDrillDownRows(MainActivity host, LinearLayout container) {
        LinearLayout list = host.card(host.surface());
        list.setPadding(0, host.dp(6), 0, host.dp(6));

        String path = host.currentStructurePath();
        String parentQuery = path.isEmpty() ? null : path;

        if (host.structureSearchQuery() != null && !host.structureSearchQuery().trim().isEmpty()) {
            String q = host.structureSearchQuery().trim().toLowerCase(Locale.ROOT);
            for (Category folder : host.childFolders(parentQuery)) {
                if (folder.title.toLowerCase(Locale.ROOT).contains(q)) list.addView(host.drillDownFolderRow(folder));
            }
            for (Note note : host.notesInFolder(parentQuery)) {
                if (note.title.toLowerCase(Locale.ROOT).contains(q)) list.addView(host.drillDownNoteRow(note));
            }
            for (LocalFileLink file : host.localFilesInFolder(parentQuery)) {
                if (host.fileMatchesSearch(file, q)) list.addView(host.drillDownFileRow(file));
            }
        } else {
            List<Category> subfolders = host.childFolders(parentQuery);
            List<Note> notes = host.notesInFolder(parentQuery);
            List<LocalFileLink> files = host.localFilesInFolder(parentQuery);

            for (Category folder : subfolders) list.addView(host.drillDownFolderRow(folder));
            for (Note note : notes) list.addView(host.drillDownNoteRow(note));
            for (LocalFileLink file : files) list.addView(host.drillDownFileRow(file));

            list.addView(host.inlineAddRow(parentQuery, 0));
        }

        container.addView(list);
    }

    public static View inlineAddRow(MainActivity host, @Nullable String folderPath, int depth) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, host.dp(12), 0);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, host.dp(44)));

        row.addView(host.createIndentLayout(depth));

        View chevronSpace = new View(host);
        row.addView(chevronSpace, new LinearLayout.LayoutParams(host.dp(40), host.dp(32)));

        ImageView plus = new ImageView(host);
        plus.setImageResource(host.getResources().getIdentifier("ic_nav_create", "drawable", host.getPackageName()));
        plus.setColorFilter(Color.rgb(65, 120, 220));
        plus.setPadding(host.dp(6), host.dp(6), host.dp(6), host.dp(6));
        row.addView(plus, new LinearLayout.LayoutParams(host.dp(34), host.dp(34)));

        TextView titleView = host.text("  " + host.str(R.string.str_add_ellipsis), 14, Color.rgb(65, 120, 220), false);
        row.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        row.setOnClickListener(v -> host.showAddContentDialog(folderPath));
        return host.wrapWithDivider(row, depth);
    }

    public static View emptyStructureHint(MainActivity host) {
        TextView empty = host.text(host.str(R.string.str_folder_empty_hint), 13, host.secondaryText(), false);
        empty.setPadding(host.dp(14), host.dp(16), host.dp(14), host.dp(16));
        return empty;
    }

    public static View structureFileRow(MainActivity host, LocalFileLink file, int depth) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, host.dp(12), 0);
        row.setMinimumHeight(host.dp(52));
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        row.addView(host.createIndentLayout(depth));
        row.addView(new View(host), new LinearLayout.LayoutParams(host.dp(24), 1));
        addFileRowContent(host, row, file);
        return host.wrapWithDivider(row, depth);
    }

    public static View drillDownFileRow(MainActivity host, LocalFileLink file) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(host.dp(16), 0, host.dp(16), 0);
        row.setMinimumHeight(host.dp(52));
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addFileRowContent(host, row, file);
        return host.wrapWithDivider(row, 0);
    }

    public static void addFileRowContent(MainActivity host, LinearLayout row, LocalFileLink file) {
        android.widget.ImageView icon = host.iconAction(host.fileIconName(file), host.fileIconColor(file));
        row.addView(icon, new LinearLayout.LayoutParams(host.dp(34), host.dp(34)));

        LinearLayout textCol = new LinearLayout(host);
        textCol.setOrientation(LinearLayout.VERTICAL);
        TextView title = host.text("  " + file.title, 15, host.primaryText(), false);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        textCol.addView(title);
        String details = host.fileMimeLabel(file) + " · " + host.formatFileSize(file.size);
        textCol.addView(host.text("  " + details, 11, host.secondaryText(), false));
        row.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        row.setOnClickListener(v -> host.focusEntityOnMap(file));
        if (host.isLinked(file)) {
            row.addView(host.quickLinkIcon(file.nodeId(), false), new LinearLayout.LayoutParams(host.dp(34), host.dp(34)));
        }
        if (host.isNodeHidden(file)) {
            row.addView(host.quickVisibilityIcon(file), new LinearLayout.LayoutParams(host.dp(28), host.dp(28)));
        }
        if (host.structureSelectionMode()) {
            boolean selected = host.structureSelectedFiles().contains(file);
            View checkIcon = host.createCustomCheckbox(selected);
            LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(host.dp(22), host.dp(22));
            checkParams.setMargins(host.dp(8), host.dp(8), host.dp(8), host.dp(8));
            row.addView(checkIcon, checkParams);
            row.setOnClickListener(v -> {
                if (selected) host.structureSelectedFiles().remove(file);
                else host.structureSelectedFiles().add(file);
                host.renderContent();
            });
        } else {
            android.widget.ImageView moreBtn = host.iconAction("ic_more", Color.rgb(106, 116, 132));
            moreBtn.setPadding(host.dp(6), host.dp(6), host.dp(6), host.dp(6));
            moreBtn.setOnClickListener(v -> host.showLocalFileDialog(file));
            row.addView(moreBtn, new LinearLayout.LayoutParams(host.dp(36), host.dp(36)));
            row.setOnLongClickListener(v -> {
                host.structureSelectionMode(true);
                host.structureSelectedFiles().add(file);
                host.renderContent();
                return true;
            });
        }
    }

    public static View structureNoteRow(MainActivity host, Note note, int depth) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, host.dp(12), 0);
        row.setMinimumHeight(host.dp(52));
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        row.addView(host.createIndentLayout(depth));
        row.addView(new View(host), new LinearLayout.LayoutParams(host.dp(24), 1));

        android.widget.ImageView icon = host.iconAction("ic_create_note", Color.rgb(65, 120, 220));
        row.addView(icon, new LinearLayout.LayoutParams(host.dp(34), host.dp(34)));
        TextView title = host.text("  " + note.title, 15, host.primaryText(), false);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (note.reminderEnabled) {
            row.addView(host.quickReminderIcon(note), new LinearLayout.LayoutParams(host.dp(28), host.dp(28)));
        }
        if (host.isLinked(note)) {
            row.addView(host.quickLinkIcon("note:" + note.fullPath(), false), new LinearLayout.LayoutParams(host.dp(28), host.dp(28)));
        }
        if (host.isNodeHidden(note)) {
            row.addView(host.quickVisibilityIcon(note), new LinearLayout.LayoutParams(host.dp(28), host.dp(28)));
        }

        addNoteSelectionOrActions(host, row, note);
        return host.wrapWithDivider(row, depth);
    }

    public static View drillDownNoteRow(MainActivity host, Note note) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(host.dp(16), 0, host.dp(16), 0);
        row.setMinimumHeight(host.dp(52));
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        android.widget.ImageView icon = host.iconAction("ic_create_note", Color.rgb(65, 120, 220));
        row.addView(icon, new LinearLayout.LayoutParams(host.dp(34), host.dp(34)));
        TextView title = host.text("  " + note.title, 15, host.primaryText(), false);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (note.reminderEnabled) {
            row.addView(host.quickReminderIcon(note), new LinearLayout.LayoutParams(host.dp(28), host.dp(28)));
        }
        if (host.isLinked(note)) {
            row.addView(host.quickLinkIcon("note:" + note.fullPath(), false), new LinearLayout.LayoutParams(host.dp(28), host.dp(28)));
        }
        if (host.isNodeHidden(note)) {
            row.addView(host.quickVisibilityIcon(note), new LinearLayout.LayoutParams(host.dp(28), host.dp(28)));
        }

        addNoteSelectionOrActions(host, row, note);
        return host.wrapWithDivider(row, 0);
    }

    private static void addNoteSelectionOrActions(MainActivity host, LinearLayout row, Note note) {
        if (host.structureSelectionMode()) {
            boolean selected = host.structureSelectedNotes().contains(note);
            View checkIcon = host.createCustomCheckbox(selected);
            LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(host.dp(22), host.dp(22));
            checkParams.setMargins(host.dp(8), host.dp(8), host.dp(8), host.dp(8));
            row.addView(checkIcon, checkParams);

            row.setOnClickListener(v -> {
                if (selected) host.structureSelectedNotes().remove(note);
                else host.structureSelectedNotes().add(note);
                host.renderContent();
            });
        } else {
            row.setOnClickListener(v -> host.focusEntityOnMap(note));
            row.setOnLongClickListener(v -> {
                host.structureSelectionMode(true);
                host.structureSelectedNotes().add(note);
                host.renderContent();
                return true;
            });

            android.widget.ImageView moreBtn = host.iconAction("ic_more", Color.rgb(106, 116, 132));
            moreBtn.setPadding(host.dp(6), host.dp(6), host.dp(6), host.dp(6));
            moreBtn.setOnClickListener(v -> host.showNoteActionsDialog(note));
            row.addView(moreBtn, new LinearLayout.LayoutParams(host.dp(36), host.dp(36)));
        }
    }

    public static View structureFolderRow(MainActivity host, Category folder, int depth) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, host.dp(12), 0);
        row.setMinimumHeight(host.dp(52));
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        row.addView(host.createIndentLayout(depth));

        android.widget.ImageView chevronLeft = new android.widget.ImageView(host);
        chevronLeft.setImageResource(host.getResources().getIdentifier(host.expandedFolders().contains(folder.fullPath()) ? "ic_chevron_down" : "ic_chevron_right", "drawable", host.getPackageName()));
        chevronLeft.setColorFilter(Color.rgb(150, 156, 170));
        chevronLeft.setPadding(host.dp(8), host.dp(4), host.dp(8), host.dp(4));
        chevronLeft.setOnClickListener(v -> {
            String path = folder.fullPath();
            if (host.expandedFolders().contains(path)) host.expandedFolders().remove(path);
            else host.expandedFolders().add(path);
            host.renderContent();
        });
        row.addView(chevronLeft, new LinearLayout.LayoutParams(host.dp(40), host.dp(32)));

        addFolderMainContent(host, row, folder, false);
        addFolderSelectionOrActions(host, row, folder);
        return host.wrapWithDivider(row, depth);
    }

    public static View drillDownFolderRow(MainActivity host, Category folder) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(host.dp(16), 0, host.dp(16), 0);
        row.setMinimumHeight(host.dp(52));
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        addFolderMainContent(host, row, folder, true);
        addFolderSelectionOrActions(host, row, folder);
        return host.wrapWithDivider(row, 0);
    }

    private static void addFolderMainContent(MainActivity host, LinearLayout row, Category folder, boolean linkByPath) {
        int count = host.getFolderItemsCount(folder.fullPath());
        boolean empty = count == 0;
        android.widget.ImageView icon = host.iconAction("ic_folder_outline", empty ? host.secondaryText() : Color.rgb(65, 120, 220));
        icon.setAlpha(empty ? 0.5f : 1.0f);
        row.addView(icon, new LinearLayout.LayoutParams(host.dp(34), host.dp(34)));

        String titleText = folder.title + (count > 0 ? " (" + count + ")" : "");
        TextView title = host.text("  " + titleText, 15, host.primaryText(), false);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (host.isLinked(linkByPath ? folder.fullPath() : folder)) {
            row.addView(host.quickLinkIcon("folder:" + folder.fullPath(), true), new LinearLayout.LayoutParams(host.dp(28), host.dp(28)));
        }
        if (host.isNodeHidden(folder)) {
            row.addView(host.quickVisibilityIcon(folder), new LinearLayout.LayoutParams(host.dp(28), host.dp(28)));
        }
    }

    private static void addFolderSelectionOrActions(MainActivity host, LinearLayout row, Category folder) {
        if (host.structureSelectionMode()) {
            boolean selected = host.structureSelectedFolders().contains(folder.fullPath());
            View checkIcon = host.createCustomCheckbox(selected);
            LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(host.dp(22), host.dp(22));
            checkParams.setMargins(host.dp(8), host.dp(8), host.dp(8), host.dp(8));
            row.addView(checkIcon, checkParams);

            row.setOnClickListener(v -> {
                if (selected) host.structureSelectedFolders().remove(folder.fullPath());
                else host.structureSelectedFolders().add(folder.fullPath());
                host.renderContent();
            });
        } else {
            row.setOnClickListener(v -> host.focusEntityOnMap(folder));
            row.setOnLongClickListener(v -> {
                host.structureSelectionMode(true);
                host.structureSelectedFolders().add(folder.fullPath());
                host.renderContent();
                return true;
            });

            android.widget.ImageView moreBtn = host.iconAction("ic_more", Color.rgb(106, 116, 132));
            moreBtn.setPadding(host.dp(6), host.dp(6), host.dp(6), host.dp(6));
            moreBtn.setOnClickListener(v -> host.showFolderActionsDialog(folder.fullPath()));
            row.addView(moreBtn, new LinearLayout.LayoutParams(host.dp(36), host.dp(36)));
        }
    }

    public static void addCenterRootRow(MainActivity host, LinearLayout list, Category cat, int index, int total, boolean isCenter) {
        final String path = cat.fullPath();
        final boolean expanded = host.expandedFolders().contains(path);

        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, host.dp(12), 0);
        row.setMinimumHeight(host.dp(54));
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        android.widget.ImageView chevron = new android.widget.ImageView(host);
        chevron.setImageResource(host.getResources().getIdentifier(expanded ? "ic_chevron_down" : "ic_chevron_right", "drawable", host.getPackageName()));
        chevron.setColorFilter(Color.rgb(150, 156, 170));
        chevron.setPadding(host.dp(8), host.dp(4), host.dp(8), host.dp(4));
        chevron.setOnClickListener(v -> {
            if (host.expandedFolders().contains(path)) host.expandedFolders().remove(path);
            else host.expandedFolders().add(path);
            host.renderContent();
        });
        row.addView(chevron, new LinearLayout.LayoutParams(host.dp(40), host.dp(32)));

        android.widget.ImageView icon = new android.widget.ImageView(host);
        icon.setImageResource(host.getResources().getIdentifier(isCenter ? "ic_person_root" : "ic_folder_outline", "drawable", host.getPackageName()));
        icon.setColorFilter(isCenter ? host.accentColor() : Color.rgb(65, 120, 220));
        row.addView(icon, new LinearLayout.LayoutParams(host.dp(28), host.dp(28)));

        TextView title = host.text("  " + cat.title, isCenter ? 16 : 15, host.primaryText(), true);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (host.isLinked(cat)) {
            row.addView(host.quickLinkIcon("folder:" + path, true), new LinearLayout.LayoutParams(host.dp(28), host.dp(28)));
        }
        if (host.isNodeHidden(cat)) {
            row.addView(host.quickVisibilityIcon(cat), new LinearLayout.LayoutParams(host.dp(28), host.dp(28)));
        }

        if (!host.structureSelectionMode()) {
            if (isCenter && total > 1) {
                LinearLayout arrows = new LinearLayout(host);
                arrows.setOrientation(LinearLayout.VERTICAL);
                arrows.setGravity(Gravity.CENTER);
                android.widget.ImageView up = new android.widget.ImageView(host);
                up.setImageResource(host.getResources().getIdentifier("ic_chevron_right", "drawable", host.getPackageName()));
                up.setRotation(-90f);
                up.setColorFilter(index == 0 ? Color.rgb(200, 200, 200) : Color.rgb(120, 126, 140));
                up.setAlpha(index == 0 ? 0.35f : 1f);
                up.setOnClickListener(v -> host.moveCenter(cat, -1));
                android.widget.ImageView down = new android.widget.ImageView(host);
                down.setImageResource(host.getResources().getIdentifier("ic_chevron_right", "drawable", host.getPackageName()));
                down.setRotation(90f);
                down.setColorFilter(index == total - 1 ? Color.rgb(200, 200, 200) : Color.rgb(120, 126, 140));
                down.setAlpha(index == total - 1 ? 0.35f : 1f);
                down.setOnClickListener(v -> host.moveCenter(cat, 1));
                arrows.addView(up, new LinearLayout.LayoutParams(host.dp(20), host.dp(16)));
                arrows.addView(down, new LinearLayout.LayoutParams(host.dp(20), host.dp(16)));
                row.addView(arrows);
            }
            android.widget.ImageView moreBtn = host.iconAction("ic_more", Color.rgb(106, 116, 132));
            moreBtn.setPadding(host.dp(6), host.dp(6), host.dp(6), host.dp(6));
            moreBtn.setOnClickListener(v -> host.showFolderActionsDialog(path));
            row.addView(moreBtn, new LinearLayout.LayoutParams(host.dp(36), host.dp(36)));
        }
        row.setOnClickListener(v -> host.focusEntityOnMap(cat));
        list.addView(row);

        if (expanded) {
            renderFolderChildren(host, list, path, 1);
            for (Note note : host.notesInFolder(path)) list.addView(host.structureNoteRow(note, 1));
            for (LocalFileLink file : host.localFilesInFolder(path)) list.addView(host.structureFileRow(file, 1));
            list.addView(host.inlineAddRow(path, 1));
        }
    }

    public static void renderFolderChildren(MainActivity host, LinearLayout list, @Nullable String parent, int depth) {
        for (Category folder : host.childFolders(parent)) {
            list.addView(host.structureFolderRow(folder, depth));
            if (host.expandedFolders().contains(folder.fullPath())) {
                renderFolderChildren(host, list, folder.fullPath(), depth + 1);
                for (Note note : host.notesInFolder(folder.fullPath())) list.addView(host.structureNoteRow(note, depth + 1));
                for (LocalFileLink file : host.localFilesInFolder(folder.fullPath())) list.addView(host.structureFileRow(file, depth + 1));
                list.addView(host.inlineAddRow(folder.fullPath(), depth + 1));
            }
        }
    }

    public static int getFolderItemsCount(MainActivity host, String folderPath) {
        int count = 0;
        for (Category folder : host.categoriesList()) {
            if (folderPath == null ? folder.parent == null || folder.parent.isEmpty() : folderPath.equals(folder.parent)) {
                count++;
            }
        }
        for (Note note : host.notesList()) {
            if (folderPath == null ? note.isUnbound() : folderPath.equals(note.categoryPath)) {
                count++;
            }
        }
        for (LocalFileLink file : host.localFilesList()) {
            if (folderPath == null ? file.isUnbound() : folderPath.equals(file.folderPath)) {
                count++;
            }
        }
        return count;
    }

    public static View wrapWithDivider(MainActivity host, View row, int depth) {
        LinearLayout wrapper = new LinearLayout(host);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        wrapper.addView(row);
        View divider = new View(host);
        divider.setBackgroundColor(host.strokeColor());
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, host.dp(1));
        dividerParams.setMarginStart(host.dp(Math.min(depth, 5) * 20));
        wrapper.addView(divider, dividerParams);
        return wrapper;
    }

    private static void addTopLevelSeparator(MainActivity host, LinearLayout list) {
        View divider = new View(host);
        divider.setBackgroundColor(host.strokeColor());
        list.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, host.dp(1)));
    }

    public static View createIndentLayout(MainActivity host, int depth) {
        LinearLayout indentLayout = new LinearLayout(host);
        indentLayout.setOrientation(LinearLayout.HORIZONTAL);
        indentLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        int visibleDepth = Math.min(depth, 5);
        for (int i = 0; i < visibleDepth; i++) {
            FrameLayout cell = new FrameLayout(host);
            LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(host.dp(20), ViewGroup.LayoutParams.MATCH_PARENT);
            cell.setLayoutParams(cellParams);

            View line = new View(host);
            FrameLayout.LayoutParams lineParams = new FrameLayout.LayoutParams(host.dp(1.5f), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL);
            line.setLayoutParams(lineParams);
            line.setBackgroundColor(host.strokeColor());
            cell.addView(line);

            indentLayout.addView(cell);
        }
        return indentLayout;
    }
}
