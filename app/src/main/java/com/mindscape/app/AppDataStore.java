package com.mindscape.app;

import android.content.SharedPreferences;
import android.graphics.Color;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class AppDataStore {
    public static final class LoadResult {
        public final List<Note> notes = new ArrayList<>();
        public final List<LocalFileLink> localFiles = new ArrayList<>();
        public final List<Category> categories = new ArrayList<>();
        public final List<Connection> connections = new ArrayList<>();
        public final List<ChatSession> sessions = new ArrayList<>();
        public String currentAiSessionId;
    }

    private AppDataStore() {
    }

    public static void save(
            SharedPreferences prefs,
            String dataKey,
            List<Note> notes,
            List<LocalFileLink> localFiles,
            List<Category> categories,
            List<Connection> connections,
            Set<String> hiddenNodes,
            List<ChatSession> sessions,
            @Nullable ChatSession currentAiSession
    ) throws Exception {
        JSONObject rootObj = new JSONObject();
        JSONArray notesArr = new JSONArray();
        for (Note n : notes) {
            JSONObject obj = new JSONObject();
            obj.put("title", n.title);
            obj.put("categoryPath", n.categoryPath);
            obj.put("content", n.content);
            obj.put("favorite", n.favorite);
            obj.put("createdAt", n.createdAt);
            obj.put("updatedAt", n.updatedAt);
            obj.put("reminderEnabled", n.reminderEnabled);
            obj.put("reminderAt", n.reminderAt);
            obj.put("reminderTriggered", n.reminderTriggered);
            obj.put("quickContainerNote", n.quickContainerNote);
            notesArr.put(obj);
        }
        rootObj.put("notes", notesArr);

        JSONArray filesArr = new JSONArray();
        for (LocalFileLink file : localFiles) {
            JSONObject obj = new JSONObject();
            obj.put("title", file.title);
            obj.put("folderPath", file.folderPath);
            obj.put("uri", file.uri);
            obj.put("mimeType", file.mimeType);
            obj.put("size", file.size);
            obj.put("addedAt", file.addedAt);
            filesArr.put(obj);
        }
        rootObj.put("localFiles", filesArr);

        JSONArray catsArr = new JSONArray();
        for (Category c : categories) {
            JSONObject obj = new JSONObject();
            obj.put("title", c.title);
            obj.put("description", c.description);
            obj.put("color", c.color);
            if (c.parent != null) obj.put("parent", c.parent);
            if (c.isCenter) obj.put("isCenter", true);
            catsArr.put(obj);
        }
        rootObj.put("categories", catsArr);

        JSONArray connArr = new JSONArray();
        for (Connection c : connections) {
            JSONObject obj = new JSONObject();
            obj.put("source", c.source);
            obj.put("target", c.target);
            connArr.put(obj);
        }
        rootObj.put("connections", connArr);

        JSONArray hiddenArr = new JSONArray();
        for (String hiddenNode : hiddenNodes) {
            hiddenArr.put(hiddenNode);
        }
        rootObj.put("hiddenNodes", hiddenArr);

        JSONArray sessionsArr = new JSONArray();
        for (ChatSession session : sessions) {
            JSONObject sessionObj = new JSONObject();
            sessionObj.put("id", session.id);
            sessionObj.put("title", session.title);
            JSONArray messagesArr = new JSONArray();
            for (ChatMessage message : session.messages) {
                JSONObject messageObj = new JSONObject();
                messageObj.put("isAi", message.isAi);
                messageObj.put("text", message.text);
                messagesArr.put(messageObj);
            }
            sessionObj.put("messages", messagesArr);
            sessionsArr.put(sessionObj);
        }
        rootObj.put("aiSessions", sessionsArr);
        if (currentAiSession != null) {
            rootObj.put("currentAiSessionId", currentAiSession.id);
        }

        prefs.edit().putString(dataKey, rootObj.toString()).apply();
    }

    @Nullable
    public static LoadResult load(SharedPreferences prefs, String dataKey) throws Exception {
        String json = prefs.getString(dataKey, null);
        if (json == null) {
            return null;
        }

        JSONObject obj = new JSONObject(json);
        LoadResult result = new LoadResult();

        if (obj.has("notes")) {
            JSONArray notesArr = obj.getJSONArray("notes");
            for (int i = 0; i < notesArr.length(); i++) {
                JSONObject no = notesArr.getJSONObject(i);
                Note note = new Note(
                        no.getString("title"),
                        no.optString("categoryPath", no.optString("category", "")),
                        no.optString("content", ""),
                        no.optLong("createdAt", System.currentTimeMillis()),
                        no.optLong("updatedAt", System.currentTimeMillis())
                );
                note.favorite = no.optBoolean("favorite", false);
                note.reminderEnabled = no.optBoolean("reminderEnabled", false);
                note.reminderAt = no.optLong("reminderAt", 0L);
                note.reminderTriggered = no.optBoolean("reminderTriggered", false);
                note.quickContainerNote = no.optBoolean("quickContainerNote", false);
                result.notes.add(note);
            }
        }
        if (obj.has("categories")) {
            JSONArray catsArr = obj.getJSONArray("categories");
            for (int i = 0; i < catsArr.length(); i++) {
                JSONObject co = catsArr.getJSONObject(i);
                Category category = new Category(
                        co.getString("title"),
                        co.optString("description", ""),
                        co.optInt("color", Color.rgb(65, 120, 220)),
                        co.optString("parent", null)
                );
                category.isCenter = co.optBoolean("isCenter", false);
                result.categories.add(category);
            }
        }
        if (obj.has("localFiles")) {
            JSONArray filesArr = obj.getJSONArray("localFiles");
            for (int i = 0; i < filesArr.length(); i++) {
                JSONObject fo = filesArr.getJSONObject(i);
                result.localFiles.add(new LocalFileLink(
                        fo.optString("title", "file"),
                        fo.optString("folderPath", null),
                        fo.optString("uri", ""),
                        fo.optString("mimeType", ""),
                        fo.optLong("size", -1L),
                        fo.optLong("addedAt", System.currentTimeMillis())
                ));
            }
        }
        if (obj.has("connections")) {
            JSONArray connArr = obj.getJSONArray("connections");
            for (int i = 0; i < connArr.length(); i++) {
                JSONObject co = connArr.getJSONObject(i);
                result.connections.add(new Connection(
                        co.getString("source"),
                        co.getString("target")
                ));
            }
        }
        if (obj.has("aiSessions")) {
            JSONArray sessionsArr = obj.getJSONArray("aiSessions");
            for (int i = 0; i < sessionsArr.length(); i++) {
                JSONObject sessionObj = sessionsArr.getJSONObject(i);
                ChatSession session = new ChatSession(sessionObj.optString("title", "Session"));
                session.id = sessionObj.optString("id", session.id);
                if (sessionObj.has("messages")) {
                    JSONArray msgsArr = sessionObj.getJSONArray("messages");
                    for (int j = 0; j < msgsArr.length(); j++) {
                        JSONObject messageObj = msgsArr.getJSONObject(j);
                        session.messages.add(new ChatMessage(
                                messageObj.optBoolean("isAi", false),
                                messageObj.optString("text", "")
                        ));
                    }
                }
                result.sessions.add(session);
            }
        }
        result.currentAiSessionId = obj.optString("currentAiSessionId", null);
        return result;
    }

    public static void loadHiddenNodes(SharedPreferences prefs, String dataKey, Set<String> hiddenNodes) throws Exception {
        String json = prefs.getString(dataKey, null);
        if (json == null) {
            return;
        }
        JSONObject obj = new JSONObject(json);
        if (!obj.has("hiddenNodes")) {
            return;
        }
        JSONArray hiddenArr = obj.getJSONArray("hiddenNodes");
        for (int i = 0; i < hiddenArr.length(); i++) {
            hiddenNodes.add(hiddenArr.getString(i));
        }
    }
}
