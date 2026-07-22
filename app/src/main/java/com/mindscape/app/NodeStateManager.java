package com.mindscape.app;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class NodeStateManager {
    private NodeStateManager() {
    }

    public static void migrateLegacyConnectionsAndHiddenNodes(
            List<Connection> connections,
            Set<String> hiddenNodes,
            List<Note> notes,
            List<Category> categories
    ) {
        for (Connection conn : connections) {
            conn.source = normalizeConnectionNodeId(conn.source);
            conn.target = normalizeConnectionNodeId(conn.target);
            if (!isPrefixedNodeId(conn.source)) {
                conn.source = resolvePrefixedNodeId(conn.source, notes, categories);
            }
            if (!isPrefixedNodeId(conn.target)) {
                conn.target = resolvePrefixedNodeId(conn.target, notes, categories);
            }
        }

        Set<String> migratedHidden = new HashSet<>();
        for (String hidden : hiddenNodes) {
            if (isPrefixedNodeId(hidden)) {
                migratedHidden.add(hidden);
            } else {
                migratedHidden.add(resolvePrefixedNodeId(hidden, notes, categories));
            }
        }
        hiddenNodes.clear();
        hiddenNodes.addAll(migratedHidden);
    }

    public static String cleanNodeId(String id) {
        if (id == null) return "";
        id = normalizeConnectionNodeId(id);
        if (id.startsWith("folder:")) return id.substring(7);
        if (id.startsWith("note:")) return id.substring(5);
        if (id.startsWith("file:")) return id.substring(5);
        return id;
    }

    public static String normalizeConnectionNodeId(String id) {
        if (id == null) return "";
        String value = id.trim();
        while (value.startsWith("folder:file:") || value.startsWith("note:file:")) {
            value = value.substring(value.indexOf("file:"));
        }
        return value;
    }

    public static String resolvePrefixedNodeId(String rawName, List<Note> notes, List<Category> categories) {
        if (rawName == null || rawName.isEmpty()) return "";
        for (Note note : notes) {
            if (note.fullPath().equalsIgnoreCase(rawName) || note.title.equalsIgnoreCase(rawName)) {
                return "note:" + note.fullPath();
            }
        }
        for (Category category : categories) {
            if (category.fullPath().equalsIgnoreCase(rawName)) {
                return "folder:" + category.fullPath();
            }
        }
        for (Category category : categories) {
            if (category.title.equalsIgnoreCase(rawName)) {
                return "folder:" + category.fullPath();
            }
        }
        return "folder:" + rawName;
    }

    public static boolean isNodeLinked(Object entity, List<Connection> connections) {
        Set<String> keys = candidateKeys(entity);
        if (keys.isEmpty()) return false;

        for (Connection connection : connections) {
            String source = normalizeConnectionNodeId(connection.source).toLowerCase(Locale.ROOT);
            String target = normalizeConnectionNodeId(connection.target).toLowerCase(Locale.ROOT);
            if (keys.contains(source) || keys.contains(target)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNodeHidden(Object entity, Set<String> hiddenNodes) {
        if (entity instanceof Note) {
            Note note = (Note) entity;
            return hiddenNodes.contains("note:" + note.fullPath())
                    || hiddenNodes.contains("note:" + note.title)
                    || hiddenNodes.contains(note.fullPath())
                    || hiddenNodes.contains(note.title);
        } else if (entity instanceof Category) {
            Category category = (Category) entity;
            return hiddenNodes.contains("folder:" + category.fullPath())
                    || hiddenNodes.contains("folder:" + category.title)
                    || hiddenNodes.contains(category.fullPath())
                    || hiddenNodes.contains(category.title);
        } else if (entity instanceof LocalFileLink) {
            LocalFileLink file = (LocalFileLink) entity;
            return hiddenNodes.contains(file.nodeId())
                    || hiddenNodes.contains(file.title)
                    || hiddenNodes.contains(file.uri);
        } else if (entity instanceof String) {
            String path = (String) entity;
            return hiddenNodes.contains("folder:" + path)
                    || hiddenNodes.contains("folder:" + folderName(path))
                    || hiddenNodes.contains(path)
                    || hiddenNodes.contains(folderName(path));
        }
        return false;
    }

    public static void toggleNodeHidden(Object entity, Set<String> hiddenNodes) {
        if (entity instanceof Note) {
            Note note = (Note) entity;
            String key = "note:" + note.fullPath();
            String legacyKey = note.fullPath();
            if (hiddenNodes.contains(key)) {
                hiddenNodes.remove(key);
            } else if (hiddenNodes.contains(legacyKey)) {
                hiddenNodes.remove(legacyKey);
            } else {
                hiddenNodes.add(key);
            }
        } else if (entity instanceof Category) {
            Category category = (Category) entity;
            toggleFolderHidden(category.fullPath(), category.title, hiddenNodes);
        } else if (entity instanceof LocalFileLink) {
            LocalFileLink file = (LocalFileLink) entity;
            String key = file.nodeId();
            if (hiddenNodes.contains(key)) {
                hiddenNodes.remove(key);
            } else if (hiddenNodes.contains(file.title)) {
                hiddenNodes.remove(file.title);
            } else if (hiddenNodes.contains(file.uri)) {
                hiddenNodes.remove(file.uri);
            } else {
                hiddenNodes.add(key);
            }
        } else if (entity instanceof String) {
            String folderPath = (String) entity;
            toggleFolderHidden(folderPath, folderName(folderPath), hiddenNodes);
        }
    }

    private static Set<String> candidateKeys(Object entity) {
        Set<String> keys = new HashSet<>();
        if (entity instanceof Note) {
            Note note = (Note) entity;
            keys.add(("note:" + note.fullPath()).toLowerCase(Locale.ROOT));
            keys.add(("note:" + note.title).toLowerCase(Locale.ROOT));
            keys.add(note.fullPath().toLowerCase(Locale.ROOT));
            keys.add(note.title.toLowerCase(Locale.ROOT));
        } else if (entity instanceof Category) {
            Category category = (Category) entity;
            keys.add(("folder:" + category.fullPath()).toLowerCase(Locale.ROOT));
            keys.add(("folder:" + category.title).toLowerCase(Locale.ROOT));
            keys.add(category.fullPath().toLowerCase(Locale.ROOT));
            keys.add(category.title.toLowerCase(Locale.ROOT));
        } else if (entity instanceof LocalFileLink) {
            LocalFileLink file = (LocalFileLink) entity;
            String nodeId = file.nodeId();
            keys.add(nodeId.toLowerCase(Locale.ROOT));
            keys.add(("folder:" + nodeId).toLowerCase(Locale.ROOT));
            keys.add(("note:" + nodeId).toLowerCase(Locale.ROOT));
            if (file.title != null) keys.add(file.title.toLowerCase(Locale.ROOT));
            if (file.uri != null) keys.add(file.uri.toLowerCase(Locale.ROOT));
        } else if (entity instanceof String) {
            String path = (String) entity;
            keys.add(path.toLowerCase(Locale.ROOT));
            keys.add(("folder:" + path).toLowerCase(Locale.ROOT));
            keys.add(("note:" + path).toLowerCase(Locale.ROOT));
            keys.add(("file:" + path).toLowerCase(Locale.ROOT));
        }
        return keys;
    }

    private static void toggleFolderHidden(String path, String title, Set<String> hiddenNodes) {
        String keyPath = "folder:" + path;
        String keyTitle = "folder:" + title;
        if (hiddenNodes.contains(keyPath)) {
            hiddenNodes.remove(keyPath);
            hiddenNodes.remove(keyTitle);
        } else if (hiddenNodes.contains(path)) {
            hiddenNodes.remove(path);
            hiddenNodes.remove(title);
        } else {
            hiddenNodes.add(keyPath);
            hiddenNodes.add(keyTitle);
        }
    }

    private static boolean isPrefixedNodeId(String id) {
        return id != null && (id.startsWith("folder:") || id.startsWith("note:") || id.startsWith("file:"));
    }

    private static String folderName(String path) {
        if (path == null || path.isEmpty()) return "";
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }
}
