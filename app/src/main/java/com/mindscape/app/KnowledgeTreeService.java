package com.mindscape.app;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class KnowledgeTreeService {
    private KnowledgeTreeService() {
    }

    public static List<Category> childFolders(List<Category> categories, @Nullable String parent) {
        List<Category> result = new ArrayList<>();
        for (Category folder : categories) {
            if (parent == null ? folder.parent == null || folder.parent.isEmpty() : parent.equals(folder.parent)) {
                result.add(folder);
            }
        }
        result.sort(Comparator.comparing(c -> c.title.toLowerCase(Locale.ROOT)));
        return result;
    }

    public static List<Note> notesInFolder(List<Note> notes, @Nullable String folderPath) {
        List<Note> result = new ArrayList<>();
        for (Note note : notes) {
            if (folderPath == null ? note.isUnbound() : folderPath.equals(note.categoryPath)) {
                result.add(note);
            }
        }
        result.sort(Comparator.comparing(n -> n.title.toLowerCase(Locale.ROOT)));
        return result;
    }

    public static String folderName(String path) {
        if (path == null || path.isEmpty()) return "Структура";
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    @Nullable
    public static String parentPath(@Nullable String path) {
        if (path == null || !path.contains("/")) return null;
        return path.substring(0, path.lastIndexOf('/'));
    }

    public static boolean categoryExists(List<Category> categories, String title, String parentPath) {
        for (Category category : categories) {
            boolean titleMatch = category.title.equalsIgnoreCase(title);
            boolean parentMatch = parentPath == null || parentPath.isEmpty()
                    ? category.parent == null || category.parent.isEmpty()
                    : parentPath.equalsIgnoreCase(category.parent);
            if (titleMatch && parentMatch) return true;
        }
        return false;
    }

    public static boolean noteExists(List<Note> notes, String title, String catPath) {
        String targetPath = catPath == null ? "" : catPath.trim();
        for (Note note : notes) {
            if (!note.title.equalsIgnoreCase(title)) continue;
            String notePath = note.categoryPath == null ? "" : note.categoryPath.trim();
            if (targetPath.equalsIgnoreCase(notePath)) return true;
        }
        return false;
    }

    public static int[] countContents(List<Category> categories, List<Note> notes, String rootPath) {
        int subs = 0;
        int noteCount = 0;
        for (Category category : categories) {
            if (category.parent != null && (category.parent.equals(rootPath) || category.parent.startsWith(rootPath + "/"))) {
                subs++;
            }
        }
        for (Note note : notes) {
            if (note.categoryPath != null && (note.categoryPath.equals(rootPath) || note.categoryPath.startsWith(rootPath + "/"))) {
                noteCount++;
            }
        }
        return new int[]{subs, noteCount};
    }

    public static List<String> getAllCategoryPaths(List<Category> categories) {
        List<String> paths = new ArrayList<>();
        for (Category category : categories) {
            paths.add(category.fullPath());
        }
        Collections.sort(paths);
        return paths;
    }

    public static List<Note> unboundNotes(List<Note> notes) {
        List<Note> result = new ArrayList<>();
        for (Note note : notes) {
            if (note.isUnbound()) {
                result.add(note);
            }
        }
        return result;
    }

    public static List<Category> getCenters(List<Category> categories) {
        List<Category> result = new ArrayList<>();
        for (Category category : categories) {
            if (category.isCenter && (category.parent == null || category.parent.isEmpty())) {
                result.add(category);
            }
        }
        return result;
    }

    public static List<Category> getLegacyTopLevel(List<Category> categories) {
        List<Category> result = new ArrayList<>();
        for (Category category : categories) {
            if (!category.isCenter && (category.parent == null || category.parent.isEmpty())) {
                result.add(category);
            }
        }
        return result;
    }

    public static CatTreeNode buildCategoryTree(List<Category> categories, String rootNodeTitle, String rootGroup) {
        CatTreeNode root = new CatTreeNode(rootNodeTitle, rootGroup);
        Map<String, CatTreeNode> map = new HashMap<>();
        map.put(rootGroup, root);
        List<Category> sorted = new ArrayList<>(categories);
        sorted.sort(Comparator.comparing(Category::fullPath));
        for (Category category : sorted) {
            String path = category.fullPath();
            CatTreeNode node = new CatTreeNode(category.title, path);
            map.put(path, node);
            if (category.parent == null || category.parent.isEmpty()) {
                root.children.add(node);
            } else {
                CatTreeNode parent = map.get(category.parent);
                if (parent != null) parent.children.add(node);
                else root.children.add(node);
            }
        }
        return root;
    }
}
