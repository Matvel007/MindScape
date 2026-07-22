package com.mindscape.app.ai;

import android.graphics.Color;

import com.mindscape.app.AiCommandParser;
import com.mindscape.app.Category;
import com.mindscape.app.Connection;
import com.mindscape.app.LocalFileLink;
import com.mindscape.app.MainActivity;
import com.mindscape.app.Note;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AiCommandExecutor {
    private AiCommandExecutor() {}

    public static void execute(MainActivity host, String response) {
        host.runOnUiThread(() -> {
            boolean changed = false;
            for (AiCommandParser.Command command : AiCommandParser.parse(response)) {
                String cmd = command.type;
                String[] args = command.args;
                try {
                    if (cmd.equals("CREATE_CENTER")) {
                        if (args.length < 1 || args[0].isEmpty()) continue;
                        String title = host.sanitizeInput(args[0], 100);
                        if (!host.categoryExists(title, null)) {
                            host.categoriesList().add(new Category(title, "", Color.rgb(65, 120, 220), null, true));
                            changed = true;
                        }
                    } else if (cmd.equals("MOVE_CENTER")) {
                        if (args.length < 2) continue;
                        String title = host.sanitizeInput(args[0], 100);
                        String dirArg = host.sanitizeInput(args[1], 10).toLowerCase(Locale.ROOT);
                        int dir = dirArg.startsWith("up") ? -1 : (dirArg.startsWith("down") ? 1 : 0);
                        if (dir == 0) continue;
                        Category target = null;
                        for (Category c : host.categoriesList()) {
                            if (c.isCenter && c.title.equalsIgnoreCase(title)) { target = c; break; }
                        }
                        if (target != null) {
                            host.moveCenter(target, dir);
                            changed = true;
                        }
                    } else if (cmd.equals("CREATE_CATEGORY")) {
                        if (args.length < 1 || args[0].isEmpty()) continue;
                        String title = host.sanitizeInput(args[0], 100);
                        String parent = args.length > 1 ? args[1] : "";
                        if (parent.isEmpty() || parent.equalsIgnoreCase("null") || parent.equalsIgnoreCase("none")) parent = null;
                        else parent = host.sanitizeInput(parent, 200);
                        if (!host.categoryExists(title, parent)) {
                            host.categoriesList().add(new Category(title, "", Color.rgb(65, 120, 220), parent));
                            changed = true;
                        }
                    } else if (cmd.equals("CREATE_NOTE")) {
                        if (args.length < 1 || args[0].isEmpty()) continue;
                        String title = host.sanitizeInput(args[0], 100);
                        String cat = args.length > 1 ? args[1] : null;
                        if (cat != null && (cat.isEmpty() || cat.equalsIgnoreCase("null") || cat.equalsIgnoreCase("none"))) cat = null;
                        else if (cat != null) cat = host.sanitizeInput(cat, 200);
                        String content = args.length > 2 ? args[2] : "";
                        if (content.length() > 50000) content = content.substring(0, 50000);
                        if (!host.noteExists(title, cat)) {
                            host.notesList().add(new Note(title, cat, content));
                            changed = true;
                        }
                    } else if (cmd.equals("CREATE_CONNECTION")) {
                        if (args.length >= 2) {
                            String srcRaw = host.sanitizeInput(args[0], 200);
                            String tgtRaw = host.sanitizeInput(args[1], 200);
                            String src = host.resolvePrefixedNodeId(srcRaw);
                            String tgt = host.resolvePrefixedNodeId(tgtRaw);
                            if (src.isEmpty() || tgt.isEmpty() || src.equalsIgnoreCase(tgt)) continue;
                            boolean exists = false;
                            for (Connection c : host.connectionsList()) {
                                if ((c.source.equalsIgnoreCase(src) && c.target.equalsIgnoreCase(tgt))
                                        || (c.source.equalsIgnoreCase(tgt) && c.target.equalsIgnoreCase(src))) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                host.connectionsList().add(new Connection(src, tgt));
                                changed = true;
                            }
                        }
                    } else if (cmd.equals("DELETE_NODE")) {
                        if (args.length < 1 || args[0].isEmpty()) continue;
                        String path = host.sanitizeInput(args[0], 200);
                        boolean deletedNote = false;
                        for (Note n : new ArrayList<>(host.notesList())) {
                            if (n.title.equalsIgnoreCase(path)) {
                                List<Note> single = new ArrayList<>(); single.add(n);
                                host.deleteNotes(single);
                                deletedNote = true;
                                break;
                            }
                        }
                        if (!deletedNote) host.deleteFolderSilent(path, true);
                        changed = true;
                    } else if (cmd.equals("HIDE_NODE")) {
                        if (args.length < 1 || args[0].isEmpty()) continue;
                        String rawNode = host.sanitizeInput(args[0], 200);
                        String resolved = host.resolvePrefixedNodeId(rawNode);
                        if (!resolved.isEmpty()) {
                            String clean = host.cleanNodeId(resolved);
                            if (resolved.startsWith("folder:") && host.categoryPathExists(clean)) {
                                host.setFolderSubtreeHidden(clean, true);
                            } else {
                                host.hiddenNodes().add(resolved);
                            }
                            changed = true;
                        }
                    } else if (cmd.equals("SHOW_NODE")) {
                        if (args.length < 1 || args[0].isEmpty()) continue;
                        String rawNode = host.sanitizeInput(args[0], 200);
                        String resolved = host.resolvePrefixedNodeId(rawNode);
                        if (!resolved.isEmpty()) {
                            String clean = host.cleanNodeId(resolved);
                            if (resolved.startsWith("folder:") && host.categoryPathExists(clean)) {
                                host.setFolderSubtreeHidden(clean, false);
                            } else {
                                host.hiddenNodes().remove(resolved);
                                host.hiddenNodes().remove(rawNode);
                            }
                            changed = true;
                        }
                    } else if (cmd.equals("DELETE_FILE_LINK")) {
                        if (args.length < 1 || args[0].isEmpty()) continue;
                        LocalFileLink file = host.findLocalFileLink(args[0]);
                        if (file != null) {
                            host.removeLocalFileLink(file, false);
                            changed = true;
                        }
                    } else if (cmd.equals("MOVE_FILE_LINK")) {
                        if (args.length < 2) continue;
                        LocalFileLink file = host.findLocalFileLink(args[0]);
                        String target = host.normalizeAiFolderTarget(args[1]);
                        if (file != null && (target == null || host.categoryPathExists(target))) {
                            file.folderPath = target;
                            host.reindexFileAsync(file);
                            changed = true;
                        }
                    } else if (cmd.equals("COPY_FILE_LINK")) {
                        if (args.length < 2) continue;
                        LocalFileLink file = host.findLocalFileLink(args[0]);
                        String target = host.normalizeAiFolderTarget(args[1]);
                        if (file != null && (target == null || host.categoryPathExists(target))) {
                            LocalFileLink copy = new LocalFileLink(file.title, target, file.uri, file.mimeType, file.size, System.currentTimeMillis());
                            host.localFilesList().add(copy);
                            host.reindexFileAsync(copy);
                            changed = true;
                        }
                    }
                } catch (Exception error) {
                    android.util.Log.w("MindScape", "Unable to execute AI command", error);
                }
            }

            if (changed) {
                host.savePersistentData();
                host.pushGraphDataToWebView();
                if (!"AI".equals(host.selectedSection())) {
                    host.renderContent();
                }
            }
        });
    }
}
