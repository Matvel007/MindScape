package com.mindscape.app.screens;

import android.graphics.Color;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.mindscape.app.LocalFileLink;
import com.mindscape.app.MainActivity;
import com.mindscape.app.Note;
import com.mindscape.app.R;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class NotesScreens {
    private NotesScreens() {}

    public static View notesTabScreen(MainActivity host) {
        ScrollView scroll = new ScrollView(host);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, 0, 0, host.dp(18));
        LinearLayout body = new LinearLayout(host);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(host.dp(18), host.dp(8), host.dp(18), host.dp(18));
        scroll.addView(body);

        LinearLayout headRow = new LinearLayout(host);
        headRow.setOrientation(LinearLayout.HORIZONTAL);
        headRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleCol = new LinearLayout(host);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        int selectedCount = host.selectedNotes().size() + host.selectedLocalFiles().size();
        String notesTitle = selectedCount == 0
                ? host.str(R.string.str_all_notes)
                : host.str(R.string.str_selected) + selectedCount;
        TextView titleView = host.text(notesTitle, 22, host.primaryText(), true);
        titleCol.addView(titleView);
        String sortLabel = host.notesSortMode() == 0
                ? host.str(R.string.str_newest_first)
                : host.notesSortMode() == 1
                    ? host.str(R.string.str_by_title)
                    : host.notesSortMode() == 2
                        ? host.str(R.string.str_by_category)
                        : host.str(R.string.str_favorites_first);
        titleCol.addView(host.text(host.str(R.string.str_total).trim() + " " + (host.notesList().size() + host.localFilesList().size()) + " • " + sortLabel, 12, host.secondaryText(), false));
        headRow.addView(titleCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout actionContainer = new LinearLayout(host);
        actionContainer.setOrientation(LinearLayout.HORIZONTAL);
        if (selectedCount > 0) {
            TextView selectAll = host.text(host.str(R.string.str_all), 14, host.accentColor(), true);
            selectAll.setPadding(host.dp(8), host.dp(8), host.dp(8), host.dp(8));
            selectAll.setOnClickListener(v -> {
                host.selectedNotes().clear();
                host.selectedNotes().addAll(host.notesList());
                host.selectedLocalFiles().clear();
                host.selectedLocalFiles().addAll(host.localFilesList());
                host.renderContent();
            });
            actionContainer.addView(selectAll);
            android.widget.ImageView deleteSelected = host.iconAction("ic_trash", Color.rgb(205, 80, 78));
            deleteSelected.setOnClickListener(v -> {
                if (!host.selectedNotes().isEmpty()) host.deleteNotes(new ArrayList<>(host.selectedNotes()));
                if (!host.selectedLocalFiles().isEmpty()) host.confirmRemoveLocalFiles(new ArrayList<>(host.selectedLocalFiles()));
                host.selectedNotes().clear();
                if (host.selectedLocalFiles().isEmpty()) host.renderContent();
            });
            actionContainer.addView(deleteSelected, new LinearLayout.LayoutParams(host.dp(42), host.dp(42)));
            TextView cancel = host.text(host.str(R.string.str_cancel), 14, host.secondaryText(), true);
            cancel.setPadding(host.dp(8), host.dp(8), host.dp(8), host.dp(8));
            cancel.setOnClickListener(v -> {
                host.selectedNotes().clear();
                host.selectedLocalFiles().clear();
                host.renderContent();
            });
            actionContainer.addView(cancel);
        }
        headRow.addView(actionContainer);
        body.addView(headRow);

        EditText searchBar = host.createStyledInput("", host.str(R.string.str_search_notes));
        searchBar.setSingleLine(true);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchParams.setMargins(0, host.dp(10), 0, host.dp(10));
        body.addView(searchBar, searchParams);

        android.widget.HorizontalScrollView hScroll = new android.widget.HorizontalScrollView(host);
        hScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chipsRow = new LinearLayout(host);
        chipsRow.setOrientation(LinearLayout.HORIZONTAL);
        chipsRow.setPadding(0, 0, 0, host.dp(4));

        host.addChipWithMargin(chipsRow, host.filterChip("all", host.str(R.string.str_all), "ic_file_text"));
        host.addChipWithMargin(chipsRow, host.filterChip("favorites", host.str(R.string.str_favorites), "ic_star_outline"));
        host.addChipWithMargin(chipsRow, host.filterChip("recent", host.str(R.string.str_recent), "ic_clock"));

        hScroll.addView(chipsRow);
        body.addView(hScroll);

        LinearLayout notesContainer = new LinearLayout(host);
        notesContainer.setOrientation(LinearLayout.VERTICAL);
        body.addView(notesContainer);

        final Runnable[] populateNotesRef = new Runnable[1];
        Runnable populateNotes = () -> {
            notesContainer.removeAllViews();
            int currentSelectedCount = host.selectedNotes().size() + host.selectedLocalFiles().size();
            titleView.setText(currentSelectedCount == 0
                    ? host.str(R.string.str_all_notes)
                    : host.str(R.string.str_selected) + currentSelectedCount);
            String query = searchBar.getText().toString().trim().toLowerCase(Locale.ROOT);
            List<Note> visibleNotes = new ArrayList<>(host.notesList());

            List<Note> recentTop3 = new ArrayList<>(host.notesList());
            recentTop3.sort((a, b) -> Long.compare(Math.max(b.createdAt, b.updatedAt), Math.max(a.createdAt, a.updatedAt)));
            if (recentTop3.size() > 3) recentTop3 = recentTop3.subList(0, 3);

            if (host.notesSortMode() == 0) {
                visibleNotes.sort((a, b) -> Long.compare(Math.max(b.createdAt, b.updatedAt), Math.max(a.createdAt, a.updatedAt)));
            } else if (host.notesSortMode() == 1) {
                visibleNotes.sort(Comparator.comparing(n -> n.title.toLowerCase(Locale.ROOT)));
            } else if (host.notesSortMode() == 2) {
                visibleNotes.sort(Comparator.comparing((Note n) -> n.categoryPath != null ? n.categoryPath.toLowerCase(Locale.ROOT) : "").thenComparing(n -> n.title.toLowerCase(Locale.ROOT)));
            } else if (host.notesSortMode() == 3) {
                visibleNotes.sort((a, b) -> {
                    if (a.favorite == b.favorite) return a.title.compareToIgnoreCase(b.title);
                    return a.favorite ? -1 : 1;
                });
            }

            for (Note n : visibleNotes) {
                if (!"all".equals(host.activeFileTypeFilter()) && !"notes".equals(host.activeFileTypeFilter())) continue;
                if (query.isEmpty() || n.title.toLowerCase(Locale.ROOT).contains(query) || n.content.toLowerCase(Locale.ROOT).contains(query)) {
                    if ("favorites".equals(host.activeNotesFilter()) && !n.favorite) continue;
                    if ("recent".equals(host.activeNotesFilter()) && !recentTop3.contains(n)) continue;

                    boolean selected = host.selectedNotes().contains(n);
                    LinearLayout nCard = host.card(host.surface());
                    nCard.setPadding(host.dp(14), host.dp(12), host.dp(14), host.dp(12));
                    int selectedBg = host.isDarkTheme() ? Color.rgb(43, 61, 92) : Color.rgb(232, 241, 255);
                    int selectedStroke = host.isDarkTheme() ? Color.rgb(170, 210, 255) : Color.rgb(65, 120, 220);
                    nCard.setBackground(host.roundedBg(selected ? selectedBg : host.surface(), 12, selected ? 2 : 1, selected ? selectedStroke : host.strokeColor()));

                    LinearLayout topRow = new LinearLayout(host);
                    topRow.setGravity(Gravity.CENTER_VERTICAL);
                    topRow.addView(host.text(n.title, 16, host.primaryText(), true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    if (n.reminderEnabled) {
                        topRow.addView(host.quickReminderIcon(n), new LinearLayout.LayoutParams(host.dp(34), host.dp(34)));
                    }

                    android.widget.ImageView star = host.iconAction(n.favorite ? "ic_star_filled" : "ic_star_outline", n.favorite ? Color.rgb(240, 180, 50) : Color.rgb(160, 168, 180));
                    star.setOnClickListener(v -> {
                        n.favorite = !n.favorite;
                        host.renderContent();
                    });
                    topRow.addView(star, new LinearLayout.LayoutParams(host.dp(38), host.dp(38)));
                    nCard.addView(topRow);

                    nCard.addView(host.chip(n.displayCategory()));

                    TextView preview = host.text(n.content, 13, host.secondaryText(), false);
                    preview.setMaxLines(2);
                    preview.setEllipsize(TextUtils.TruncateAt.END);
                    nCard.addView(preview);

                    LinearLayout bRow = new LinearLayout(host);
                    bRow.setGravity(Gravity.CENTER_VERTICAL);
                    bRow.setPadding(0, host.dp(6), 0, 0);
                    bRow.addView(host.text(host.formatTimeAgo(Math.max(n.createdAt, n.updatedAt)), 11, host.secondaryText(), false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                    android.widget.ImageView deleteBtn = host.iconAction("ic_trash", Color.rgb(205, 80, 78));
                    deleteBtn.setOnClickListener(v -> {
                        host.deleteNote(n);
                        host.selectedNotes().remove(n);
                        host.renderContent();
                    });
                    bRow.addView(deleteBtn, new LinearLayout.LayoutParams(host.dp(38), host.dp(38)));

                    android.widget.ImageView editBtn = host.iconAction("ic_wrench", host.secondaryText());
                    editBtn.setOnClickListener(v -> host.editNoteDialog(n));
                    LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(host.dp(38), host.dp(38));
                    editParams.setMargins(host.dp(4), 0, 0, 0);
                    bRow.addView(editBtn, editParams);

                    nCard.addView(bRow);

                    nCard.setOnClickListener(v -> {
                        if (!host.selectedNotes().isEmpty() || !host.selectedLocalFiles().isEmpty()) {
                            if (host.selectedNotes().contains(n)) {
                                host.selectedNotes().remove(n);
                            } else {
                                host.selectedNotes().add(n);
                            }
                            if (host.selectedNotes().isEmpty() && host.selectedLocalFiles().isEmpty()) host.renderContent();
                            else populateNotesRef[0].run();
                            return;
                        }
                        host.selectedMapEntity(n);
                        host.selectedSection("Карта");
                        host.renderContent();
                        host.updateBottomNav();
                    });
                    nCard.setOnLongClickListener(v -> {
                        boolean wasSelectionMode = !host.selectedNotes().isEmpty() || !host.selectedLocalFiles().isEmpty();
                        if (host.selectedNotes().contains(n)) {
                            host.selectedNotes().remove(n);
                        } else {
                            host.selectedNotes().add(n);
                        }
                        if (wasSelectionMode) {
                            if (host.selectedNotes().isEmpty() && host.selectedLocalFiles().isEmpty()) host.renderContent();
                            else populateNotesRef[0].run();
                        } else {
                            host.renderContent();
                        }
                        return true;
                    });

                    notesContainer.addView(nCard);
                }
            }
            if (!"favorites".equals(host.activeNotesFilter())) {
                List<LocalFileLink> visibleFiles = new ArrayList<>(host.localFilesList());
                visibleFiles.sort((a, b) -> Long.compare(b.addedAt, a.addedAt));
                for (LocalFileLink file : visibleFiles) {
                    if (!host.fileMatchesActiveTypeFilter(file)) continue;
                    if (!query.isEmpty() && !host.fileMatchesSearch(file, query)) {
                        continue;
                    }
                    if ("recent".equals(host.activeNotesFilter()) && visibleFiles.indexOf(file) >= 6) continue;

                    LinearLayout fCard = host.card(host.surface());
                    fCard.setPadding(host.dp(14), host.dp(12), host.dp(14), host.dp(12));
                    boolean fileSelected = host.selectedLocalFiles().contains(file);
                    int selectedBg = host.isDarkTheme() ? Color.rgb(43, 61, 92) : Color.rgb(232, 241, 255);
                    int selectedStroke = host.isDarkTheme() ? Color.rgb(170, 210, 255) : Color.rgb(65, 120, 220);
                    fCard.setBackground(host.roundedBg(fileSelected ? selectedBg : (host.isDarkTheme() ? host.surface() : Color.WHITE), 12, fileSelected ? 2 : 1, fileSelected ? selectedStroke : host.strokeColor()));

                    LinearLayout topRow = new LinearLayout(host);
                    topRow.setGravity(Gravity.CENTER_VERTICAL);
                    topRow.addView(host.iconAction(host.fileIconName(file), host.fileIconColor(file)), new LinearLayout.LayoutParams(host.dp(36), host.dp(36)));
                    TextView fileTitle = host.text("  " + file.title, 16, host.primaryText(), true);
                    fileTitle.setMaxLines(2);
                    fileTitle.setEllipsize(TextUtils.TruncateAt.END);
                    topRow.addView(fileTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    fCard.addView(topRow);

                    fCard.addView(host.chip(file.displayFolder()));
                    fCard.addView(host.text(host.fileMimeLabel(file) + " · " + host.formatFileSize(file.size), 13, host.secondaryText(), false));

                    LinearLayout bRow = new LinearLayout(host);
                    bRow.setGravity(Gravity.CENTER_VERTICAL);
                    bRow.setPadding(0, host.dp(6), 0, 0);
                    bRow.addView(host.text(host.formatTimeAgo(file.addedAt), 11, host.secondaryText(), false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                    android.widget.ImageView deleteBtn = host.iconAction("ic_trash", Color.rgb(205, 80, 78));
                    deleteBtn.setOnClickListener(v -> host.confirmRemoveLocalFile(file));
                    bRow.addView(deleteBtn, new LinearLayout.LayoutParams(host.dp(38), host.dp(38)));
                    android.widget.ImageView openBtn = host.iconAction("ic_more", host.secondaryText());
                    openBtn.setOnClickListener(v -> host.showLocalFileDialog(file));
                    bRow.addView(openBtn, new LinearLayout.LayoutParams(host.dp(38), host.dp(38)));
                    fCard.addView(bRow);

                    fCard.setOnClickListener(v -> {
                        if (!host.selectedNotes().isEmpty() || !host.selectedLocalFiles().isEmpty()) {
                            if (host.selectedLocalFiles().contains(file)) host.selectedLocalFiles().remove(file);
                            else host.selectedLocalFiles().add(file);
                            populateNotesRef[0].run();
                            return;
                        }
                        host.selectedMapEntity(file);
                        host.selectedSection("Карта");
                        host.renderContent();
                        host.updateBottomNav();
                    });
                    fCard.setOnLongClickListener(v -> {
                        boolean wasSelectionMode = !host.selectedNotes().isEmpty() || !host.selectedLocalFiles().isEmpty();
                        if (host.selectedLocalFiles().contains(file)) host.selectedLocalFiles().remove(file);
                        else host.selectedLocalFiles().add(file);
                        if (wasSelectionMode) {
                            populateNotesRef[0].run();
                        } else {
                            host.renderContent();
                        }
                        return true;
                    });
                    notesContainer.addView(fCard);
                }
            }
            if (notesContainer.getChildCount() == 0) {
                LinearLayout empty = host.card(host.surface());
                empty.addView(host.text(host.str(R.string.str_nothing_found), 15, Color.rgb(90, 100, 118), true));
                empty.addView(host.text(host.str(R.string.str_change_search_filter_or_sorting), 12, host.secondaryText(), false));
                notesContainer.addView(empty);
            }
        };
        populateNotesRef[0] = populateNotes;

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                populateNotes.run();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        populateNotes.run();
        return scroll;
    }
}
