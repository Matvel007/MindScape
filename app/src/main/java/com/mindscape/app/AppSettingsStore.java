package com.mindscape.app;

import android.content.Context;
import android.content.SharedPreferences;

import com.mindscape.app.data.SecureStore;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class AppSettingsStore {
        public static final class State {
        public int themeMode = 2;
        public String activeLanguage = "en";
        public float textSizeRatio = 1f;
        public boolean boldTextEnabled;
        public int animationSmoothness = 1;
        public boolean smoothZoom = true;
        public boolean connectionAnimation = true;
        public boolean nodeGlow = true;
        public float nodeSizeValue = 1f;
        public float connectionDensity = 0.5f;
        public boolean notifReminders = true;
        public boolean notifSync;
        public boolean notifAi = true;
        public boolean autosaveEnabled;
        public int autosaveInterval = 15;
        public float aiMaxTokens = 8000f;
        public boolean aiSaveChatEnabled;
        public String aiProviderName;
        public String aiProviderBaseUrl;
        public String aiProviderModel;
        public String aiProviderApiKey = "";
        public String ocrModel = "";
        public String transcriptionModel = "";
        public final java.util.Map<String, String> providerApiKeys = new java.util.HashMap<>();
    }

    private AppSettingsStore() {
    }

    public static State load(Context context, SharedPreferences prefs, String prefThemeMode, String defaultProviderName, String defaultBaseUrl, String defaultModel) throws Exception {
        State state = new State();
        state.themeMode = prefs.getInt(prefThemeMode, 2);
        state.activeLanguage = prefs.getString("activeLanguage", "en");
        state.textSizeRatio = prefs.getFloat("textSizeRatio", 1f);
        state.boldTextEnabled = prefs.getBoolean("boldTextEnabled", false);
        state.animationSmoothness = prefs.getInt("animationSmoothness", 1);
        state.smoothZoom = prefs.getBoolean("smoothZoom", true);
        state.connectionAnimation = prefs.getBoolean("connectionAnimation", true);
        state.nodeGlow = prefs.getBoolean("nodeGlow", true);
        state.nodeSizeValue = prefs.getFloat("nodeSizeValue", 1f);
        state.connectionDensity = prefs.getFloat("connectionDensity", 0.5f);
        state.notifReminders = prefs.getBoolean("notifReminders", true);
        state.notifSync = prefs.getBoolean("notifSync", false);
        state.notifAi = prefs.getBoolean("notifAi", true);
        state.autosaveEnabled = prefs.getBoolean("autosaveEnabled", false);
        state.autosaveInterval = Math.max(1, prefs.getInt("autosaveInterval", 15));
        state.aiMaxTokens = prefs.getFloat("aiMaxTokens", 8000f);
        state.aiSaveChatEnabled = prefs.getBoolean("aiSaveChatEnabled", false);
        state.aiProviderName = prefs.getString("aiProviderName", defaultProviderName);
        state.aiProviderBaseUrl = prefs.getString("aiProviderBaseUrl", defaultBaseUrl);
        state.aiProviderModel = prefs.getString("aiProviderModel", defaultModel);
        state.ocrModel = prefs.getString("ocrModel", "");
        state.transcriptionModel = prefs.getString("transcriptionModel", "");

        SharedPreferences securePrefs = SecureStore.open(context);
        String savedApiKey = securePrefs.getString("aiProviderApiKey", null);
        if (savedApiKey != null && !savedApiKey.isEmpty()) {
            state.aiProviderApiKey = savedApiKey;
        }
        String savedProvKeys = securePrefs.getString("providerApiKeys", null);
        if (savedProvKeys != null) {
            JSONObject keysJson = new JSONObject(savedProvKeys);
            Iterator<String> keys = keysJson.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                state.providerApiKeys.put(key, keysJson.getString(key));
            }
        }
        if (!state.aiProviderApiKey.isEmpty()) {
            state.providerApiKeys.put(state.aiProviderBaseUrl, state.aiProviderApiKey);
        }
        return state;
    }

    public static void save(Context context, SharedPreferences prefs, String prefThemeMode, State state) throws Exception {
        prefs.edit()
                .putInt(prefThemeMode, state.themeMode)
                .putString("activeLanguage", state.activeLanguage)
                .putFloat("textSizeRatio", state.textSizeRatio)
                .putBoolean("boldTextEnabled", state.boldTextEnabled)
                .putInt("animationSmoothness", state.animationSmoothness)
                .putBoolean("smoothZoom", state.smoothZoom)
                .putBoolean("connectionAnimation", state.connectionAnimation)
                .putBoolean("nodeGlow", state.nodeGlow)
                .putFloat("nodeSizeValue", state.nodeSizeValue)
                .putFloat("connectionDensity", state.connectionDensity)
                .putBoolean("notifReminders", state.notifReminders)
                .putBoolean("notifSync", state.notifSync)
                .putBoolean("notifAi", state.notifAi)
                .putBoolean("autosaveEnabled", state.autosaveEnabled)
                .putInt("autosaveInterval", state.autosaveInterval)
                .putFloat("aiMaxTokens", state.aiMaxTokens)
                .putBoolean("aiSaveChatEnabled", state.aiSaveChatEnabled)
                .putString("aiProviderName", state.aiProviderName)
                .putString("aiProviderBaseUrl", state.aiProviderBaseUrl)
                .putString("aiProviderModel", state.aiProviderModel)
                .putString("ocrModel", state.ocrModel)
                .putString("transcriptionModel", state.transcriptionModel)
                .apply();

        SharedPreferences securePrefs = SecureStore.open(context);
        securePrefs.edit()
                .putString("aiProviderApiKey", state.aiProviderApiKey)
                .putString("providerApiKeys", new JSONObject(state.providerApiKeys).toString())
                .apply();
    }
}
