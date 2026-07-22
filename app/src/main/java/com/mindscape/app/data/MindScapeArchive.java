package com.mindscape.app.data;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import com.mindscape.app.Category;
import com.mindscape.app.ChatMessage;
import com.mindscape.app.ChatSession;
import com.mindscape.app.Connection;
import com.mindscape.app.LocalFileLink;
import com.mindscape.app.MainActivity;
import com.mindscape.app.Note;
import com.mindscape.app.files.LocalFileReader;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MindScapeArchive {
    public static final long MAX_EMBEDDED_FILE_BYTES = 2_000_000_000L;
    public static final String MIME = "application/vnd.mindscape.archive";

    private MindScapeArchive() {}

    public static final class ImportStats {
        public int notes;
        public int categories;
        public int connections;
        public int files;
        public int extractedFiles;
        public final List<String> failedFiles = new ArrayList<>();
    }

    public static void exportToUri(MainActivity host, Uri outputUri) throws Exception {
        File dbFile = exportToTempFile(host);
        try (InputStream in = new java.io.FileInputStream(dbFile);
             OutputStream out = host.getContentResolver().openOutputStream(outputUri, "wt")) {
            if (out == null) throw new Exception("Unable to open archive output");
            copy(in, out);
        } finally {
            dbFile.delete();
        }
    }

    public static File exportToTempFile(MainActivity host) throws Exception {
        File dbFile = File.createTempFile("mindscape-export", ".msarchive", host.getCacheDir());
        if (dbFile.exists()) dbFile.delete();
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
        try {
            createSchema(db);
            insertMetadata(db, "format", "mindscape-sqlar");
            insertMetadata(db, "version", "1");
            insertMetadata(db, "created_at", String.valueOf(System.currentTimeMillis()));
            exportCategories(db, host.categoriesList());
            exportNotes(db, host.notesList());
            exportConnections(db, host.connectionsList());
            exportHiddenNodes(db, host.hiddenNodes());
            exportChatSessions(db, host.aiSessions(), host.currentAiSession());
            exportLocalFiles(host, db, host.localFilesList());
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
        return dbFile;
    }

    public static ImportStats importFromUri(MainActivity host, Uri inputUri, boolean extractFiles) throws Exception {
        try {
            host.getContentResolver().takePersistableUriPermission(inputUri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
            // Some content Uris are readable for the current grant but not persistable.
        }
        File dbFile = copyUriToTemp(host, inputUri);
        SQLiteDatabase db = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        ImportStats stats = new ImportStats();
        ArchiveData data = new ArchiveData();
        try {
            validateArchive(db);
            importCategories(db, data, stats);
            importNotes(db, data, stats);
            importChatSessions(db, data);
            Map<String, String> nodeIdMap = importLocalFiles(db, data, host, inputUri, extractFiles, stats);
            importConnections(db, data, nodeIdMap, stats);
            importHiddenNodes(db, data, nodeIdMap);
        } finally {
            db.close();
            dbFile.delete();
        }
        host.notesList().clear();
        host.categoriesList().clear();
        host.connectionsList().clear();
        host.localFilesList().clear();
        host.hiddenNodes().clear();
        host.aiSessions().clear();
        host.selectedNotes().clear();
        host.selectedLocalFiles().clear();
        host.structureSelectedNotes().clear();
        host.structureSelectedFolders().clear();
        host.structureSelectedFiles().clear();
        host.structureSelectionMode(false);
        host.currentAiSession(null);
        host.categoriesList().addAll(data.categories);
        host.notesList().addAll(data.notes);
        host.connectionsList().addAll(data.connections);
        host.localFilesList().addAll(data.localFiles);
        host.hiddenNodes().addAll(data.hiddenNodes);
        host.aiSessions().addAll(data.chatSessions);
        host.currentAiSession(data.currentChatSession);
        host.currentChatMessages(data.currentChatSession == null ? new ArrayList<>() : new ArrayList<>(data.currentChatSession.messages));
        for (Note note : host.notesList()) host.scheduleNoteReminder(note);
        host.migrateLegacyConnectionsAndHiddenNodes();
        return stats;
    }

    private static final class ArchiveData {
        final List<Category> categories = new ArrayList<>();
        final List<Note> notes = new ArrayList<>();
        final List<Connection> connections = new ArrayList<>();
        final List<LocalFileLink> localFiles = new ArrayList<>();
        final java.util.Set<String> hiddenNodes = new java.util.HashSet<>();
        final List<ChatSession> chatSessions = new ArrayList<>();
        ChatSession currentChatSession;
    }

    private static void validateArchive(SQLiteDatabase db) throws Exception {
        requireTable(db, "metadata");
        requireTable(db, "categories");
        requireTable(db, "notes");
        requireTable(db, "connections");
        requireTable(db, "local_files");
        requireTable(db, "sqlar");
    }

    private static void requireTable(SQLiteDatabase db, String name) throws Exception {
        try (Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[]{name})) {
            if (!cursor.moveToFirst()) throw new Exception("invalid_archive_missing_" + name);
        }
    }

    public static byte[] readSqlarBytes(Context context, Uri archiveUri, String entryName, long limit) throws Exception {
        File dbFile = copyUriToTemp(context, archiveUri);
        SQLiteDatabase db = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        try {
            return readSqlarBytes(db, entryName, limit);
        } finally {
            db.close();
            dbFile.delete();
        }
    }

    public static boolean isSqlarUri(String uri) {
        return uri != null && uri.startsWith("sqlar:");
    }

    public static Uri sqlarArchiveUri(String uri) {
        int hash = uri == null ? -1 : uri.indexOf('#');
        String encoded = hash < 0 ? uri.substring("sqlar:".length()) : uri.substring("sqlar:".length(), hash);
        return Uri.parse(Uri.decode(encoded));
    }

    public static String sqlarEntryName(String uri) {
        int hash = uri == null ? -1 : uri.indexOf('#');
        return hash < 0 ? "" : Uri.decode(uri.substring(hash + 1));
    }

    public static String buildSqlarUri(Uri archiveUri, String entryName) {
        return "sqlar:" + Uri.encode(archiveUri.toString()) + "#" + Uri.encode(entryName);
    }

    private static void createSchema(SQLiteDatabase db) {
        db.beginTransaction();
        db.execSQL("CREATE TABLE metadata (key TEXT PRIMARY KEY, value TEXT)");
        db.execSQL("CREATE TABLE categories (title TEXT, description TEXT, color INTEGER, parent TEXT, is_center INTEGER)");
        db.execSQL("CREATE TABLE notes (title TEXT, category_path TEXT, content TEXT, favorite INTEGER, created_at INTEGER, updated_at INTEGER, reminder_enabled INTEGER, reminder_at INTEGER, reminder_triggered INTEGER, quick_container_note INTEGER)");
        db.execSQL("CREATE TABLE connections (source TEXT, target TEXT)");
        db.execSQL("CREATE TABLE hidden_nodes (node_id TEXT PRIMARY KEY)");
        db.execSQL("CREATE TABLE ai_sessions (id TEXT PRIMARY KEY, title TEXT, is_current INTEGER)");
        db.execSQL("CREATE TABLE ai_messages (session_id TEXT, position INTEGER, is_ai INTEGER, text TEXT)");
        db.execSQL("CREATE TABLE local_files (title TEXT, folder_path TEXT, uri TEXT, mime_type TEXT, size INTEGER, added_at INTEGER, archive_entry TEXT, original_node_id TEXT)");
        db.execSQL("CREATE TABLE content_chunks (chunk_id TEXT PRIMARY KEY, node_id TEXT, kind TEXT, title TEXT, folder TEXT, mime TEXT, chunk_index INTEGER, content TEXT, updated_at INTEGER)");
        db.execSQL("CREATE TABLE embeddings (chunk_id TEXT PRIMARY KEY, model TEXT, dimensions INTEGER, vector BLOB, updated_at INTEGER)");
        db.execSQL("CREATE TABLE sqlar (name TEXT PRIMARY KEY, mode INT, mtime INT, sz INT, data BLOB)");
    }

    private static void insertMetadata(SQLiteDatabase db, String key, String value) {
        ContentValues values = new ContentValues();
        values.put("key", key);
        values.put("value", value);
        db.insert("metadata", null, values);
    }

    private static void exportCategories(SQLiteDatabase db, List<Category> categories) {
        for (Category c : categories) {
            ContentValues v = new ContentValues();
            v.put("title", c.title);
            v.put("description", c.description);
            v.put("color", c.color);
            v.put("parent", c.parent);
            v.put("is_center", c.isCenter ? 1 : 0);
            db.insert("categories", null, v);
        }
    }

    private static void exportNotes(SQLiteDatabase db, List<Note> notes) {
        for (Note n : notes) {
            ContentValues v = new ContentValues();
            v.put("title", n.title);
            v.put("category_path", n.categoryPath);
            v.put("content", n.content);
            v.put("favorite", n.favorite ? 1 : 0);
            v.put("created_at", n.createdAt);
            v.put("updated_at", n.updatedAt);
            v.put("reminder_enabled", n.reminderEnabled ? 1 : 0);
            v.put("reminder_at", n.reminderAt);
            v.put("reminder_triggered", n.reminderTriggered ? 1 : 0);
            v.put("quick_container_note", n.quickContainerNote ? 1 : 0);
            db.insert("notes", null, v);
        }
    }

    private static void exportConnections(SQLiteDatabase db, List<Connection> connections) {
        for (Connection c : connections) {
            ContentValues v = new ContentValues();
            v.put("source", c.source);
            v.put("target", c.target);
            db.insert("connections", null, v);
        }
    }

    private static void exportHiddenNodes(SQLiteDatabase db, java.util.Set<String> hiddenNodes) {
        for (String node : hiddenNodes) {
            ContentValues v = new ContentValues();
            v.put("node_id", node);
            db.insert("hidden_nodes", null, v);
        }
    }

    private static void exportChatSessions(SQLiteDatabase db, List<ChatSession> sessions, ChatSession current) {
        for (ChatSession s : sessions) {
            ContentValues session = new ContentValues();
            session.put("id", s.id);
            session.put("title", s.title);
            session.put("is_current", current != null && s.id.equals(current.id) ? 1 : 0);
            db.insert("ai_sessions", null, session);
            for (int i = 0; i < s.messages.size(); i++) {
                ChatMessage m = s.messages.get(i);
                ContentValues msg = new ContentValues();
                msg.put("session_id", s.id);
                msg.put("position", i);
                msg.put("is_ai", m.isAi ? 1 : 0);
                msg.put("text", m.text);
                db.insert("ai_messages", null, msg);
            }
        }
    }

    private static void exportLocalFiles(MainActivity host, SQLiteDatabase db, List<LocalFileLink> files) {
        Map<String, String> entryByUri = new HashMap<>();
        for (LocalFileLink file : files) {
            String originalNodeId = file.nodeId();
            String entry = entryByUri.get(file.uri);
            if (entry == null) entry = "";
            try {
                if (entry.isEmpty() && (file.size <= 0 || file.size <= MAX_EMBEDDED_FILE_BYTES)) {
                    byte[] bytes = readFileBytesForArchive(host, file);
                    if (bytes != null) {
                        entry = "files/" + entryByUri.size() + "_" + safeName(file.title);
                        ContentValues sqlar = new ContentValues();
                        sqlar.put("name", entry);
                        sqlar.put("mode", 0100644);
                        sqlar.put("mtime", System.currentTimeMillis() / 1000L);
                        sqlar.put("sz", bytes.length);
                        sqlar.put("data", bytes);
                        db.insert("sqlar", null, sqlar);
                        entryByUri.put(file.uri, entry);
                    }
                }
            } catch (Exception error) {
                android.util.Log.w("MindScape", "Unable to embed file in archive: " + file.title, error);
            }
            ContentValues v = new ContentValues();
            v.put("title", file.title);
            v.put("folder_path", file.folderPath);
            v.put("uri", file.uri);
            v.put("mime_type", file.mimeType);
            v.put("size", file.size);
            v.put("added_at", file.addedAt);
            v.put("archive_entry", entry);
            v.put("original_node_id", originalNodeId);
            db.insert("local_files", null, v);
        }
    }

    private static void importCategories(SQLiteDatabase db, ArchiveData data, ImportStats stats) {
        try (Cursor c = db.query("categories", null, null, null, null, null, null)) {
            while (c.moveToNext()) {
                data.categories.add(new Category(str(c, "title"), str(c, "description"), integer(c, "color"), nullableStr(c, "parent"), integer(c, "is_center") == 1));
                stats.categories++;
            }
        }
    }

    private static void importNotes(SQLiteDatabase db, ArchiveData data, ImportStats stats) {
        try (Cursor c = db.query("notes", null, null, null, null, null, null)) {
            while (c.moveToNext()) {
                Note n = new Note(str(c, "title"), str(c, "category_path"), str(c, "content"), longValue(c, "created_at"), longValue(c, "updated_at"));
                n.favorite = integer(c, "favorite") == 1;
                n.reminderEnabled = integer(c, "reminder_enabled") == 1;
                n.reminderAt = longValue(c, "reminder_at");
                n.reminderTriggered = integer(c, "reminder_triggered") == 1;
                n.quickContainerNote = integer(c, "quick_container_note") == 1;
                data.notes.add(n);
                stats.notes++;
            }
        }
    }

    private static void importConnections(SQLiteDatabase db, ArchiveData data, Map<String, String> nodeIdMap, ImportStats stats) {
        try (Cursor c = db.query("connections", null, null, null, null, null, null)) {
            while (c.moveToNext()) {
                data.connections.add(new Connection(remapNodeId(str(c, "source"), nodeIdMap), remapNodeId(str(c, "target"), nodeIdMap)));
                stats.connections++;
            }
        }
    }

    private static void importHiddenNodes(SQLiteDatabase db, ArchiveData data, Map<String, String> nodeIdMap) {
        try (Cursor c = db.query("hidden_nodes", null, null, null, null, null, null)) {
            while (c.moveToNext()) data.hiddenNodes.add(remapNodeId(str(c, "node_id"), nodeIdMap));
        }
    }

    private static void importChatSessions(SQLiteDatabase db, ArchiveData data) {
        List<ChatSession> sessions = new ArrayList<>();
        String currentId = "";
        try (Cursor c = db.query("ai_sessions", null, null, null, null, null, null)) {
            while (c.moveToNext()) {
                ChatSession s = new ChatSession(str(c, "title"));
                s.id = str(c, "id");
                if (integer(c, "is_current") == 1) currentId = s.id;
                sessions.add(s);
            }
        }
        for (ChatSession s : sessions) {
            try (Cursor c = db.query("ai_messages", null, "session_id = ?", new String[]{s.id}, null, null, "position ASC")) {
                while (c.moveToNext()) s.messages.add(new ChatMessage(integer(c, "is_ai") == 1, str(c, "text")));
            }
            data.chatSessions.add(s);
            if (s.id.equals(currentId)) data.currentChatSession = s;
        }
    }

    private static Map<String, String> importLocalFiles(SQLiteDatabase db, ArchiveData data, MainActivity host, Uri archiveUri, boolean extractFiles, ImportStats stats) throws Exception {
        Map<String, String> nodeIdMap = new HashMap<>();
        Map<String, String> extractedUriByEntry = new HashMap<>();
        try (Cursor c = db.query("local_files", null, null, null, null, null, null)) {
            while (c.moveToNext()) {
                String entry = nullableStr(c, "archive_entry");
                String originalNodeId = nullableStr(c, "original_node_id");
                String uri = str(c, "uri");
                long size = longValue(c, "size");
                String title = str(c, "title");
                String mime = str(c, "mime_type");
                boolean extracted = false;
                if (entry != null && !entry.isEmpty()) {
                    if (extractFiles) {
                        try {
                            uri = extractSqlarEntryToDownloads(host, db, entry, title, mime, extractedUriByEntry);
                            extracted = true;
                        } catch (Exception error) {
                            android.util.Log.w("MindScape", "Unable to extract archived file, keeping sqlar link: " + title, error);
                            stats.failedFiles.add(title + ": " + (error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage()));
                            uri = buildSqlarUri(archiveUri, entry);
                        }
                    } else {
                        uri = buildSqlarUri(archiveUri, entry);
                    }
                } else if (extractFiles && isSqlarUri(uri)) {
                    byte[] fallbackBytes = readMatchingDownloadBytes(host, title, size);
                    if (fallbackBytes != null) {
                        uri = writeBytesToDownloads(host, title, mime, fallbackBytes);
                        extracted = true;
                    }
                }
                LocalFileLink imported = new LocalFileLink(title, nullableStr(c, "folder_path"), uri, mime, size, longValue(c, "added_at"));
                data.localFiles.add(imported);
                if (originalNodeId != null && !originalNodeId.isEmpty()) nodeIdMap.put(originalNodeId, imported.nodeId());
                stats.files++;
                if (extracted) stats.extractedFiles++;
            }
        }
        return nodeIdMap;
    }

    private static String extractSqlarEntryToDownloads(Context context, SQLiteDatabase db, String entry, String title, String mime, Map<String, String> extractedUriByEntry) throws Exception {
        String existingUri = extractedUriByEntry.get(entry);
        if (existingUri != null) return existingUri;
        byte[] bytes = readSqlarBytes(db, entry, MAX_EMBEDDED_FILE_BYTES);
        if (bytes == null) throw new Exception("Archived file is missing: " + entry);
        String result = writeBytesToDownloads(context, title, mime, bytes);
        extractedUriByEntry.put(entry, result);
        return result;
    }

    private static byte[] readSqlarBytes(SQLiteDatabase db, String entryName, long limit) throws Exception {
        long size;
        try (Cursor cursor = db.query("sqlar", new String[]{"sz"}, "name = ?", new String[]{entryName}, null, null, null, "1")) {
            if (!cursor.moveToFirst()) return null;
            size = cursor.getLong(0);
        }
        if (size > limit) throw new Exception("Archived file too large");
        if (size > Integer.MAX_VALUE) throw new Exception("Archived file too large for memory");

        try (SQLiteStatement statement = db.compileStatement("SELECT data FROM sqlar WHERE name = ?")) {
            statement.bindString(1, entryName);
            try (ParcelFileDescriptor descriptor = statement.simpleQueryForBlobFileDescriptor()) {
                if (descriptor == null) return null;
                int initialSize = size > 0 && size <= Integer.MAX_VALUE ? (int) size : 32 * 1024;
                try (InputStream in = new FileInputStream(descriptor.getFileDescriptor());
                     ByteArrayOutputStream out = new ByteArrayOutputStream(initialSize)) {
                    byte[] buffer = new byte[64 * 1024];
                    long total = 0;
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        total += read;
                        if (total > limit || total > Integer.MAX_VALUE) throw new Exception("Archived file too large");
                        out.write(buffer, 0, read);
                    }
                    return out.toByteArray();
                }
            }
        }
    }

    private static String writeBytesToDownloads(Context context, String title, String mime, byte[] bytes) throws Exception {
        String name = safeName(title);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (isApkName(name)) {
                return writeApkBytesToDownloads(context, name, bytes).toString();
            }
            String effectiveMime = downloadMime(name, mime);
            try {
                return writeBytesToPendingDownload(context, name, effectiveMime, bytes).toString();
            } catch (Exception firstError) {
                if ("application/octet-stream".equals(effectiveMime)) throw firstError;
                android.util.Log.w("MindScape", "Retrying extracted file with generic MIME: " + name, firstError);
                return writeBytesToPendingDownload(context, name, "application/octet-stream", bytes).toString();
            }
        }
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MindScape");
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("Unable to create MindScape downloads folder");
        File file = new File(dir, name);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(bytes);
        }
        return Uri.fromFile(file).toString();
    }

    private static Uri writeApkBytesToDownloads(Context context, String name, byte[] bytes) throws Exception {
        try {
            return writeBytesToPublicDownload(context, name, bytes);
        } catch (Exception error) {
            android.util.Log.w("MindScape", "Unable to write APK directly to Downloads, retrying as generic file: " + name, error);
        }
        String tempName = name.substring(0, name.length() - 4) + "-restored.bin";
        return writeBytesToPendingDownload(context, tempName, "application/octet-stream", bytes);
    }

    private static Uri writeBytesToPublicDownload(Context context, String name, byte[] bytes) throws Exception {
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MindScape");
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("Unable to create MindScape downloads folder");
        File file = new File(dir, name);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(bytes);
        }
        if (!file.exists() || file.length() != bytes.length) {
            throw new Exception("Extracted APK was not saved to Downloads");
        }
        MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, null, null);
        return Uri.fromFile(file);
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private static Uri writeBytesToPendingDownload(Context context, String name, String mime, byte[] bytes) throws Exception {
        Uri uri = insertPendingDownload(context, name, mime);
        if (uri == null) throw new Exception("Unable to create extracted file");
        try {
            try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new Exception("Unable to write extracted file");
                out.write(bytes);
            }
            ContentValues published = new ContentValues();
            published.put(MediaStore.Downloads.IS_PENDING, 0);
            int updated = context.getContentResolver().update(uri, published, null, null);
            if (updated <= 0) throw new Exception("Unable to publish extracted file");
            verifyPublished(context, uri);
            return uri;
        } catch (Exception error) {
            context.getContentResolver().delete(uri, null, null);
            throw error;
        }
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private static void verifyPublished(Context context, Uri uri) throws Exception {
        try (Cursor c = context.getContentResolver().query(uri, new String[]{MediaStore.Downloads.IS_PENDING}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int pending = c.getInt(0);
                if (pending != 0) throw new Exception("Extracted file stayed pending");
            }
        }
    }

    private static byte[] readFileBytesForArchive(Context context, LocalFileLink file) throws Exception {
        Exception primaryError = null;
        try {
            byte[] bytes = LocalFileReader.readBytes(context, file, MAX_EMBEDDED_FILE_BYTES);
            if (bytes != null) return bytes;
        } catch (Exception error) {
            primaryError = error;
        }
        byte[] fallback = readMatchingDownloadBytes(context, file.title, file.size);
        if (fallback != null) return fallback;
        if (primaryError != null) throw primaryError;
        return null;
    }

    private static byte[] readMatchingDownloadBytes(Context context, String title, long expectedSize) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null;
        String name = safeName(title);
        if (name.isEmpty()) return null;
        Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        String[] projection = new String[]{MediaStore.MediaColumns._ID, MediaStore.MediaColumns.SIZE};
        String selection = MediaStore.MediaColumns.DISPLAY_NAME + " = ?";
        try (Cursor cursor = context.getContentResolver().query(collection, projection, selection, new String[]{name}, null)) {
            if (cursor == null) return null;
            while (cursor.moveToNext()) {
                long rowSize = cursor.isNull(1) ? -1L : cursor.getLong(1);
                if (expectedSize > 0 && rowSize > 0 && rowSize != expectedSize) continue;
                Uri rowUri = ContentUris.withAppendedId(collection, cursor.getLong(0));
                try (InputStream in = context.getContentResolver().openInputStream(rowUri)) {
                    if (in != null) return LocalFileReader.readStreamToBytes(in, MAX_EMBEDDED_FILE_BYTES);
                }
            }
        } catch (Exception error) {
            android.util.Log.w("MindScape", "Unable to resolve archived file from Downloads: " + name, error);
        }
        return null;
    }

    private static String downloadMime(String name, String mime) {
        return mime == null || mime.isEmpty() ? "application/octet-stream" : mime;
    }

    private static boolean isApkName(String name) {
        return name != null && name.toLowerCase(java.util.Locale.ROOT).endsWith(".apk");
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private static Uri insertPendingDownload(Context context, String name, String mime) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, name);
            values.put(MediaStore.Downloads.MIME_TYPE, mime);
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MindScape");
            values.put(MediaStore.Downloads.IS_PENDING, 1);
            return context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        } catch (RuntimeException error) {
            android.util.Log.w("MindScape", "Unable to create MediaStore download entry for " + name, error);
            return null;
        }
    }

    private static String remapNodeId(String nodeId, Map<String, String> nodeIdMap) {
        String mapped = nodeIdMap.get(nodeId);
        return mapped == null ? nodeId : mapped;
    }

    private static File copyUriToTemp(Context context, Uri uri) throws Exception {
        File file = File.createTempFile("mindscape-import", ".msarchive", context.getCacheDir());
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(file)) {
            if (in == null) throw new Exception("Unable to open archive");
            copy(in, out);
        }
        return file;
    }

    private static void copy(InputStream in, OutputStream out) throws Exception {
        byte[] buffer = new byte[64 * 1024];
        int read;
        while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
    }

    private static String safeName(String raw) {
        String name = raw == null || raw.trim().isEmpty() ? "file" : raw.trim();
        name = name.replaceAll("[\\\\/\\p{Cntrl}]", "_").replace("..", "_").replaceAll("\\s+", " ").trim();
        if (name.isEmpty()) name = "file";
        return name.length() > 120 ? name.substring(0, 120) : name;
    }

    private static String str(Cursor c, String column) {
        int index = c.getColumnIndex(column);
        return index < 0 || c.isNull(index) ? "" : c.getString(index);
    }

    private static String nullableStr(Cursor c, String column) {
        int index = c.getColumnIndex(column);
        return index < 0 || c.isNull(index) ? null : c.getString(index);
    }

    private static int integer(Cursor c, String column) {
        int index = c.getColumnIndex(column);
        return index < 0 || c.isNull(index) ? 0 : c.getInt(index);
    }

    private static long longValue(Cursor c, String column) {
        int index = c.getColumnIndex(column);
        return index < 0 || c.isNull(index) ? 0L : c.getLong(index);
    }
}
