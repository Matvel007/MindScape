package com.mindscape.app.tree;

import androidx.annotation.Nullable;

import com.mindscape.app.Category;
import com.mindscape.app.Note;

import java.util.List;
import java.util.Set;

/**
 * Операции над {@code hiddenNodes}: скрытие/показ папок-поддеревьев и заметок
 * по каноническому префиксному ключу ({@code folder:/note:}).
 * Источник: вынесено из MainActivity.java (toggleFolderSubtreeHidden,
 * setFolderSubtreeHidden, setCategoryHidden, setNoteHidden, isPathInSubtree,
 * categoryPathExists, toggleNodeHidden, displayNodeId).
 */
public final class HiddenNodesManager {

    private HiddenNodesManager() {}

    public static boolean categoryPathExists(List<Category> categories, String path) {
        for (Category category : categories) {
            if (category.fullPath().equals(path)) return true;
        }
        return false;
    }

    public static boolean isPathInSubtree(@Nullable String path, String rootPath) {
        return path != null && (path.equals(rootPath) || path.startsWith(rootPath + "/"));
    }

    public static void toggleFolderSubtreeHidden(List<Category> categories, List<Note> notes, Set<String> hiddenNodes, String folderPath) {
        boolean currentlyHidden = com.mindscape.app.NodeStateManager.isNodeHidden(folderPath, hiddenNodes);
        setFolderSubtreeHidden(categories, notes, hiddenNodes, folderPath, !currentlyHidden);
    }

    public static void setFolderSubtreeHidden(List<Category> categories, List<Note> notes, Set<String> hiddenNodes, String folderPath, boolean hidden) {
        for (Category category : categories) {
            if (isPathInSubtree(category.fullPath(), folderPath)) {
                setCategoryHidden(hiddenNodes, category, hidden);
            }
        }
        for (Note note : notes) {
            if (!note.isUnbound() && isPathInSubtree(note.categoryPath, folderPath)) {
                setNoteHidden(hiddenNodes, note, hidden);
            }
        }
    }

    public static void setCategoryHidden(Set<String> hiddenNodes, Category category, boolean hidden) {
        String fullPath = category.fullPath();
        hiddenNodes.remove("folder:" + fullPath);
        hiddenNodes.remove("folder:" + category.title);
        hiddenNodes.remove(fullPath);
        hiddenNodes.remove(category.title);
        if (hidden) {
            hiddenNodes.add("folder:" + fullPath);
        }
    }

    public static void setNoteHidden(Set<String> hiddenNodes, Note note, boolean hidden) {
        String fullPath = note.fullPath();
        hiddenNodes.remove("note:" + fullPath);
        hiddenNodes.remove("note:" + note.title);
        hiddenNodes.remove(fullPath);
        hiddenNodes.remove(note.title);
        if (hidden) {
            hiddenNodes.add("note:" + fullPath);
        }
    }
}
