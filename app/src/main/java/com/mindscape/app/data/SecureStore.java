package com.mindscape.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/**
 * Тонкая обёртка над {@link EncryptedSharedPreferences}.
 * Все методы логируют сбои и возвращают fallback-значения — безопасно вызывать из UI без try/catch.
 *
 * Источник: вынесено из MainActivity.java.
 */
public final class SecureStore {

    private static final String FILE_NAME = "mindscape_secure";

    private SecureStore() {}

    /** Создаёт (или открывает) EncryptedSharedPreferences. */
    public static SharedPreferences open(Context context) throws Exception {
        return open(context, false);
    }

    private static SharedPreferences open(Context context, boolean recovered) throws Exception {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
        try {
            return EncryptedSharedPreferences.create(
                    context,
                    FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception error) {
            if (recovered) throw error;
            android.util.Log.w("MindScape", "Encrypted preferences are unreadable; recreating secure store", error);
            context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit().clear().commit();
            java.io.File prefs = new java.io.File(new java.io.File(context.getApplicationInfo().dataDir, "shared_prefs"), FILE_NAME + ".xml");
            if (prefs.exists() && !prefs.delete()) {
                android.util.Log.w("MindScape", "Unable to delete corrupted encrypted preferences file");
            }
            return open(context, true);
        }
    }

    public static String getString(Context context, String key, String fallback) {
        try {
            return open(context).getString(key, fallback);
        } catch (Exception e) {
            android.util.Log.w("MindScape", "Unable to read secure preference: " + key, e);
            return fallback;
        }
    }

    public static void putString(Context context, String key, String value) {
        try {
            open(context).edit().putString(key, value).apply();
        } catch (Exception e) {
            android.util.Log.w("MindScape", "Unable to write secure preference: " + key, e);
        }
    }

    public static void remove(Context context, String... keys) {
        try {
            SharedPreferences.Editor editor = open(context).edit();
            for (String key : keys) editor.remove(key);
            editor.apply();
        } catch (Exception e) {
            android.util.Log.w("MindScape", "Unable to remove secure preferences", e);
        }
    }
}
