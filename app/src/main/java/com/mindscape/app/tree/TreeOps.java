package com.mindscape.app.tree;

import androidx.annotation.Nullable;

import com.mindscape.app.CatTreeNode;
import com.mindscape.app.Category;
import com.mindscape.app.Connection;
import com.mindscape.app.KnowledgeTreeService;
import com.mindscape.app.LocalFileLink;
import com.mindscape.app.Note;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Без state-хелперы над деревом категорий: дети/заметки/файлы папки,
 * поиск/проверка существования, центры. Делегирует в {@link KnowledgeTreeService}.
 * Источник: вынесено из MainActivity.java.
 */
public final class TreeOps {

    private TreeOps() {}

    public static List<Category> childFolders(List<Category> categories, @Nullable String parent) {
        return KnowledgeTreeService.childFolders(categories, parent);
    }

    public static List<Note> notesInFolder(List<Note> notes, @Nullable String folderPath) {
        return KnowledgeTreeService.notesInFolder(notes, folderPath);
    }

    public static List<LocalFileLink> localFilesInFolder(List<LocalFileLink> localFiles, @Nullable String folderPath) {
        List<LocalFileLink> result = new ArrayList<>();
        for (LocalFileLink file : localFiles) {
            if (folderPath == null ? file.isUnbound() : folderPath.equals(file.folderPath)) {
                result.add(file);
            }
        }
        result.sort(Comparator.comparing(f -> f.title.toLowerCase(Locale.ROOT)));
        return result;
    }

    public static String folderName(String path) {
        return KnowledgeTreeService.folderName(path);
    }

    @Nullable
    public static String parentPath(@Nullable String path) {
        return KnowledgeTreeService.parentPath(path);
    }

    public static List<Category> getCenters(List<Category> categories) {
        return KnowledgeTreeService.getCenters(categories);
    }

    public static List<Category> getLegacyTopLevel(List<Category> categories) {
        return KnowledgeTreeService.getLegacyTopLevel(categories);
    }

    public static CatTreeNode buildCategoryTree(List<Category> categories, String rootNodeTitle, String rootGroup) {
        return KnowledgeTreeService.buildCategoryTree(categories, rootNodeTitle, rootGroup);
    }

    public static boolean categoryExists(List<Category> categories, String title, String parentPath) {
        return KnowledgeTreeService.categoryExists(categories, title, parentPath);
    }

    public static boolean noteExists(List<Note> notes, String title, String catPath) {
        return KnowledgeTreeService.noteExists(notes, title, catPath);
    }

    public static int[] countContents(List<Category> categories, List<Note> notes, String rootPath) {
        return KnowledgeTreeService.countContents(categories, notes, rootPath);
    }

    public static List<String> getAllCategoryPaths(List<Category> categories) {
        return KnowledgeTreeService.getAllCategoryPaths(categories);
    }

    public static void duplicateCategoryContent(com.mindscape.app.MainActivity host, String oldPath, String newPath) {
        List<Connection> originalConnections = new ArrayList<>(host.connectionsList);
        Map<String, String> copiedNodeIds = new HashMap<>();
        duplicateCategoryContent(host, oldPath, newPath, copiedNodeIds);
        duplicateConnectionsForCopies(host, originalConnections, copiedNodeIds);
    }

    private static void duplicateCategoryContent(com.mindscape.app.MainActivity host, String oldPath, String newPath, Map<String, String> copiedNodeIds) {
        copiedNodeIds.put(oldPath, newPath);
        copiedNodeIds.put("folder:" + oldPath, "folder:" + newPath);

        List<Category> subFolders = new ArrayList<>();
        for (Category c : host.categoriesList) {
            if (oldPath.equals(c.parent)) subFolders.add(c);
        }

        for (Category child : subFolders) {
            java.util.Random r = new java.util.Random();
            int color = android.graphics.Color.rgb(100 + r.nextInt(120), 100 + r.nextInt(120), 100 + r.nextInt(120));

            String childTitle = child.title;
            int catSuffix = 1;
            while (host.categoryExistsGlobally(childTitle + " (" + catSuffix + ")")) {
                catSuffix++;
            }
            String newChildTitle = childTitle + " (" + catSuffix + ")";

            Category childCopy = new Category(newChildTitle, child.description, color, newPath, false);
            host.categoriesList.add(childCopy);

            duplicateCategoryContent(host, child.fullPath(), childCopy.fullPath(), copiedNodeIds);
        }

        List<Note> folderNotes = new ArrayList<>();
        for (Note n : host.notesList) {
            if (oldPath.equals(n.categoryPath)) folderNotes.add(n);
        }

        for (Note note : folderNotes) {
            String newNoteTitle = note.title;
            int noteSuffix = 1;
            while (host.noteExists(newNoteTitle + " (" + noteSuffix + ")", newPath)) {
                noteSuffix++;
            }
            newNoteTitle = newNoteTitle + " (" + noteSuffix + ")";

            Note noteCopy = new Note(newNoteTitle, newPath, note.content);
            noteCopy.favorite = note.favorite;
            noteCopy.reminderEnabled = note.reminderEnabled;
            noteCopy.reminderAt = note.reminderAt;
            noteCopy.reminderTriggered = note.reminderTriggered;
            noteCopy.quickContainerNote = note.quickContainerNote;
            host.notesList.add(noteCopy);
            copiedNodeIds.put(note.fullPath(), noteCopy.fullPath());
            copiedNodeIds.put("note:" + note.fullPath(), "note:" + noteCopy.fullPath());
        }

        List<LocalFileLink> folderFiles = new ArrayList<>();
        for (LocalFileLink file : host.localFilesList) {
            if (oldPath.equals(file.folderPath)) folderFiles.add(file);
        }

        long now = System.currentTimeMillis();
        int fileIndex = 0;
        for (LocalFileLink file : folderFiles) {
            LocalFileLink fileCopy = new LocalFileLink(
                    host.uniqueFileCopyTitle(file.title, newPath),
                    newPath,
                    file.uri,
                    file.mimeType,
                    file.size,
                    now + fileIndex++
            );
            host.localFilesList.add(fileCopy);
            host.reindexFileAsync(fileCopy);
            copiedNodeIds.put(file.nodeId(), fileCopy.nodeId());
        }
    }

    private static void duplicateConnectionsForCopies(com.mindscape.app.MainActivity host, List<Connection> originalConnections, Map<String, String> copiedNodeIds) {
        for (Connection connection : originalConnections) {
            String copiedSource = copiedNodeIds.get(connection.source);
            String copiedTarget = copiedNodeIds.get(connection.target);
            if (copiedSource == null && copiedTarget == null) continue;
            String source = copiedSource == null ? connection.source : copiedSource;
            String target = copiedTarget == null ? connection.target : copiedTarget;
            if (source.equalsIgnoreCase(target) || connectionExists(host, source, target)) continue;
            host.connectionsList.add(new Connection(source, target));
        }
    }

    private static boolean connectionExists(com.mindscape.app.MainActivity host, String source, String target) {
        for (Connection connection : host.connectionsList) {
            if (connection.source.equalsIgnoreCase(source) && connection.target.equalsIgnoreCase(target)) return true;
            if (connection.source.equalsIgnoreCase(target) && connection.target.equalsIgnoreCase(source)) return true;
        }
        return false;
    }

    public static void deleteFolderSilent(com.mindscape.app.MainActivity host, String folderPath, boolean deleteContents) {
        host.categoriesList.removeIf(c -> c.fullPath().equals(folderPath)
                                  || folderPath.equals(c.parent)
                                  || (c.parent != null && c.parent.startsWith(folderPath + "/")));
        if (deleteContents) {
            List<Note> toDelete = new ArrayList<>();
            for (Note note : host.notesList) {
                if (folderPath.equals(note.categoryPath) || (note.categoryPath != null && note.categoryPath.startsWith(folderPath + "/"))) {
                    toDelete.add(note);
                }
            }
            deleteNotesSilent(host, toDelete);
            List<LocalFileLink> filesToDelete = new ArrayList<>();
            for (LocalFileLink file : host.localFilesList) {
                if (folderPath.equals(file.folderPath) || (file.folderPath != null && file.folderPath.startsWith(folderPath + "/"))) {
                    filesToDelete.add(file);
                }
            }
            for (LocalFileLink file : filesToDelete) {
                host.fileSearchIndex().remove(file);
            }
            host.localFilesList.removeAll(filesToDelete);
        } else {
            for (Note note : host.notesList) {
                if (folderPath.equals(note.categoryPath) || (note.categoryPath != null && note.categoryPath.startsWith(folderPath + "/"))) {
                    String oldFullPath = note.fullPath();
                    note.categoryPath = null;
                    String newFullPath = note.fullPath();

                    String oldKey = "note:" + oldFullPath;
                    String newKey = "note:" + newFullPath;

                    for (Connection conn : host.connectionsList) {
                        if (conn.source.equalsIgnoreCase(oldKey)) conn.source = newKey;
                        else if (conn.source.equalsIgnoreCase(oldFullPath)) conn.source = newFullPath;

                        if (conn.target.equalsIgnoreCase(oldKey)) conn.target = newKey;
                        else if (conn.target.equalsIgnoreCase(oldFullPath)) conn.target = newFullPath;
                    }

                    if (host.hiddenNodes.contains(oldKey)) {
                        host.hiddenNodes.remove(oldKey);
                        host.hiddenNodes.add(newKey);
                    }
                    if (host.hiddenNodes.contains(oldFullPath)) {
                        host.hiddenNodes.remove(oldFullPath);
                        host.hiddenNodes.add(newFullPath);
                    }
                }
            }
            for (LocalFileLink file : host.localFilesList) {
                if (folderPath.equals(file.folderPath) || (file.folderPath != null && file.folderPath.startsWith(folderPath + "/"))) {
                    file.folderPath = null;
                }
            }
        }
        String fPrefix = "folder:" + folderPath;
        String fSlash = "folder:" + folderPath + "/";
        host.connectionsList.removeIf(conn ->
            conn.source.equals(folderPath) || conn.source.startsWith(folderPath + "/") ||
            conn.target.equals(folderPath) || conn.target.startsWith(folderPath + "/") ||
            conn.source.equals(fPrefix) || conn.source.startsWith(fSlash) ||
            conn.target.equals(fPrefix) || conn.target.startsWith(fSlash)
        );
        host.hiddenNodes.remove(folderPath);
        host.hiddenNodes.remove(fPrefix);
        host.hiddenNodes.removeIf(h -> h.startsWith(folderPath + "/") || h.startsWith(fSlash));

        String parent = parentPath(folderPath);
        if (parent != null) host.expandedFolders.add(parent);
    }

    public static void deleteNotesSilent(com.mindscape.app.MainActivity host, List<Note> notesToDelete) {
        java.util.Set<String> deletedKeys = new java.util.HashSet<>();
        for (Note note : notesToDelete) {
            String fullPath = note.fullPath();
            String title = note.title;
            host.cancelNoteReminder(fullPath);
            deletedKeys.add(("note:" + fullPath).toLowerCase(Locale.ROOT));
            deletedKeys.add(("note:" + title).toLowerCase(Locale.ROOT)); // legacy
            deletedKeys.add(fullPath.toLowerCase(Locale.ROOT)); // legacy fallback
            deletedKeys.add(title.toLowerCase(Locale.ROOT)); // legacy fallback

            host.hiddenNodes.remove("note:" + fullPath);
            host.hiddenNodes.remove("note:" + title);
            host.hiddenNodes.remove(fullPath);
            host.hiddenNodes.remove(title);
        }
        host.notesList.removeAll(notesToDelete);
        host.connectionsList.removeIf(connection ->
                deletedKeys.contains(connection.source.toLowerCase(Locale.ROOT))
                        || deletedKeys.contains(connection.target.toLowerCase(Locale.ROOT)));
        if (host.selectedMapEntity instanceof Note) {
            String selKey = ("note:" + ((Note) host.selectedMapEntity).fullPath()).toLowerCase(Locale.ROOT);
            if (deletedKeys.contains(selKey)) {
                host.selectedMapEntity = null;
            }
        }
    }

    public static void moveCenter(com.mindscape.app.MainActivity host, Category center, int dir) {
        List<Integer> centerIdx = new ArrayList<>();
        for (int i = 0; i < host.categoriesList.size(); i++) {
            Category c = host.categoriesList.get(i);
            if (c.isCenter && (c.parent == null || c.parent.isEmpty())) centerIdx.add(i);
        }
        int curPos = -1;
        for (int p = 0; p < centerIdx.size(); p++) {
            if (host.categoriesList.get(centerIdx.get(p)) == center) { curPos = p; break; }
        }
        int newPos = curPos + dir;
        if (curPos < 0 || newPos < 0 || newPos >= centerIdx.size()) return;
        int i1 = centerIdx.get(curPos);
        int i2 = centerIdx.get(newPos);
        Category tmp = host.categoriesList.get(i1);
        host.categoriesList.set(i1, host.categoriesList.get(i2));
        host.categoriesList.set(i2, tmp);
        host.pushGraphDataToWebView();
        host.renderContent();
    }
}
