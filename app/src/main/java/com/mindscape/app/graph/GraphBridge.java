package com.mindscape.app.graph;

import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.mindscape.app.Category;
import com.mindscape.app.LocalFileLink;
import com.mindscape.app.Note;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JS ↔ Java мост для PixiJS-графа (WebView).
 * Методы {@link #onReady()} и {@link #onNodeClick(String)} помечены
 * {@link JavascriptInterface} и вызываются из JS.
 *
 * Разрешение клика по узлу (префиксные ID {@code note:/folder:/file:})
 * вынесено из MainActivity.java.
 */
public final class GraphBridge {

    private final GraphHost host;
    private final List<Note> notes;
    private final List<Category> categories;
    private final List<LocalFileLink> localFiles;

    public GraphBridge(GraphHost host, List<Note> notes, List<Category> categories, List<LocalFileLink> localFiles) {
        this.host = host;
        this.notes = notes;
        this.categories = categories;
        this.localFiles = localFiles;
    }

    @JavascriptInterface
    public void onReady() {
        String json = host.buildGraphJson();
        boolean dark = host.isDarkTheme();
        String base64 = android.util.Base64.encodeToString(json.getBytes(StandardCharsets.UTF_8), android.util.Base64.NO_WRAP);
        String js = host.graphOptionsJs()
                + "setGraphData(decodeURIComponent(escape(window.atob('" + base64 + "')))); setDarkMode(" + dark + ");"
                + "setTimeout(function(){ if(typeof centerMainCenter==='function'){ centerMainCenter(); } }, 0);";
        host.evalMapJs(js);
    }

    @JavascriptInterface
    public void onNodeClick(String title) {
        if (title == null || title.length() > 1000) return;
        final String sanitized = title.replaceAll("[<>\"']", "");

        String cleanPath = sanitized;
        boolean isNoteClick = sanitized.startsWith("note:");
        boolean isFolderClick = sanitized.startsWith("folder:");
        boolean isFileClick = sanitized.startsWith("file:");
        if (isNoteClick) cleanPath = sanitized.substring(5);
        else if (isFolderClick) cleanPath = sanitized.substring(7);
        else if (isFileClick) cleanPath = sanitized.substring(5);

        if (host.rootTitle().equals(cleanPath)) {
            host.onRootSelected();
            return;
        }

        Object resolved = null;
        if (isNoteClick) {
            for (Note n : notes) {
                if (n.fullPath().equalsIgnoreCase(cleanPath)) { resolved = n; break; }
            }
        } else if (isFileClick) {
            for (LocalFileLink file : localFiles) {
                if (file.nodeId().equalsIgnoreCase(sanitized) || file.uri.equals(cleanPath)) { resolved = file; break; }
            }
        } else if (isFolderClick) {
            for (Category c : categories) {
                if (c.fullPath().equalsIgnoreCase(cleanPath)) { resolved = c; break; }
            }
        } else {
            for (Note n : notes) {
                if (n.fullPath().equalsIgnoreCase(cleanPath) || n.title.equalsIgnoreCase(cleanPath)) { resolved = n; break; }
            }
            if (resolved == null) {
                for (Category c : categories) {
                    if (c.fullPath().equalsIgnoreCase(cleanPath) || c.title.equalsIgnoreCase(cleanPath)) { resolved = c; break; }
                }
            }
            if (resolved == null) {
                for (LocalFileLink file : localFiles) {
                    if (file.title.equalsIgnoreCase(cleanPath)) { resolved = file; break; }
                }
            }
        }
        if (resolved != null) host.onEntitySelected(resolved);
    }
}
