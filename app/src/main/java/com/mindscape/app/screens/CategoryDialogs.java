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
import android.widget.TextView;
import android.widget.Toast;

import com.mindscape.app.CatTreeNode;
import com.mindscape.app.Category;
import com.mindscape.app.ChatMessage;
import com.mindscape.app.ChatSession;
import com.mindscape.app.Connection;
import com.mindscape.app.LocalFileLink;
import com.mindscape.app.MainActivity;
import com.mindscape.app.Note;
import com.mindscape.app.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import androidx.annotation.Nullable;

public final class CategoryDialogs {
    private CategoryDialogs() {}

    public static void createCategoryDialog(MainActivity host) {
        createCategoryDialog(host, null);
    }

    public static void createCategoryDialog(MainActivity host, String initialParentPath) {
        String parentText = (initialParentPath == null || initialParentPath.isEmpty())
                ? host.str(R.string.str_none_root)
                : (host.str(R.string.str_category_path) + " " + initialParentPath);

        LinearLayout dialogRoot = new LinearLayout(host);
        dialogRoot.setOrientation(LinearLayout.VERTICAL);
        dialogRoot.setPadding(host.dp(16), host.dp(14), host.dp(16), host.dp(14));

        dialogRoot.addView(host.text(host.str(R.string.str_manage_categories), 18, host.primaryText(), true));
        dialogRoot.addView(host.spacer(6));

        ScrollView treeScroll = new ScrollView(host);
        LinearLayout treeView = new LinearLayout(host);
        treeView.setOrientation(LinearLayout.VERTICAL);
        treeView.setPadding(host.dp(4), host.dp(4), host.dp(4), host.dp(4));
        CatTreeNode root = host.buildCategoryTree();
        renderTree(host, treeView, root, 0, null);
        treeScroll.addView(treeView);
        dialogRoot.addView(treeScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, host.dp(240)));

        dialogRoot.addView(host.spacer(8));

        EditText titleInput = host.createStyledInput("", host.str(R.string.str_category_title));
        dialogRoot.addView(titleInput);

        EditText descInput = host.createStyledInput("", host.str(R.string.str_description));
        dialogRoot.addView(descInput);

        LinearLayout parentRow = new LinearLayout(host);
        parentRow.setOrientation(LinearLayout.HORIZONTAL);
        parentRow.setGravity(Gravity.CENTER_VERTICAL);
        String[] selectedParent = {initialParentPath};
        TextView parentLabel = host.text(parentText, 12, host.secondaryText(), false);
        parentLabel.setPadding(0, host.dp(4), host.dp(8), host.dp(4));
        parentRow.addView(parentLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button pickParent = host.createStyledButton(host.str(R.string.str_pick));
        pickParent.setOnClickListener(v -> {
            List<String> paths = host.getAllCategoryPaths();
            paths.add(0, host.str(R.string.str_none_root));
            String[] items = paths.toArray(new String[0]);
            host.showStyledChoiceDialog(host.str(R.string.str_select_parent_category), items, "ic_folder_outline", idx -> {
                if (idx == 0) {
                    selectedParent[0] = null;
                    parentLabel.setText(host.str(R.string.str_none_root));
                } else {
                    selectedParent[0] = paths.get(idx);
                    parentLabel.setText(host.str(R.string.str_category_path) + " " + selectedParent[0]);
                }
            });
        });
        parentRow.addView(pickParent);
        dialogRoot.addView(parentRow);

        dialogRoot.addView(host.spacer(8));

        Button bindNoteBtn = host.createStyledButton(host.str(R.string.str_bind_note));
        bindNoteBtn.setOnClickListener(v -> {
            if (host.notesList().isEmpty()) {
                Toast.makeText(host, host.str(R.string.str_no_notes_to_bind), Toast.LENGTH_SHORT).show();
                return;
            }
            List<String> allPaths = host.getAllCategoryPaths();
            if (allPaths.isEmpty()) {
                Toast.makeText(host, host.str(R.string.str_create_categories_first), Toast.LENGTH_SHORT).show();
                return;
            }
            String[] catItems = allPaths.toArray(new String[0]);

            String[] noteTitles = new String[host.notesList().size()];
            for (int i = 0; i < host.notesList().size(); i++) noteTitles[i] = host.notesList().get(i).title;

            LinearLayout bindLayout = new LinearLayout(host);
            bindLayout.setOrientation(LinearLayout.VERTICAL);
            bindLayout.setPadding(host.dp(16), host.dp(14), host.dp(16), host.dp(14));
            final String[] selNote = {noteTitles[0]};
            final String[] selCat = {catItems[0]};

            TextView noteLabel = host.text(selNote[0], 14, host.primaryText(), true);
            noteLabel.setPadding(0, host.dp(4), 0, host.dp(4));
            bindLayout.addView(noteLabel);
            Button pickNoteBtn = host.createStyledButton(host.str(R.string.str_pick_note));
            pickNoteBtn.setOnClickListener(bv -> host.showStyledChoiceDialog(host.str(R.string.str_select_note), noteTitles, "ic_create_note", ni -> {
                selNote[0] = noteTitles[ni];
                noteLabel.setText(selNote[0]);
            }));
            bindLayout.addView(pickNoteBtn);
            bindLayout.addView(host.spacer(6));

            TextView catLabelBind = host.text(selCat[0].replace("/", " > "), 13, host.secondaryText(), false);
            catLabelBind.setPadding(0, host.dp(4), 0, host.dp(4));
            bindLayout.addView(catLabelBind);
            Button pickCatBtn = host.createStyledButton(host.str(R.string.str_pick_category));
            pickCatBtn.setOnClickListener(cv -> host.showStyledChoiceDialog(host.str(R.string.str_select_category_1), catItems, "ic_folder_outline", ci -> {
                selCat[0] = catItems[ci];
                catLabelBind.setText(selCat[0].replace("/", " > "));
            }));
            bindLayout.addView(pickCatBtn);

            android.app.AlertDialog.Builder bindBuilder = new android.app.AlertDialog.Builder(host);
            bindBuilder.setView(bindLayout);
            bindBuilder.setPositiveButton(host.str(R.string.str_bind), (bd, bw) -> {
                for (Note n : host.notesList()) {
                    if (n.title.equals(selNote[0])) {
                        n.categoryPath = selCat[0];
                        break;
                    }
                }
                host.renderContent();
                treeView.removeAllViews();
                CatTreeNode newRoot = host.buildCategoryTree();
                renderTree(host, treeView, newRoot, 0, null);
            });
            bindBuilder.setNegativeButton(host.str(R.string.str_cancel), null);
            android.app.AlertDialog bindDialog = bindBuilder.create();
            bindDialog.getWindow().setBackgroundDrawable(host.roundedBg(host.surface(), 16, 0, Color.TRANSPARENT));
            bindDialog.show();
        });
        dialogRoot.addView(bindNoteBtn);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(host);
        builder.setView(dialogRoot);
        builder.setPositiveButton(host.str(R.string.str_create), (dialog, which) -> {
            String title = titleInput.getText().toString().trim();
            String desc = descInput.getText().toString().trim();
            if (!title.isEmpty()) {
                if (host.categoryExists(title, selectedParent[0])) {
                    Toast.makeText(host, host.str(R.string.str_category_already_exists), Toast.LENGTH_SHORT).show();
                    return;
                }
                Random r = new Random();
                int color = Color.rgb(100 + r.nextInt(120), 100 + r.nextInt(120), 100 + r.nextInt(120));
                host.categoriesList().add(new Category(title, desc.isEmpty() ? host.str(R.string.str_no_description) : desc, color, selectedParent[0]));
                host.renderContent();
            }
        });
        builder.setNegativeButton(host.str(R.string.str_cancel), null);

        android.app.AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(host.roundedBg(host.surface(), 16, 0, Color.TRANSPARENT));
        dialog.show();
    }

    public static void renderTree(MainActivity host, LinearLayout container, CatTreeNode node, int depth, String selectedPath) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(host.dp(depth * 18 + 4), host.dp(4), host.dp(6), host.dp(4));
        row.setBackground(host.roundedBg(depth == 0 ? host.softSurface() : Color.TRANSPARENT, 8, 0, Color.TRANSPARENT));

        if (depth == 0) {
            android.widget.ImageView personIcon = new android.widget.ImageView(host);
            personIcon.setImageResource(host.getResources().getIdentifier("ic_person_root", "drawable", host.getPackageName()));
            personIcon.setColorFilter(host.accentColor());
            personIcon.setBackground(host.roundedBg(Color.rgb(235, 244, 255), 12, 0, Color.TRANSPARENT));
            personIcon.setPadding(host.dp(7), host.dp(7), host.dp(7), host.dp(7));
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(host.dp(34), host.dp(34));
            iconParams.setMargins(0, 0, host.dp(10), 0);
            row.addView(personIcon, iconParams);
        }

        TextView nameText = host.text(depth == 0 ? host.ruRootLabel() : node.name, depth == 0 ? 14 : 13, depth == 0 ? host.primaryText() : host.secondaryText(), depth == 0);
        row.addView(nameText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.setOnClickListener(v -> createCategoryDialog(host, depth == 0 ? null : node.fullPath));

        if (depth > 0) {
            Button delBtn = new Button(host);
            delBtn.setText("✕");
            delBtn.setTextSize(11);
            host.applyTextWeight(delBtn, true);
            delBtn.setTextColor(host.themedTextColor(Color.rgb(205, 80, 78)));
            delBtn.setBackground(host.roundedBg(Color.TRANSPARENT, 6, 0, Color.TRANSPARENT));
            delBtn.setOnClickListener(v -> {
                host.deleteFolderSilent(node.fullPath, true);
                host.pushGraphDataToWebView();
                host.renderContent();
            });
            row.addView(delBtn);
        }

        container.addView(row);

        for (CatTreeNode child : node.children) {
            renderTree(host, container, child, depth + 1, selectedPath);
        }
    }

    public static void createConnectionDialog(MainActivity host) {
        class NodeItem {
            final String id;
            final String displayName;
            NodeItem(String id, String displayName) { this.id = id; this.displayName = displayName; }
        }

        List<NodeItem> allNodes = new ArrayList<>();
        for (String path : host.getAllCategoryPaths()) {
            allNodes.add(new NodeItem("folder:" + path, path.replace("/", " > ")));
        }
        for (Note n : host.notesList()) {
            String noteLocation = n.isUnbound() ? "" : " [" + n.displayCategory() + "]";
            allNodes.add(new NodeItem("note:" + n.fullPath(), n.title + noteLocation + " (" + host.str(R.string.str_note) + ")"));
        }
        for (LocalFileLink file : host.localFilesList()) {
            allNodes.add(new NodeItem(file.nodeId(), file.title + " [" + file.displayFolder() + "]"));
        }

        if (allNodes.size() < 2) {
            Toast.makeText(host, host.str(R.string.str_need_two_nodes), Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout layout = new LinearLayout(host);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(host.dp(20), host.dp(16), host.dp(20), host.dp(8));

        final String[] selectedSource = {null};
        final String[] selectedTarget = {null};

        layout.addView(host.text(host.str(R.string.str_source_node), 14, host.primaryText(), true));
        layout.addView(host.spacer(6));
        ScrollView srcScroll = new ScrollView(host);
        srcScroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, host.dp(120)));
        LinearLayout srcList = new LinearLayout(host);
        srcList.setOrientation(LinearLayout.VERTICAL);
        srcScroll.addView(srcList);
        layout.addView(srcScroll);

        layout.addView(host.spacer(10));
        layout.addView(host.text(host.str(R.string.str_target_node), 14, host.primaryText(), true));
        layout.addView(host.spacer(6));
        ScrollView tgtScroll = new ScrollView(host);
        tgtScroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, host.dp(120)));
        LinearLayout tgtList = new LinearLayout(host);
        tgtList.setOrientation(LinearLayout.VERTICAL);
        tgtScroll.addView(tgtList);
        layout.addView(tgtScroll);

        final View[] srcViews = new View[allNodes.size()];
        final View[] tgtViews = new View[allNodes.size()];

        for (int i = 0; i < allNodes.size(); i++) {
            NodeItem node = allNodes.get(i);
            TextView srcRow = host.text("  " + node.displayName, 14, host.primaryText(), false);
            srcRow.setPadding(host.dp(8), host.dp(10), host.dp(8), host.dp(10));
            srcRow.setOnClickListener(v -> {
                selectedSource[0] = node.id;
                for (View sv : srcViews) if (sv != null) sv.setBackgroundColor(Color.TRANSPARENT);
                srcRow.setBackgroundColor(Color.argb(40, 65, 120, 220));
            });
            srcViews[i] = srcRow;
            srcList.addView(srcRow);
            TextView tgtRow = host.text("  " + node.displayName, 14, host.primaryText(), false);
            tgtRow.setPadding(host.dp(8), host.dp(10), host.dp(8), host.dp(10));
            tgtRow.setOnClickListener(v -> {
                selectedTarget[0] = node.id;
                for (View tv : tgtViews) if (tv != null) tv.setBackgroundColor(Color.TRANSPARENT);
                tgtRow.setBackgroundColor(Color.argb(40, 65, 120, 220));
            });
            tgtViews[i] = tgtRow;
            tgtList.addView(tgtRow);
        }

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(host)
                .setTitle(host.str(R.string.str_create_connection))
                .setView(layout)
                .setPositiveButton(host.str(R.string.str_create), (d, w) -> {
                    if (selectedSource[0] == null || selectedTarget[0] == null) {
                        Toast.makeText(host, host.str(R.string.str_select_both_nodes), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String sourceId = host.normalizeConnectionNodeId(selectedSource[0]);
                    String targetId = host.normalizeConnectionNodeId(selectedTarget[0]);
                    if (sourceId.equalsIgnoreCase(targetId)) {
                        Toast.makeText(host, host.str(R.string.str_cannot_link_self), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    for (Connection conn : host.connectionsList()) {
                        String connSource = host.normalizeConnectionNodeId(conn.source);
                        String connTarget = host.normalizeConnectionNodeId(conn.target);
                        if ((connSource.equalsIgnoreCase(sourceId) && connTarget.equalsIgnoreCase(targetId)) ||
                            (connSource.equalsIgnoreCase(targetId) && connTarget.equalsIgnoreCase(sourceId))) {
                            Toast.makeText(host, host.str(R.string.str_connection_already_exists), Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    host.connectionsList().add(new Connection(sourceId, targetId));
                    host.savePersistentData();
                    host.pushGraphDataToWebView();
                    host.renderContent();
                    Toast.makeText(host, host.str(R.string.str_connection_created), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(host.str(R.string.str_cancel), null)
                .create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(host.roundedBg(host.surface(), 16, 0, Color.TRANSPARENT));
        dialog.show();
    }

    public static void showUnboundNotesDialog(MainActivity host) {
        LinearLayout layout = new LinearLayout(host);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(host.dp(16), host.dp(14), host.dp(16), host.dp(14));
        layout.addView(host.text(host.str(R.string.str_mindscape_root), 18, host.primaryText(), true));
        layout.addView(host.text(host.str(R.string.str_unbound_notes_are_shown_first_tap_a_note_to_bind_it_to_a_category_or_subcategory), 12, host.secondaryText(), false));
        layout.addView(host.spacer(8));

        List<Note> ordered = new ArrayList<>(host.notesList());
        ordered.sort((a, b) -> {
            if (a.isUnbound() == b.isUnbound()) return a.title.compareToIgnoreCase(b.title);
            return a.isUnbound() ? -1 : 1;
        });

        if (ordered.isEmpty()) {
            layout.addView(host.text(host.str(R.string.str_no_notes_yet), 14, host.secondaryText(), false));
        } else {
            for (Note note : ordered) {
                int rowBg = note.isUnbound()
                        ? (host.isDarkTheme() ? Color.rgb(58, 38, 42) : Color.rgb(255, 242, 242))
                        : host.surface();
                LinearLayout row = host.card(rowBg);
                row.setPadding(host.dp(12), host.dp(10), host.dp(12), host.dp(10));
                row.addView(host.text(note.title, 15, host.primaryText(), true));
                row.addView(host.text(note.displayCategory(), 12, note.isUnbound() ? Color.rgb(205, 80, 78) : host.secondaryText(), false));
                row.setOnClickListener(v -> host.showBindNoteDialog(note));
                layout.addView(row);
            }
        }

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(host)
                .setView(layout)
                .setNegativeButton(host.str(R.string.str_close), null)
                .create();
        dialog.getWindow().setBackgroundDrawable(host.roundedBg(host.surface(), 16, 0, Color.TRANSPARENT));
        dialog.show();
    }

    public static void showBindNoteDialog(MainActivity host, @Nullable Note preselectedNote) {
        if (host.notesList().isEmpty()) {
            Toast.makeText(host, host.str(R.string.str_no_notes_to_bind), Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> allPaths = host.getAllCategoryPaths();
        if (allPaths.isEmpty()) {
            Toast.makeText(host, host.str(R.string.str_create_a_category_or_subcategory_first), Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout layout = new LinearLayout(host);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(host.dp(16), host.dp(14), host.dp(16), host.dp(14));
        layout.addView(host.text(host.str(R.string.str_bind_note_to_node), 18, host.primaryText(), true));
        layout.addView(host.text(host.str(R.string.str_select_a_note_and_a_category_subcategory), 12, host.secondaryText(), false));
        layout.addView(host.spacer(8));

        final Note[] selectedNote = {preselectedNote != null ? preselectedNote : host.notesList().get(0)};
        final String[] selectedPath = {allPaths.get(0)};

        layout.addView(host.text(host.str(R.string.str_note), 12, host.secondaryText(), true));
        LinearLayout notesNodes = new LinearLayout(host);
        notesNodes.setOrientation(LinearLayout.VERTICAL);
        for (Note note : host.notesList()) {
            TextView node = host.text((note.isUnbound() ? "• " : "○ ") + note.title, 13, note.isUnbound() ? Color.rgb(205, 80, 78) : host.primaryText(), note == selectedNote[0]);
            node.setPadding(host.dp(10), host.dp(8), host.dp(10), host.dp(8));
            node.setBackground(host.roundedBg(note == selectedNote[0] ? Color.rgb(235, 244, 255) : Color.TRANSPARENT, 10, note == selectedNote[0] ? 1 : 0, host.accentColor()));
            node.setOnClickListener(v -> {
                selectedNote[0] = note;
                for (int i = 0; i < notesNodes.getChildCount(); i++) {
                    TextView child = (TextView) notesNodes.getChildAt(i);
                    boolean selected = child == v;
                    host.applyTextWeight(child, selected);
                    child.setBackground(host.roundedBg(selected ? Color.rgb(235, 244, 255) : Color.TRANSPARENT, 10, selected ? 1 : 0, host.accentColor()));
                }
            });
            notesNodes.addView(node);
        }
        layout.addView(notesNodes);
        layout.addView(host.spacer(8));

        layout.addView(host.text(host.str(R.string.str_bind_target), 12, host.secondaryText(), true));
        LinearLayout categoryNodes = new LinearLayout(host);
        categoryNodes.setOrientation(LinearLayout.VERTICAL);
        for (String path : allPaths) {
            TextView node = host.text("◎ " + path.replace("/", " > "), 13, host.primaryText(), path.equals(selectedPath[0]));
            node.setPadding(host.dp(10), host.dp(8), host.dp(10), host.dp(8));
            node.setBackground(host.roundedBg(path.equals(selectedPath[0]) ? Color.rgb(235, 244, 255) : Color.TRANSPARENT, 10, path.equals(selectedPath[0]) ? 1 : 0, host.accentColor()));
            node.setOnClickListener(v -> {
                selectedPath[0] = path;
                for (int i = 0; i < categoryNodes.getChildCount(); i++) {
                    TextView child = (TextView) categoryNodes.getChildAt(i);
                    boolean selected = child == v;
                    host.applyTextWeight(child, selected);
                    child.setBackground(host.roundedBg(selected ? Color.rgb(235, 244, 255) : Color.TRANSPARENT, 10, selected ? 1 : 0, host.accentColor()));
                }
            });
            categoryNodes.addView(node);
        }
        layout.addView(categoryNodes);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(host);
        builder.setView(layout);
        builder.setPositiveButton(host.str(R.string.str_bind), (dialog, which) -> {
            if (selectedNote[0] != null && selectedPath[0] != null) {
                selectedNote[0].categoryPath = selectedPath[0];
                host.renderContent();
            }
        });
        builder.setNegativeButton(host.str(R.string.str_cancel), null);

        android.app.AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(host.roundedBg(host.surface(), 16, 0, Color.TRANSPARENT));
        dialog.show();
    }

    public static void importDialog(MainActivity host) {
        LinearLayout layout = new LinearLayout(host);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(host.dp(20), host.dp(20), host.dp(20), host.dp(20));

        layout.addView(host.text(host.str(R.string.str_import_json_data), 18, host.primaryText(), true));
        layout.addView(host.spacer(10));

        EditText rawInput = host.createStyledInput("", "{\n  \"notes\": [],\n  \"categories\": [],\n  \"connections\": []\n}");
        rawInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        host.configureLargeTextInput(rawInput, 260);
        layout.addView(rawInput);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(host);
        builder.setView(host.scrollableDialogContent(layout, 620));
        builder.setPositiveButton(host.str(R.string.str_import), (dialog, which) -> {
            String json = rawInput.getText().toString().trim();
            try {
                JSONObject obj = new JSONObject(json);
                int importedNotes = 0, importedCats = 0, importedConns = 0, importedFiles = 0;

                if (obj.has("notes")) {
                    JSONArray notesArr = obj.getJSONArray("notes");
                    for (int i = 0; i < notesArr.length(); i++) {
                        JSONObject no = notesArr.getJSONObject(i);
                        String title = no.getString("title");
                        String category = no.optString("categoryPath", no.optString("category", ""));
                        String content = no.optString("content", "");
                        Note n = new Note(
                                title,
                                category,
                                content,
                                no.optLong("createdAt", System.currentTimeMillis()),
                                no.optLong("updatedAt", System.currentTimeMillis())
                        );
                        n.favorite = no.optBoolean("favorite", false);
                        n.reminderEnabled = no.optBoolean("reminderEnabled", false);
                        n.reminderAt = no.optLong("reminderAt", 0L);
                        n.reminderTriggered = no.optBoolean("reminderTriggered", false);
                        host.notesList().add(n);
                        host.scheduleNoteReminder(n);
                        importedNotes++;
                    }
                }
                if (obj.has("categories")) {
                    JSONArray catsArr = obj.getJSONArray("categories");
                    for (int i = 0; i < catsArr.length(); i++) {
                        JSONObject co = catsArr.getJSONObject(i);
                        String title = co.getString("title");
                        String desc = co.optString("description", "");
                        int col = co.optInt("color", Color.rgb(65, 120, 220));
                        String parent = co.optString("parent", null);
                        boolean isCenter = co.optBoolean("isCenter", false);
                        host.categoriesList().add(new Category(title, desc, col, parent, isCenter));
                        importedCats++;
                    }
                }
                if (obj.has("connections")) {
                    JSONArray connArr = obj.getJSONArray("connections");
                    for (int i = 0; i < connArr.length(); i++) {
                        JSONObject co = connArr.getJSONObject(i);
                        host.connectionsList().add(new Connection(co.getString("source"), co.getString("target")));
                        importedConns++;
                    }
                }
                if (obj.has("localFiles")) {
                    JSONArray filesArr = obj.getJSONArray("localFiles");
                    for (int i = 0; i < filesArr.length(); i++) {
                        JSONObject fo = filesArr.getJSONObject(i);
                        host.localFilesList().add(new LocalFileLink(
                                fo.optString("title", "file"),
                                fo.optString("folderPath", null),
                                fo.optString("uri", ""),
                                fo.optString("mimeType", ""),
                                fo.optLong("size", -1L),
                                fo.optLong("addedAt", System.currentTimeMillis())
                        ));
                        importedFiles++;
                    }
                }
                if (obj.has("hiddenNodes")) {
                    JSONArray hiddenArr = obj.getJSONArray("hiddenNodes");
                    for (int i = 0; i < hiddenArr.length(); i++) {
                        host.hiddenNodes().add(hiddenArr.getString(i));
                    }
                }
                if (obj.has("aiSessions")) {
                    JSONArray sessionsArr = obj.getJSONArray("aiSessions");
                    for (int i = 0; i < sessionsArr.length(); i++) {
                        JSONObject sessionObj = sessionsArr.getJSONObject(i);
                        ChatSession session = new ChatSession(sessionObj.optString("title", "Session"));
                        session.id = sessionObj.optString("id", session.id);
                        JSONArray messagesArr = sessionObj.optJSONArray("messages");
                        if (messagesArr != null) {
                            for (int j = 0; j < messagesArr.length(); j++) {
                                JSONObject messageObj = messagesArr.getJSONObject(j);
                                session.messages.add(new ChatMessage(
                                        messageObj.optBoolean("isAi", false),
                                        messageObj.optString("text", "")
                                ));
                            }
                        }
                        host.aiSessions().add(session);
                        if (session.id.equals(obj.optString("currentAiSessionId", null)))
                            host.currentAiSession(session);
                    }
                }
                host.migrateLegacyConnectionsAndHiddenNodes();

                String msg = "ru".equals(host.activeLanguage())
                        ? "Импортировано: " + importedNotes + " заметок, " + importedCats + " категорий, " + importedConns + " связей, " + importedFiles + " файлов"
                        : "Imported: " + importedNotes + " notes, " + importedCats + " categories, " + importedConns + " connections, " + importedFiles + " files";
                Toast.makeText(host, msg, Toast.LENGTH_LONG).show();
                host.savePersistentData();
                host.renderContent();
            } catch (Exception e) {
                Toast.makeText(host, "Не удалось импортировать JSON", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(host.str(R.string.str_cancel), null);
        builder.show();
    }
}
