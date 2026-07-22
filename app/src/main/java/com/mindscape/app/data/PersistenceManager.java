package com.mindscape.app.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import com.mindscape.app.Category;
import com.mindscape.app.ChatSession;
import com.mindscape.app.MainActivity;
import com.mindscape.app.AppDataStore;
import com.mindscape.app.AppSettingsStore;

import com.mindscape.app.NodeStateManager;
import java.util.ArrayList;

public class PersistenceManager {

    public static void savePersistentData(MainActivity host) {
        try {
            AppDataStore.save(
                    host.getSharedPreferences("mindscape_settings", Context.MODE_PRIVATE),
                    "graph_data",
                    host.notesList,
                    host.localFilesList,
                    host.categoriesList,
                    host.connectionsList,
                    host.hiddenNodes,
                    host.aiSessions,
                    host.currentAiSession
            );
        } catch (Exception e) {
            android.util.Log.w("MindScape", "Unable to save persistent data", e);
        }
    }

    public static void loadPersistentData(MainActivity host) {
        try {
            AppDataStore.LoadResult result = AppDataStore.load(
                    host.getSharedPreferences("mindscape_settings", Context.MODE_PRIVATE),
                    "graph_data"
            );
            if (result == null) {
                seedInitialData(host);
                savePersistentData(host);
                return;
            }
            host.notesList.clear();
            host.localFilesList.clear();
            host.categoriesList.clear();
            host.connectionsList.clear();
            host.hiddenNodes.clear();
            host.aiSessions.clear();
            host.notesList.addAll(result.notes);
            host.localFilesList.addAll(result.localFiles);
            host.categoriesList.addAll(result.categories);
            host.connectionsList.addAll(result.connections);
            AppDataStore.loadHiddenNodes(host.getSharedPreferences("mindscape_settings", Context.MODE_PRIVATE), "graph_data", host.hiddenNodes);
            migrateLegacyConnectionsAndHiddenNodes(host);
            host.aiSessions.addAll(result.sessions);
            if (result.currentAiSessionId != null && !result.currentAiSessionId.isEmpty()) {
                for (ChatSession session : host.aiSessions) {
                    if (session.id.equals(result.currentAiSessionId)) {
                        host.currentAiSession = session;
                        host.currentChatMessages = new ArrayList<>(session.messages);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.w("MindScape", "Unable to load persistent data", e);
            seedInitialData(host);
            savePersistentData(host);
        }
        if (host.getCenters().isEmpty() && host.getLegacyTopLevel().isEmpty()) {
            seedInitialData(host);
            savePersistentData(host);
        }
    }

    public static void migrateLegacyConnectionsAndHiddenNodes(MainActivity host) {
        NodeStateManager.migrateLegacyConnectionsAndHiddenNodes(host.connectionsList, host.hiddenNodes, host.notesList, host.categoriesList);
    }

    public static void seedInitialData(MainActivity host) {
        host.categoriesList.add(new Category("MindScape", "", Color.rgb(65, 120, 220), null, true));
    }


    public static void loadPersistentSettings(MainActivity host) {
        SharedPreferences prefs = host.getSharedPreferences("mindscape_settings", Context.MODE_PRIVATE);
        try {
            AppSettingsStore.State state = AppSettingsStore.load(
                    host,
                    prefs,
                    "theme_mode",
                    host.aiProviderName,
                    host.aiProviderBaseUrl,
                    host.aiProviderModel
            );
            host.themeMode = state.themeMode;
            host.activeLanguage = state.activeLanguage;
            host.textSizeRatio = state.textSizeRatio;
            host.boldTextEnabled = state.boldTextEnabled;
            host.animationSmoothness = state.animationSmoothness;
            host.smoothZoom = state.smoothZoom;
            host.connectionAnimation = state.connectionAnimation;
            host.nodeGlow = state.nodeGlow;
            host.nodeSizeValue = state.nodeSizeValue;
            host.connectionDensity = state.connectionDensity;
            host.notifReminders = state.notifReminders;
            host.notifSync = state.notifSync;
            host.notifAi = state.notifAi;
            host.autosaveEnabled = state.autosaveEnabled;
            host.autosaveInterval = state.autosaveInterval;
            host.aiMaxTokens = state.aiMaxTokens;
            host.aiSaveChatEnabled = state.aiSaveChatEnabled;
            host.aiProviderName = state.aiProviderName;
            host.aiProviderBaseUrl = state.aiProviderBaseUrl;
            host.aiProviderModel = state.aiProviderModel;
            host.aiProviderApiKey = state.aiProviderApiKey;
            host.ocrModel = state.ocrModel;
            host.transcriptionModel = state.transcriptionModel;
            host.providerApiKeys.clear();
            host.providerApiKeys.putAll(state.providerApiKeys);
        } catch (Exception e) {
            android.util.Log.w("MindScape", "Unable to load persistent settings", e);
        }
    }

    public static void savePersistentSettings(MainActivity host) {
        try {
            AppSettingsStore.State state = new AppSettingsStore.State();
            state.themeMode = host.themeMode;
            state.activeLanguage = host.activeLanguage;
            state.textSizeRatio = host.textSizeRatio;
            state.boldTextEnabled = host.boldTextEnabled;
            state.animationSmoothness = host.animationSmoothness;
            state.smoothZoom = host.smoothZoom;
            state.connectionAnimation = host.connectionAnimation;
            state.nodeGlow = host.nodeGlow;
            state.nodeSizeValue = host.nodeSizeValue;
            state.connectionDensity = host.connectionDensity;
            state.notifReminders = host.notifReminders;
            state.notifSync = host.notifSync;
            state.notifAi = host.notifAi;
            state.autosaveEnabled = host.autosaveEnabled;
            state.autosaveInterval = host.autosaveInterval;
            state.aiMaxTokens = host.aiMaxTokens;
            state.aiSaveChatEnabled = host.aiSaveChatEnabled;
            state.aiProviderName = host.aiProviderName;
            state.aiProviderBaseUrl = host.aiProviderBaseUrl;
            state.aiProviderModel = host.aiProviderModel;
            state.aiProviderApiKey = host.aiProviderApiKey;
            state.ocrModel = host.ocrModel;
            state.transcriptionModel = host.transcriptionModel;
            state.providerApiKeys.putAll(host.providerApiKeys);
            AppSettingsStore.save(
                    host,
                    host.getSharedPreferences("mindscape_settings", Context.MODE_PRIVATE),
                    "theme_mode",
                    state
            );
        } catch (Exception e) {
            android.util.Log.w("MindScape", "Unable to save persistent settings", e);
        }
    }


}
