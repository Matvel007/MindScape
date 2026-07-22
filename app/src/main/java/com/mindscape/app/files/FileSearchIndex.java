package com.mindscape.app.files;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.mindscape.app.LocalFileLink;
import com.mindscape.app.Note;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class FileSearchIndex extends SQLiteOpenHelper {
    private static final String DB_NAME = "mindscape_file_index.db";
    private static final int DB_VERSION = 2;
    private static final int MAX_INDEX_CHARS = 120_000;
    private static final int CHUNK_SIZE = 1800;
    private static final int CHUNK_OVERLAP = 180;

    public FileSearchIndex(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE file_index (node_id TEXT PRIMARY KEY, title TEXT, folder TEXT, mime TEXT, content TEXT, updated_at INTEGER)");
        db.execSQL("CREATE INDEX idx_file_index_title ON file_index(title)");
        db.execSQL("CREATE INDEX idx_file_index_folder ON file_index(folder)");
        createContentTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createContentTables(db);
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        createContentTables(db);
    }

    private void createContentTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS content_chunks (chunk_id TEXT PRIMARY KEY, node_id TEXT, kind TEXT, title TEXT, folder TEXT, mime TEXT, chunk_index INTEGER, content TEXT, updated_at INTEGER)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_content_chunks_node ON content_chunks(node_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_content_chunks_kind ON content_chunks(kind)");
        db.execSQL("CREATE TABLE IF NOT EXISTS embeddings (chunk_id TEXT PRIMARY KEY, model TEXT, dimensions INTEGER, vector BLOB, updated_at INTEGER)");
        try {
            db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS content_chunks_fts USING fts5(chunk_id UNINDEXED, node_id UNINDEXED, kind UNINDEXED, title, folder, content)");
        } catch (Exception error) {
            android.util.Log.w("MindScape", "FTS5 is unavailable, content index will use LIKE fallback", error);
        }
    }

    public void upsert(LocalFileLink file, String content) {
        ContentValues values = new ContentValues();
        values.put("node_id", file.nodeId());
        values.put("title", lower(file.title));
        values.put("folder", lower(file.displayFolder()));
        values.put("mime", lower(file.mimeType));
        values.put("content", lower(limit(content, MAX_INDEX_CHARS)));
        values.put("updated_at", System.currentTimeMillis());
        SQLiteDatabase db = getWritableDatabase();
        db.insertWithOnConflict("file_index", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        indexContent(db, file.nodeId(), "file", file.title, file.displayFolder(), file.mimeType, content);
    }

    public void remove(LocalFileLink file) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("file_index", "node_id = ?", new String[]{file.nodeId()});
        removeContent(db, file.nodeId());
    }

    public void clearAll() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("file_index", null, null);
            db.delete("content_chunks", null, null);
            db.delete("embeddings", null, null);
            try { db.delete("content_chunks_fts", null, null); } catch (Exception ignored) {}
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void indexNotes(List<Note> notes) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("content_chunks", "kind = ?", new String[]{"note"});
            try { db.delete("content_chunks_fts", "kind = ?", new String[]{"note"}); } catch (Exception ignored) {}
            for (Note note : notes) {
                indexContent(db, "note:" + note.fullPath(), "note", note.title, note.displayCategory(), "text/plain", note.content);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public boolean matches(LocalFileLink file, String query) {
        if (query == null || query.trim().isEmpty()) return true;
        String needle = lower(query.trim());
        if (lower(file.title).contains(needle) || lower(file.displayFolder()).contains(needle) || lower(file.mimeType).contains(needle)) {
            return true;
        }
        try (Cursor cursor = getReadableDatabase().query(
                "file_index",
                new String[]{"node_id"},
                "node_id = ? AND content LIKE ?",
                new String[]{file.nodeId(), "%" + needle + "%"},
                null,
                null,
                null,
                "1")) {
            if (cursor.moveToFirst()) return true;
        } catch (Exception error) {
            android.util.Log.w("MindScape", "Unable to search file index", error);
        }
        return contentMatches(file.nodeId(), needle);
    }

    public Set<String> searchNodeIds(String query, int limit) {
        Set<String> ids = new HashSet<>();
        if (query == null || query.trim().isEmpty()) return ids;
        String needle = "%" + lower(query.trim()) + "%";
        try (Cursor cursor = getReadableDatabase().query(
                "file_index",
                new String[]{"node_id"},
                "title LIKE ? OR folder LIKE ? OR mime LIKE ? OR content LIKE ?",
                new String[]{needle, needle, needle, needle},
                null,
                null,
                "updated_at DESC",
                String.valueOf(Math.max(1, limit)))) {
            while (cursor.moveToNext()) {
                ids.add(cursor.getString(0));
            }
        } catch (Exception error) {
            android.util.Log.w("MindScape", "Unable to collect file index matches", error);
        }
        for (IndexedSnippet snippet : searchRelevantContent(query, limit)) {
            if ("file".equals(snippet.kind)) ids.add(snippet.nodeId);
        }
        return ids;
    }

    private boolean contentMatches(String nodeId, String lowerNeedle) {
        try (Cursor cursor = getReadableDatabase().query(
                "content_chunks",
                new String[]{"chunk_id"},
                "node_id = ? AND (title LIKE ? OR folder LIKE ? OR content LIKE ?)",
                new String[]{nodeId, "%" + lowerNeedle + "%", "%" + lowerNeedle + "%", "%" + lowerNeedle + "%"},
                null,
                null,
                null,
                "1")) {
            return cursor.moveToFirst();
        } catch (Exception error) {
            android.util.Log.w("MindScape", "Unable to match content chunks", error);
            return false;
        }
    }

    public List<IndexedSnippet> searchRelevantContent(String query, int limit) {
        List<IndexedSnippet> snippets = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) return snippets;
        String ftsQuery = buildFtsQuery(query);
        if (!ftsQuery.isEmpty()) {
            try (Cursor cursor = getReadableDatabase().rawQuery(
                    "SELECT node_id, kind, title, folder, content FROM content_chunks_fts WHERE content_chunks_fts MATCH ? LIMIT ?",
                    new String[]{ftsQuery, String.valueOf(Math.max(1, limit))})) {
                while (cursor.moveToNext()) {
                    snippets.add(new IndexedSnippet(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4)));
                }
                if (!snippets.isEmpty()) return snippets;
            } catch (Exception error) {
                android.util.Log.w("MindScape", "Unable to search content FTS index", error);
            }
        }
        String needle = "%" + lower(query.trim()) + "%";
        try (Cursor cursor = getReadableDatabase().query(
                "content_chunks",
                new String[]{"node_id", "kind", "title", "folder", "content"},
                "title LIKE ? OR folder LIKE ? OR content LIKE ?",
                new String[]{needle, needle, needle},
                null,
                null,
                "updated_at DESC",
                String.valueOf(Math.max(1, limit)))) {
            while (cursor.moveToNext()) {
                snippets.add(new IndexedSnippet(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4)));
            }
        } catch (Exception error) {
            android.util.Log.w("MindScape", "Unable to search content chunks", error);
        }
        return snippets;
    }

    private void indexContent(SQLiteDatabase db, String nodeId, String kind, String title, String folder, String mime, String rawContent) {
        removeContent(db, nodeId);
        String content = limit(rawContent, MAX_INDEX_CHARS);
        if (content.trim().isEmpty()) return;
        long now = System.currentTimeMillis();
        int index = 0;
        for (String chunk : chunks(content)) {
            String chunkId = nodeId + "#" + index;
            ContentValues values = new ContentValues();
            values.put("chunk_id", chunkId);
            values.put("node_id", nodeId);
            values.put("kind", kind);
            values.put("title", title == null ? "" : title);
            values.put("folder", folder == null ? "" : folder);
            values.put("mime", mime == null ? "" : mime);
            values.put("chunk_index", index);
            values.put("content", chunk);
            values.put("updated_at", now);
            db.insertWithOnConflict("content_chunks", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            ContentValues fts = new ContentValues();
            fts.put("chunk_id", chunkId);
            fts.put("node_id", nodeId);
            fts.put("kind", kind);
            fts.put("title", title == null ? "" : title);
            fts.put("folder", folder == null ? "" : folder);
            fts.put("content", chunk);
            try { db.insert("content_chunks_fts", null, fts); } catch (Exception ignored) {}
            index++;
        }
    }

    private void removeContent(SQLiteDatabase db, String nodeId) {
        db.delete("content_chunks", "node_id = ?", new String[]{nodeId});
        db.delete("embeddings", "chunk_id LIKE ?", new String[]{nodeId + "#%"});
        try { db.delete("content_chunks_fts", "node_id = ?", new String[]{nodeId}); } catch (Exception ignored) {}
    }

    private static List<String> chunks(String content) {
        List<String> result = new ArrayList<>();
        if (content == null || content.isEmpty()) return result;
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(content.length(), start + CHUNK_SIZE);
            result.add(content.substring(start, end));
            if (end >= content.length()) break;
            start = Math.max(end - CHUNK_OVERLAP, start + 1);
        }
        return result;
    }

    private static String buildFtsQuery(String query) {
        StringBuilder builder = new StringBuilder();
        for (String part : lower(query).split("[^\\p{L}\\p{N}_]+")) {
            if (part.length() < 2) continue;
            if (builder.length() > 0) builder.append(" OR ");
            builder.append('"').append(part.replace("\"", "")).append('"');
        }
        return builder.toString();
    }

    public static final class IndexedSnippet {
        public final String nodeId;
        public final String kind;
        public final String title;
        public final String folder;
        public final String content;

        IndexedSnippet(String nodeId, String kind, String title, String folder, String content) {
            this.nodeId = nodeId;
            this.kind = kind;
            this.title = title;
            this.folder = folder;
            this.content = content;
        }
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static String limit(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }
}
