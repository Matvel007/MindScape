package com.mindscape.app;

import com.mindscape.app.ui.ThemeColors;
import com.mindscape.app.ui.UiKit;
import com.mindscape.app.ui.ThemeState;
import com.mindscape.app.ui.StyledControls;
import com.mindscape.app.ui.StyledToggleState;
import com.mindscape.app.screens.AiScreens;
import com.mindscape.app.screens.AiToolsScreens;
import com.mindscape.app.screens.HomeScreens;
import com.mindscape.app.screens.MapScreens;
import com.mindscape.app.screens.NotesScreens;
import com.mindscape.app.screens.SettingsScreens;
import com.mindscape.app.screens.CategoryDialogs;
import com.mindscape.app.screens.NoteDialogs;
import com.mindscape.app.screens.StructureDialogs;
import com.mindscape.app.screens.StructureScreens;
import com.mindscape.app.files.LocalFileDialogs;
import com.mindscape.app.data.MindScapeArchive;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.graphics.drawable.GradientDrawable;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.TextView;
import android.provider.OpenableColumns;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import org.json.JSONArray;
import org.json.JSONObject;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebChromeClient;
import android.webkit.JavascriptInterface;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends AppCompatActivity implements com.mindscape.app.graph.GraphHost {
    private static final String PREFS_NAME = "mindscape_settings";
    private static final int REQ_POST_NOTIFICATIONS = 2302;
    private static final int REQ_PICK_LOCAL_FILE = 2401;
    private static final int REQ_PICK_AI_TOOL_FILE = 2402;
    private static final int REQ_CREATE_ARCHIVE = 2403;
    private static final int REQ_OPEN_ARCHIVE = 2404;
    private static final String PREF_THEME_MODE = "theme_mode";
    private static final String ROOT_NODE_TITLE = "Личный центр";
    private static final String ROOT_GROUP = "personal-root";
    private static final String UNBOUND_CONTAINER_TITLE = "MindScape";
    private static final String UNBOUND_GROUP = "unbound-notes";

    public TextView statusText;
    private TextView aiResponseText;
    private ProgressBar aiProgress;
    private Button aiButton;
    public FrameLayout contentHost;
    public LinearLayout bottomNav;
    public String selectedSection = "Главная";

    // Application state
    private final List<KnowledgeNode> nodes = new ArrayList<>();
    public final List<Note> notesList = new ArrayList<>();
    public final List<LocalFileLink> localFilesList = new ArrayList<>();

    // AI Chat state
    public final List<ChatSession> aiSessions = new ArrayList<>();
    public ChatSession currentAiSession = null;
    public List<ChatMessage> currentChatMessages = new ArrayList<>();
    private View aiScreenView;
    public boolean aiSaveChatEnabled = false;
    public final List<Category> categoriesList = new ArrayList<>();
    public final List<Connection> connectionsList = new ArrayList<>();
    public final List<View> bottomItems = new ArrayList<>();
    private final Set<Note> selectedNotes = new HashSet<>();
    private final Set<LocalFileLink> selectedLocalFiles = new HashSet<>();
    public final Set<String> expandedFolders = new HashSet<>();
    private boolean structureSelectionMode = false;
    private final Set<String> structureSelectedFolders = new HashSet<>();
    private final Set<Note> structureSelectedNotes = new HashSet<>();
    private final Set<LocalFileLink> structureSelectedFiles = new HashSet<>();
    public final Set<String> hiddenNodes = new HashSet<>(); // node IDs hidden from the map
    private String activeNotesFilter = "all";
    public String activeFileTypeFilter = "all";
    private int notesSortMode = 0; // 0 = newest, 1 = title, 2 = category, 3 = favorites
    private KnowledgeNode selectedNode = null;
    public String currentStructurePath = null;
    private String structureSearchQuery = "";
    @Nullable
    public String pendingLocalFileFolderPath = null;
    @Nullable
    private String pendingAiToolFeature = null;

    // Settings memory
    public String aiProviderName = "OpenAI";
    public String aiProviderBaseUrl = "https://api.openai.com/v1";
    public String aiProviderModel = "";
    public String aiProviderApiKey = "";
    public String ocrModel = "";
    public String transcriptionModel = "";
    public float aiMaxTokens = 2000f;
    private final HashMap<String, List<String>> cachedModels = new HashMap<>();
    public final java.util.HashMap<String, String> providerApiKeys = new HashMap<>(); // baseUrl -> apiKey
    private int editingAiProviderIdx = -1; // temp state while editing AI settings
    public String activeLanguage = "en"; // "ru" or "en"
    private android.content.Context localizedContext = null; // context with correct locale for getString()
    public int themeMode = 2; // 0 = Светлая, 1 = Темная, 2 = Системная
    
    // Rate limiting for AI requests
    private long lastAiRequestTime = 0;
    private static final long AI_REQUEST_COOLDOWN_MS = 2000; // 2 seconds between requests
    private int consecutiveAiErrors = 0;
    private static final int MAX_RETRIES = 3;
    public float textSizeRatio = 1f;
    public boolean boldTextEnabled = false;
    public int animationSmoothness = 1; // 0 = Низкая, 1 = Средняя, 2 = Высокая
    public boolean smoothZoom = true;
    public boolean connectionAnimation = true;
    public boolean nodeGlow = true;
    public float nodeSizeValue = 1f;
    public float connectionDensity = 0.5f;

    // Notifications
    public boolean notifReminders = true;
    public boolean notifSync = false;
    public boolean notifAi = true;
    public boolean autosaveEnabled = false;
    public int autosaveInterval = 15;


    // Active detail model for bottom sheet in map
    public Object selectedMapEntity = null; // can be Category or Note
    private WebView mapWebView = null; // persisted PixiJS WebView
    private WebView miniMapWebView = null; // persisted mini PixiJS WebView
    private boolean appInForeground = false;

    // Test receiver removed - security risk (any app could trigger AI requests)
    private boolean openAiSettingsNext = false;
    private boolean openInterfaceSettingsNext = false;
    private String previousSection = null;
    private final Handler localBackupHandler = new Handler(Looper.getMainLooper());
    private final Runnable localBackupTask = new Runnable() {
        @Override
        public void run() {
            createLocalAutoBackup();
            scheduleLocalAutoBackup();
        }
    };
    @Nullable
    private com.mindscape.app.files.FileSearchIndex fileSearchIndex = null;
    @Nullable

    // Кэш UI-состояния темы; синхронизируется с полями в {@link #themeState()}.
    private final ThemeState themeStateCache = new ThemeState();

    /** Возвращает {@link ThemeState}, синхронизированный с полями Activity. */
    public ThemeState themeState() {
        themeStateCache.themeMode = themeMode;
        themeStateCache.activeLanguage = activeLanguage;
        themeStateCache.textSizeRatio = textSizeRatio;
        themeStateCache.boldTextEnabled = boldTextEnabled;
        themeStateCache.systemDark = (themeMode == 2)
                && (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        return themeStateCache;
    }

    // -------------------------------------------------------------------------
    // Публичные геттеры/сеттеры для приватных полей, используемых
    // из вынесенных экранов (см. {@link com.mindscape.app.screens.SettingsScreens}).
    // Имя метода совпадает с именем поля: themeMode() / themeMode(value) и т.д.
    // -------------------------------------------------------------------------
    public int themeMode() { return themeMode; }
    public void themeMode(int v) {
        if (themeMode != v) {
            themeMode = v;
            // Chat is cached for navigation performance, but its colors are fixed when built.
            aiScreenView = null;
        }
    }
    public String activeLanguage() { return activeLanguage; }
    public void activeLanguage(String v) { activeLanguage = v; }
    public float textSizeRatio() { return textSizeRatio; }
    public void textSizeRatio(float v) { textSizeRatio = v; }
    public boolean boldTextEnabled() { return boldTextEnabled; }
    public void boldTextEnabled(boolean v) { boldTextEnabled = v; }
    public int animationSmoothness() { return animationSmoothness; }
    public void animationSmoothness(int v) { animationSmoothness = v; }
    public boolean smoothZoom() { return smoothZoom; }
    public void smoothZoom(boolean v) { smoothZoom = v; }
    public boolean connectionAnimation() { return connectionAnimation; }
    public void connectionAnimation(boolean v) { connectionAnimation = v; }
    public boolean nodeGlow() { return nodeGlow; }
    public void nodeGlow(boolean v) { nodeGlow = v; }
    public float nodeSizeValue() { return nodeSizeValue; }
    public void nodeSizeValue(float v) { nodeSizeValue = v; }
    public float connectionDensity() { return connectionDensity; }
    public void connectionDensity(float v) { connectionDensity = v; }
    public boolean notifReminders() { return notifReminders; }
    public void notifReminders(boolean v) { notifReminders = v; }
    public boolean notifSync() { return notifSync; }
    public void notifSync(boolean v) { notifSync = v; }
    public boolean notifAi() { return notifAi; }
    public void notifAi(boolean v) { notifAi = v; }
    public boolean autosaveEnabled() { return autosaveEnabled; }
    public void autosaveEnabled(boolean v) { autosaveEnabled = v; }
    public int autosaveInterval() { return autosaveInterval; }
    public void autosaveInterval(int v) { autosaveInterval = v; }
    public String aiProviderName() { return aiProviderName; }
    public void aiProviderName(String v) { aiProviderName = v; }
    public String aiProviderBaseUrl() { return aiProviderBaseUrl; }
    public void aiProviderBaseUrl(String v) { aiProviderBaseUrl = v; }
    public String aiProviderApiKey() { return aiProviderApiKey; }
    public void aiProviderApiKey(String v) { aiProviderApiKey = v; }
    public String aiProviderModel() { return aiProviderModel; }
    public void aiProviderModel(String v) { aiProviderModel = v; }
    public String ocrModel() { return ocrModel; }
    public void ocrModel(String v) { ocrModel = v; }
    public String transcriptionModel() { return transcriptionModel; }
    public void transcriptionModel(String v) { transcriptionModel = v; }
    public float aiMaxTokens() { return aiMaxTokens; }
    public void aiMaxTokens(float v) { aiMaxTokens = v; }
    public boolean aiSaveChatEnabled() { return aiSaveChatEnabled; }
    public void aiSaveChatEnabled(boolean v) { aiSaveChatEnabled = v; }
    public int editingAiProviderIdx() { return editingAiProviderIdx; }
    public void editingAiProviderIdx(int v) { editingAiProviderIdx = v; }
    public HashMap<String, List<String>> cachedModels() { return cachedModels; }
    public HashMap<String, String> providerApiKeys() { return providerApiKeys; }
    public List<Note> notesList() { return notesList; }
    public List<LocalFileLink> localFilesList() { return localFilesList; }
    public List<Category> categoriesList() { return categoriesList; }
    public List<Connection> connectionsList() { return connectionsList; }
    public Set<String> hiddenNodes() { return hiddenNodes; }
    public Set<Note> selectedNotes() { return selectedNotes; }
    public Set<LocalFileLink> selectedLocalFiles() { return selectedLocalFiles; }
    public String activeNotesFilter() { return activeNotesFilter; }
    public void activeNotesFilter(String v) { activeNotesFilter = v; }
    public String activeFileTypeFilter() { return activeFileTypeFilter; }
    public void activeFileTypeFilter(String v) { activeFileTypeFilter = v; }
    public int notesSortMode() { return notesSortMode; }
    public void notesSortMode(int v) { notesSortMode = v; }
    public Object selectedMapEntity() { return selectedMapEntity; }
    public void selectedMapEntity(Object v) { selectedMapEntity = v; }
    public WebView miniMapWebView() { return miniMapWebView; }
    public void miniMapWebView(WebView v) { miniMapWebView = v; }
    public WebView mapWebView() { return mapWebView; }
    public void mapWebView(WebView v) { mapWebView = v; }
    public String currentStructurePath() { return currentStructurePath; }
    public void currentStructurePath(String v) { currentStructurePath = v; }
    public String structureSearchQuery() { return structureSearchQuery; }
    public void structureSearchQuery(String v) { structureSearchQuery = v; }
    public boolean structureSelectionMode() { return structureSelectionMode; }
    public void structureSelectionMode(boolean v) { structureSelectionMode = v; }
    public Set<Note> structureSelectedNotes() { return structureSelectedNotes; }
    public Set<String> structureSelectedFolders() { return structureSelectedFolders; }
    public Set<LocalFileLink> structureSelectedFiles() { return structureSelectedFiles; }
    public Set<String> expandedFolders() { return expandedFolders; }
    public List<ChatSession> aiSessions() { return aiSessions; }
    public ChatSession currentAiSession() { return currentAiSession; }
    public void currentAiSession(ChatSession v) { currentAiSession = v; }
    public List<ChatMessage> currentChatMessages() { return currentChatMessages; }
    public void currentChatMessages(List<ChatMessage> v) { currentChatMessages = v; }
    public View aiScreenView() { return aiScreenView; }
    public void aiScreenView(View v) { aiScreenView = v; }
    public String pendingAiToolFeature() { return pendingAiToolFeature; }
    public void pendingAiToolFeature(String v) { pendingAiToolFeature = v; }

    public com.mindscape.app.files.FileSearchIndex fileSearchIndex() {
        if (fileSearchIndex == null) fileSearchIndex = new com.mindscape.app.files.FileSearchIndex(this);
        return fileSearchIndex;
    }
    public boolean openAiSettingsNext() { return openAiSettingsNext; }
    public void openAiSettingsNext(boolean v) { openAiSettingsNext = v; }
    public boolean openInterfaceSettingsNext() { return openInterfaceSettingsNext; }
    public void openInterfaceSettingsNext(boolean v) { openInterfaceSettingsNext = v; }
    public String previousSection() { return previousSection; }
    public void previousSection(String v) { previousSection = v; }
    public String selectedSection() { return selectedSection; }
    public void selectedSection(String v) { selectedSection = v; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        PDFBoxResourceLoader.init(getApplicationContext());
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPressed();
            }
        });
        
        // BroadcastReceiver removed for security

        // No default API keys from BuildConfig for security.
        // Users provide their own AI provider credentials.

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        activeLanguage = prefs.getString("activeLanguage", "en");
        // Reset any previously set per-app locale to avoid keyboard layout changes
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.getEmptyLocaleList());
        // Build a localized context so getString() uses activeLanguage, not the system locale
        updateLocalizedContext();
        // No default API key from BuildConfig for security.
        loadPersistentSettings();

        applySystemBars();

        startApp();
    }

    public void startApp() {
        loadPersistentData();
        requestNotificationPermissionIfNeeded();
        rescheduleActiveReminders();
        configureLocalAutoBackup();
        setContentView(buildUi());
        handleReminderIntent(getIntent());
    }

    public void handleBackPressed() {
        finish();
    }

    public SharedPreferences securePrefs() throws Exception {
        return com.mindscape.app.data.SecureStore.open(this);
    }

    public String secureString(String key, String fallback) {
        return com.mindscape.app.data.SecureStore.getString(this, key, fallback);
    }

    public void putSecureString(String key, String value) {
        com.mindscape.app.data.SecureStore.putString(this, key, value);
    }

    public void removeSecureStrings(String... keys) {
        com.mindscape.app.data.SecureStore.remove(this, keys);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleReminderIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_LOCAL_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            addLocalFileLink(data);
        } else if (requestCode == REQ_PICK_AI_TOOL_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            handleAiToolFilePicked(data.getData());
        } else if (requestCode == REQ_CREATE_ARCHIVE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            exportArchive(data.getData());
        } else if (requestCode == REQ_OPEN_ARCHIVE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            confirmImportArchive(data.getData());
        }
    }

    public void createArchiveBackup() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(MindScapeArchive.MIME);
        intent.putExtra(Intent.EXTRA_TITLE, "MindScape-Backup-" + System.currentTimeMillis() + ".msarchive");
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(intent, REQ_CREATE_ARCHIVE);
    }

    public void openArchiveBackup() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQ_OPEN_ARCHIVE);
    }

    private void exportArchive(Uri outputUri) {
        Toast.makeText(this, "ru".equals(activeLanguage) ? "Создаём backup..." : "Creating backup...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                MindScapeArchive.exportToUri(this, outputUri);
                runOnUiThread(() -> Toast.makeText(this, "ru".equals(activeLanguage) ? "Backup создан" : "Backup created", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, userFriendlyError(e), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    public void configureLocalAutoBackup() {
        localBackupHandler.removeCallbacks(localBackupTask);
        if (appInForeground && autosaveEnabled) scheduleLocalAutoBackup();
    }

    private void scheduleLocalAutoBackup() {
        if (!appInForeground || !autosaveEnabled) return;
        localBackupHandler.postDelayed(localBackupTask, Math.max(1, autosaveInterval) * 60_000L);
    }

    private void createLocalAutoBackup() {
        new Thread(() -> {
            try {
                Uri output = localAutoBackupUri();
                MindScapeArchive.exportToUri(this, output);
                android.util.Log.i("MindScape", "Local automatic SQLAR backup completed");
            } catch (Exception error) {
                android.util.Log.w("MindScape", "Unable to create local automatic SQLAR backup", error);
            }
        }, "MindScapeLocalAutoBackup").start();
    }

    private Uri localAutoBackupUri() throws Exception {
        final String name = "MindScape-AutoBackup.msarchive";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            android.content.ContentResolver resolver = getContentResolver();
            String relativePath = android.os.Environment.DIRECTORY_DOWNLOADS + "/MindScape";
            String selection = android.provider.MediaStore.Downloads.DISPLAY_NAME + " = ? AND "
                    + android.provider.MediaStore.Downloads.RELATIVE_PATH + " = ?";
            try (Cursor cursor = resolver.query(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    new String[]{android.provider.MediaStore.Downloads._ID}, selection,
                    new String[]{name, relativePath}, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    return android.content.ContentUris.withAppendedId(
                            android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cursor.getLong(0));
                }
            }
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, name);
            values.put(android.provider.MediaStore.Downloads.MIME_TYPE, MindScapeArchive.MIME);
            values.put(android.provider.MediaStore.Downloads.RELATIVE_PATH, relativePath);
            Uri uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new Exception("Unable to create automatic backup file");
            return uri;
        }
        java.io.File directory = new java.io.File(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "MindScape");
        if (!directory.exists() && !directory.mkdirs()) throw new Exception("Unable to create backup directory");
        return Uri.fromFile(new java.io.File(directory, name));
    }

    private void confirmImportArchive(Uri inputUri) {
        boolean ru = "ru".equals(activeLanguage);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(18), dp(16), dp(18), dp(8));
        layout.addView(text(ru ? "Восстановить бекап" : "Restore backup", 20, primaryText(), true));
        layout.addView(spacer(8));
        layout.addView(text(ru
                ? "Текущие заметки, связи и файлы будут заменены данными из архива. Файлы можно извлечь в Downloads/MindScape или читать напрямую из бекапа. Чтение без извлечения не позволяет просматривать фотографии и другие медиафайлы во внешних приложениях."
                : "Current notes, links and files will be replaced with archive data. Files can be extracted to Downloads/MindScape or read directly from backup. Reading without extraction does not allow viewing photos and other media files in external apps.",
                14, secondaryText(), false));
        layout.addView(spacer(14));

        final AlertDialog[] dialogRef = new AlertDialog[1];
        Button extract = createStyledButton(ru ? "Извлечь файлы" : "Extract files");
        extract.setOnClickListener(v -> {
            if (dialogRef[0] != null) dialogRef[0].dismiss();
            importArchive(inputUri, true);
        });
        layout.addView(extract, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));
        layout.addView(spacer(8));

        Button direct = createStyledButton(ru ? "Читать из бекапа" : "Read from backup");
        direct.setOnClickListener(v -> {
            if (dialogRef[0] != null) dialogRef[0].dismiss();
            importArchive(inputUri, false);
        });
        layout.addView(direct, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));
        layout.addView(spacer(8));

        Button cancel = createStyledButton(ru ? "Отмена" : "Cancel");
        cancel.setOnClickListener(v -> {
            if (dialogRef[0] != null) dialogRef[0].dismiss();
        });
        layout.addView(cancel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(scrollableDialogContent(layout, 420))
                .create();
        dialogRef[0] = dialog;
        dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(roundedBg(surface(), 16, 0, Color.TRANSPARENT));
    }

    private void importArchive(Uri inputUri, boolean extractFiles) {
        Toast.makeText(this, "ru".equals(activeLanguage) ? "Восстанавливаем backup..." : "Restoring backup...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                MindScapeArchive.ImportStats stats = MindScapeArchive.importFromUri(this, inputUri, extractFiles);
                runOnUiThread(() -> {
                    savePersistentData();
                    fileSearchIndex().clearAll();
                    reindexAllReadableFilesAsync();
                    reindexNotesAsync();
                    pushGraphDataToWebView();
                    renderContent();
                    String message = ("ru".equals(activeLanguage) ? "Backup восстановлен: " : "Backup restored: ")
                            + stats.notes + " notes, " + stats.categories + " folders, " + stats.files + " files";
                    if (!stats.failedFiles.isEmpty()) {
                        message += ("ru".equals(activeLanguage) ? "\nНе извлеклись: " : "\nNot extracted: ")
                                + stats.failedFiles.get(0);
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, userFriendlyError(e), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void reindexAllReadableFilesAsync() {
        List<LocalFileLink> snapshot = new ArrayList<>(localFilesList);
        new Thread(() -> {
            for (LocalFileLink file : snapshot) {
                if (file == null || !isAiReadableFile(file)) continue;
                try {
                    fileSearchIndex().upsert(file, readLocalFileText(file, 120_000));
                } catch (Exception error) {
                    android.util.Log.w("MindScape", "Unable to rebuild file content index", error);
                }
            }
        }, "MindScapeFileReindexAll").start();
    }

    public void pickLocalFile(@Nullable String folderPath) {
        pendingLocalFileFolderPath = folderPath;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQ_PICK_LOCAL_FILE);
    }

    public void pickAiToolFile(String feature) {
        pendingAiToolFeature = feature;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("ocr".equals(feature) ? "image/*" : "audio/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, REQ_PICK_AI_TOOL_FILE);
    }

    public void handleAiToolFilePicked(Uri uri) {
        String feature = pendingAiToolFeature == null ? "transcription" : pendingAiToolFeature;
        pendingAiToolFeature = null;
        Toast.makeText(this, "ru".equals(activeLanguage) ? "Обрабатываем файл..." : "Processing file...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                String title = queryDisplayName(uri);
                String mime = getContentResolver().getType(uri);
                byte[] bytes;
                try (InputStream stream = getContentResolver().openInputStream(uri)) {
                    bytes = readStreamToBytes(stream, 25L * 1024L * 1024L);
                }
                String model = "ocr".equals(feature) ? ocrModel : transcriptionModel;
                if (aiProviderBaseUrl.trim().isEmpty() || aiProviderApiKey.trim().isEmpty() || model == null || model.trim().isEmpty()) {
                    throw new IllegalStateException("Configure an AI provider, API key, and model for this tool in AI settings.");
                }
                String name = title == null || title.trim().isEmpty() ? ("file_" + System.currentTimeMillis()) : title;
                String type = mime == null ? "application/octet-stream" : mime;
                String text = "ocr".equals(feature)
                        ? AiClient.extractImageText(aiProviderBaseUrl, aiProviderApiKey, model, type, bytes)
                        : AiClient.transcribeAudio(aiProviderBaseUrl, aiProviderApiKey, model, name, type, bytes);
                runOnUiThread(() -> saveAiToolResult(feature, title, text));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, com.mindscape.app.BuildConfig.DEBUG ? userFriendlyError(e) : ("ru".equals(activeLanguage) ? "Не удалось обработать файл. Попробуйте позже." : "Could not process the file. Try again later."), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    public void saveAiToolResult(String feature, @Nullable String sourceTitle, String text) {
        if (text == null || text.trim().isEmpty()) {
            Toast.makeText(this, "ru".equals(activeLanguage) ? "Не удалось распознать текст." : "Could not recognize text.", Toast.LENGTH_LONG).show();
            return;
        }
        String folderTitle = "ocr".equals(feature) ? "OCR" : "Транскрибация";
        String folderPath = ensureMindScapeToolFolder(folderTitle);
        String prefix = "ocr".equals(feature) ? "OCR" : "Transcription";
        String cleanSource = sourceTitle == null || sourceTitle.trim().isEmpty() ? "file" : sourceTitle.replaceAll("[^A-Za-zА-Яа-я0-9._ -]+", "_");
        String fileName = prefix + "_" + System.currentTimeMillis() + "_" + cleanSource + ".txt";
        Uri saved = saveTextToDownloadsMindscape(fileName, text);
        if (saved == null) {
            Toast.makeText(this, "ru".equals(activeLanguage) ? "Не удалось сохранить результат." : "Could not save the result.", Toast.LENGTH_LONG).show();
            return;
        }
        LocalFileLink link = new LocalFileLink(fileName, folderPath, saved.toString(), "text/plain", text.getBytes(StandardCharsets.UTF_8).length, System.currentTimeMillis());
        localFilesList.add(link);
        connectionsList.add(new Connection("folder:" + folderPath, link.nodeId()));
        savePersistentData();
        reindexFileAsync(link);
        pushGraphDataToWebView();
        renderContent();
        Toast.makeText(this, "ru".equals(activeLanguage) ? "Результат сохранён и добавлен в MindScape." : "The result was saved and added to MindScape.", Toast.LENGTH_LONG).show();
    }

    private String ensureMindScapeToolFolder(String folderTitle) {
        ensureTopLevelCenter("MindScape");
        String path = "MindScape/" + folderTitle;
        for (Category category : categoriesList) {
            if (folderTitle.equalsIgnoreCase(category.title) && "MindScape".equalsIgnoreCase(category.parent)) return path;
        }
        categoriesList.add(new Category(folderTitle, "AI-инструменты MindScape", Color.rgb(65, 120, 220), "MindScape", false));
        return path;
    }

    private void ensureTopLevelCenter(String title) {
        for (Category category : categoriesList) {
            if (title.equalsIgnoreCase(category.title) && (category.parent == null || category.parent.isEmpty())) return;
        }
        categoriesList.add(new Category(title, "AI-инструменты MindScape", Color.rgb(65, 120, 220), null, true));
    }

    public void addLocalFileLink(Intent data) {
        com.mindscape.app.files.LocalFileOps.addLocalFileLink(this, data);
    }

    public String readStreamToString(InputStream stream, int maxChars) throws Exception {
        if (stream == null) return "";
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            char[] buf = new char[4096];
            int read;
            while ((read = reader.read(buf)) != -1 && builder.length() < maxChars) {
                int take = Math.min(read, maxChars - builder.length());
                builder.append(buf, 0, take);
            }
        }
        return builder.toString();
    }

    @Nullable
    public String queryDisplayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return cursor.getString(idx);
            }
        } catch (Exception error) {
            android.util.Log.w("MindScape", "Unable to query display name for URI", error);
        }
        return null;
    }

    public long queryFileSize(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (idx >= 0) return cursor.getLong(idx);
            }
        } catch (Exception error) {
            android.util.Log.w("MindScape", "Unable to query file size for URI", error);
        }
        return -1L;
    }

    @Override
    protected void onResume() {
        super.onResume();
        appInForeground = true;
        configureLocalAutoBackup();
    }

    @Override
    protected void onPause() {
        super.onPause();
        appInForeground = false;
        localBackupHandler.removeCallbacks(localBackupTask);
        savePersistentData();
        savePersistentSettings();
    }

    @Override
    protected void onDestroy() {
        if (mapWebView != null) {
            mapWebView.destroy();
            mapWebView = null;
        }
        if (miniMapWebView != null) {
            miniMapWebView.destroy();
            miniMapWebView = null;
        }
        super.onDestroy();
    }


    public void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, REQ_POST_NOTIFICATIONS);
        }
    }

    public void handleReminderIntent(@Nullable Intent intent) {
        if (intent == null) return;
        String notePath = intent.getStringExtra(ReminderReceiver.EXTRA_NOTE_PATH);
        if (notePath == null || notePath.isEmpty()) return;

        Note note = findNoteByFullPath(notePath);
        cancelReminderNotification(notePath);
        if (note == null) return;

        note.reminderEnabled = false;
        note.reminderTriggered = false;
        savePersistentData();
        selectedMapEntity = note;
        selectedSection = "Карта";
        renderContent();
        updateBottomNav();
    }

    @Nullable
    public Note findNoteByFullPath(String fullPath) {
        for (Note note : notesList) {
            if (note.fullPath().equals(fullPath)) return note;
        }
        return null;
    }

    public void scheduleNoteReminder(Note note) {
        if (!notifReminders || !note.reminderEnabled || note.reminderAt <= System.currentTimeMillis()) {
            com.mindscape.app.reminders.ReminderScheduler.cancel(this, note.fullPath());
            return;
        }
        com.mindscape.app.reminders.ReminderScheduler.schedule(
                this, note.fullPath(), note.title, note.content, note.reminderAt);
    }

    public void cancelNoteReminder(String notePath) {
        com.mindscape.app.reminders.ReminderScheduler.cancel(this, notePath);
    }

    public void cancelReminderNotification(String notePath) {
        com.mindscape.app.reminders.ReminderScheduler.cancelNotification(this, notePath);
    }

    public void rescheduleActiveReminders() {
        long now = System.currentTimeMillis();
        for (Note note : notesList) {
            if (note.reminderEnabled && note.reminderAt > now) {
                com.mindscape.app.reminders.ReminderScheduler.schedule(
                        this, note.fullPath(), note.title, note.content, note.reminderAt);
            }
        }
    }

    public void maybeNotifyAiResponse(String response) {
        if (!notifAi || appInForeground || TextUtils.isEmpty(response)) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    "mindscape_ai",
                    "MindScape AI",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            manager.createNotificationChannel(channel);
        }
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                9301,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        String preview = AiCommandParser.stripCommands(response);
        if (preview.length() > 160) preview = preview.substring(0, 160) + "...";
        android.app.Notification notification = new androidx.core.app.NotificationCompat.Builder(this, "mindscape_ai")
                .setSmallIcon(R.drawable.ic_ai)
                .setContentTitle(str(R.string.str_ai_assistant))
                .setContentText(preview)
                .setStyle(new androidx.core.app.NotificationCompat.BigTextStyle().bigText(preview))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();
        manager.notify(9301, notification);
    }

    public void savePersistentData() {
        com.mindscape.app.data.PersistenceManager.savePersistentData(this);
        reindexNotesAsync();
    }

    public void loadPersistentData() {
        com.mindscape.app.data.PersistenceManager.loadPersistentData(this);
    }

    public View buildUi() {
        return com.mindscape.app.screens.CoreScreens.buildUi(this);
    }

    public View buildAppUi() {
        return com.mindscape.app.screens.CoreScreens.buildUi(this);
    }

    public boolean hasScrolledToBottom(ScrollView scroll) {
        if (scroll.getChildCount() == 0) return true;
        View child = scroll.getChildAt(0);
        return child.getBottom() <= scroll.getScrollY() + scroll.getHeight() + dp(8);
    }


    public void refreshBottomTabs() {
        bottomNav.removeAllViews();
        bottomItems.clear();

        boolean ru = "ru".equals(activeLanguage);
        addBottomItem("Главная", str(R.string.str_main), "ic_nav_home");
        addBottomItem("Карта", str(R.string.str_map), "ic_nav_map");
        addBottomItem("Структура", str(R.string.str_structure), "ic_nav_create");
        addBottomItem("Заметки", str(R.string.str_notes), "ic_nav_notes");
        addBottomItem("Настройки", str(R.string.str_settings), "ic_nav_settings");
        
        bottomNav.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);
    }


    public TextView actionText(String label) {
        TextView action = text(label, 24, secondaryText(), false);
        action.setGravity(Gravity.CENTER);
        action.setPadding(dp(10), dp(6), dp(10), dp(6));
        return action;
    }

    public android.widget.ImageView iconAction(String drawableName, int tint) {
        android.widget.ImageView action = new android.widget.ImageView(this);
        int resId = getResources().getIdentifier(drawableName, "drawable", getPackageName());
        if (resId != 0) {
            action.setImageResource(resId);
        }
        action.setColorFilter(tint);
        action.setContentDescription(drawableName.replace("ic_", "").replace("_", " "));
        action.setMinimumWidth(dp(44));
        action.setMinimumHeight(dp(44));
        action.setPadding(dp(8), dp(8), dp(8), dp(8));
        action.setBackground(roundedBg(Color.TRANSPARENT, 12, 0, Color.TRANSPARENT));
        return action;
    }

    @Override
    public boolean isDarkTheme() {
        if (themeMode == 1) {
            return true;
        }
        if (themeMode == 2) {
            int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            return nightMode == Configuration.UI_MODE_NIGHT_YES;
        }
        return false;
    }

    public void loadPersistentSettings() {
        com.mindscape.app.data.PersistenceManager.loadPersistentSettings(this);
    }

    public void savePersistentSettings() {
        com.mindscape.app.data.PersistenceManager.savePersistentSettings(this);
    }

    public void saveThemeMode() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putInt(PREF_THEME_MODE, themeMode)
                .apply();
    }

    public int appBg() {
        return ThemeColors.appBg(isDarkTheme());
    }

    public int surface() {
        return ThemeColors.surface(isDarkTheme());
    }

    public int softSurface() {
        return ThemeColors.softSurface(isDarkTheme());
    }

    public int primaryText() {
        return ThemeColors.primaryText(isDarkTheme());
    }

    public int secondaryText() {
        return ThemeColors.secondaryText(isDarkTheme());
    }

    public int strokeColor() {
        return ThemeColors.strokeColor(isDarkTheme());
    }

    public int accentColor() {
        return ThemeColors.accentColor(isDarkTheme());
    }

    public void applySystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(appBg());
        window.setNavigationBarColor(appBg());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(!isDarkTheme());
        controller.setAppearanceLightNavigationBars(!isDarkTheme());
    }

    public int themedBgColor(int color) {
        return ThemeColors.themedBgColor(color, isDarkTheme());
    }

    public int themedTextColor(int color) {
        return ThemeColors.themedTextColor(color, isDarkTheme());
    }

    public int themedStrokeColor(int color) {
        return ThemeColors.themedStrokeColor(color, isDarkTheme());
    }

    public GradientDrawable roundedBg(int color, int radiusDp, int strokeDp, int strokeColor) {
        return UiKit.roundedBg(this, color, radiusDp, strokeDp, strokeColor, isDarkTheme());
    }

    public LinearLayout card(int color) {
        return UiKit.card(this, color, isDarkTheme());
    }

    public TextView text(String value, int sp, int color, boolean bold) {
        return UiKit.text(this, value, sp, color, bold, themeState());
    }

    public void applyTextWeight(TextView view, boolean bold) {
        UiKit.applyTextWeight(view, bold, themeState());
    }

    public TextView chip(String value) {
        return UiKit.chip(this, value, themeState());
    }

    public TextView section(String title, String body) {
        return UiKit.section(this, title, body, themeState());
    }

    public LinearLayout row() {
        return UiKit.row(this);
    }

    public LinearLayout dashboardTile(String title, String detail, int color, String target, String drawableName) {
        return com.mindscape.app.screens.CoreScreens.dashboardTile(this, title, detail, color, target, drawableName);
    }


    public void addBottomItem(String section, String label, String drawableName) {
        com.mindscape.app.screens.CoreScreens.addBottomItem(this, section, label, drawableName);
    }

    public void updateBottomNav() {
        com.mindscape.app.screens.CoreScreens.updateBottomNav(this);
    }

    public void cleanDanglingConnections() {
        java.util.Set<String> validNodes = new java.util.HashSet<>();
        for (Note n : notesList) {
            validNodes.add(n.fullPath().toLowerCase(Locale.ROOT));
            validNodes.add(n.title.toLowerCase(Locale.ROOT)); // Backwards compatibility
        }
        for (String path : getAllCategoryPaths()) {
            validNodes.add(path.toLowerCase(Locale.ROOT));
        }
        for (LocalFileLink file : localFilesList) {
            validNodes.add(cleanNodeId(file.nodeId()).toLowerCase(Locale.ROOT));
            validNodes.add(file.title.toLowerCase(Locale.ROOT)); // Backwards compatibility
            validNodes.add(file.uri.toLowerCase(Locale.ROOT));
        }
        connectionsList.removeIf(conn -> {
            String cleanSrc = cleanNodeId(conn.source).toLowerCase(Locale.ROOT);
            String cleanTgt = cleanNodeId(conn.target).toLowerCase(Locale.ROOT);
            String normalizedSrc = normalizeConnectionNodeId(conn.source);
            String normalizedTgt = normalizeConnectionNodeId(conn.target);
            if (normalizedSrc.startsWith("file:") && findLocalFileByNodeId(normalizedSrc) == null) return true;
            if (normalizedTgt.startsWith("file:") && findLocalFileByNodeId(normalizedTgt) == null) return true;
            if (isFolderItemConnection(normalizedSrc, normalizedTgt)) return true;
            if (!validNodes.contains(cleanSrc) || !validNodes.contains(cleanTgt)) return true;
            LocalFileLink sourceFile = findLocalFileByNodeId(conn.source);
            LocalFileLink targetFile = findLocalFileByNodeId(conn.target);
            return sourceFile != null
                    && targetFile != null
                    && sourceFile != targetFile
                    && sourceFile.uri != null
                    && sourceFile.uri.equals(targetFile.uri);
        });
    }

    private boolean isFolderItemConnection(String source, String target) {
        boolean sourceFolder = source != null && source.startsWith("folder:");
        boolean targetFolder = target != null && target.startsWith("folder:");
        boolean sourceItem = source != null && (source.startsWith("file:") || source.startsWith("note:"));
        boolean targetItem = target != null && (target.startsWith("file:") || target.startsWith("note:"));
        return (sourceFolder && targetItem) || (targetFolder && sourceItem);
    }

    @Nullable
    private LocalFileLink findLocalFileByNodeId(String nodeId) {
        if (nodeId == null) return null;
        String normalized = normalizeConnectionNodeId(nodeId);
        for (LocalFileLink file : localFilesList) {
            if (file.nodeId().equalsIgnoreCase(normalized)) return file;
        }
        return null;
    }

    public void renderContent() {
        if (contentHost == null) return;
        
        cleanDanglingConnections();

        // Auto-save data on every content refresh (covers all mutation paths)
        savePersistentData();
        savePersistentSettings();
        
        // Remove everything EXCEPT mapWebView
        for (int i = contentHost.getChildCount() - 1; i >= 0; i--) {
            View child = contentHost.getChildAt(i);
            if (child != mapWebView) {
                contentHost.removeViewAt(i);
            }
        }
        
        if (bottomNav != null) {
            bottomNav.setVisibility("AI".equals(selectedSection) ? View.GONE : View.VISIBLE);
        }
        
        boolean showMap = "Карта".equals(selectedSection);
        if (mapWebView != null) {
            mapWebView.setVisibility(showMap ? View.VISIBLE : View.GONE);
        }
        
        FrameLayout.LayoutParams matchParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        if (showMap) {
            contentHost.addView(mapScreen(), matchParams);
        } else if ("AI".equals(selectedSection)) {
            if (aiScreenView == null) aiScreenView = aiScreen();
            if (aiScreenView.getParent() != null) ((ViewGroup) aiScreenView.getParent()).removeView(aiScreenView);
            contentHost.addView(aiScreenView, matchParams);
        } else if ("AI_TOOLS".equals(selectedSection)) {
            contentHost.addView(AiToolsScreens.aiToolsScreen(this), matchParams);
        } else if ("Заметки".equals(selectedSection)) {
            contentHost.addView(notesTabScreen(), matchParams);
        } else if ("Создать".equals(selectedSection) || "Структура".equals(selectedSection)) {
            contentHost.addView(createTabScreen(), matchParams);
        } else if ("Настройки".equals(selectedSection)) {
            contentHost.addView(SettingsScreens.settingsTabScreen(this), matchParams);
        } else {
            contentHost.addView(homeScreen(), matchParams);
        }
    }

    // --- HOME SCREEN ---
    public View homeScreen() {
        return HomeScreens.homeScreen(this);
    }

    public void addHomeHeader(LinearLayout body) {
        LinearLayout topBar = new LinearLayout(this);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(4), dp(6), dp(4), dp(10));
        topBar.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.addView(text("MindScape", 20, primaryText(), true));
        titleBox.addView(titleRow);

        topBar.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        body.addView(topBar);
    }


    public String formatTimeAgo(long time) {
        return com.mindscape.app.util.Texts.formatTimeAgo(time, "ru".equals(activeLanguage), str(R.string.str_just_now));
    }

    public View recentItem(Note note) {
        return com.mindscape.app.screens.CoreScreens.recentItem(this, note);
    }

    // --- MAP SCREEN (PixiJS WebView) ---
    public View mapScreen() {
        return MapScreens.mapScreen(this);
    }

    /** Helper: push fresh graph JSON to the live WebView without re-creating it */
    public void pushGraphDataToWebView() {
        if (mapWebView == null) return;
        String base64 = android.util.Base64.encodeToString(buildGraphJson().getBytes(java.nio.charset.StandardCharsets.UTF_8), android.util.Base64.NO_WRAP);
        mapWebView.evaluateJavascript(graphOptionsJs() + "setGraphData(decodeURIComponent(escape(window.atob('" + base64 + "'))))", null);
    }

    @Override
    public String graphOptionsJs() {
        try {
            JSONObject options = new JSONObject();
            options.put("nodeSize", nodeSizeValue);
            options.put("connectionDensity", connectionDensity);
            options.put("connectionAnimation", connectionAnimation);
            options.put("nodeGlow", nodeGlow);
            options.put("smoothZoom", smoothZoom);
            options.put("animationBoost", animationSmoothness == 0 ? 0.75 : (animationSmoothness == 1 ? 1.2 : 1.45));
            return "if(typeof setGraphOptions==='function'){setGraphOptions(" + options + ");}";
        } catch (Exception error) {
            android.util.Log.w("MindScape", "Unable to build graph options", error);
            return "";
        }
    }

    /** Build JSON payload for the PixiJS graph — full recursive hierarchy */

    // ---- Реализация GraphHost ----

    @Override
    public String buildGraphJson() {
        return GraphJsonBuilder.build(
                categoriesList,
                notesList,
                localFilesList,
                connectionsList,
                this::isNodeHidden,
                ROOT_NODE_TITLE
        );
    }

    @Override
    public String rootTitle() {
        return ROOT_NODE_TITLE;
    }

    @Override
    public void evalMapJs(String js) {
        runOnUiThread(() -> { if (mapWebView != null) mapWebView.evaluateJavascript(js, null); });
    }

    @Override
    public void onRootSelected() {
        runOnUiThread(() -> {
            selectedSection = "Структура";
            renderContent();
            updateBottomNav();
        });
    }

    @Override
    public void onEntitySelected(Object entity) {
        runOnUiThread(() -> {
            selectedMapEntity = entity;
            renderContent();
        });
    }

    

    // --- CREATE SCREEN ---
    public View createTabScreen() {
        return StructureScreens.createTabScreen(this);
    }

    public void addStructureHeader(LinearLayout body, LinearLayout listContainer) {
        StructureScreens.addStructureHeader(this, body, listContainer);
    }

    public void addDrillDownHeader(LinearLayout body, LinearLayout listContainer) {
        StructureScreens.addDrillDownHeader(this, body, listContainer);
    }
    
    public void renderStructureSelectionBar(LinearLayout top) {
        StructureScreens.renderStructureSelectionBar(this, top);
    }

    public void renderStructureRows(LinearLayout container) {
        StructureScreens.renderStructureRows(this, container);
    }

    /** Корневая строка-центр: разворачиваемая главная папка с собственным состоянием.
     *  index/total — позиция среди центров (для стрелок перемещения). */
    public void addCenterRootRow(LinearLayout list, Category cat, int index, int total, boolean isCenter) {
        StructureScreens.addCenterRootRow(this, list, cat, index, total, isCenter);
    }

    public void renderDrillDownRows(LinearLayout container) {
        StructureScreens.renderDrillDownRows(this, container);
    }

    public int getFolderItemsCount(String folderPath) {
        return StructureScreens.getFolderItemsCount(this, folderPath);
    }

    public View wrapWithDivider(View row, int depth) {
        return StructureScreens.wrapWithDivider(this, row, depth);
    }

    public View createCustomCheckbox(boolean selected) {
        TextView tv = new TextView(this);
        tv.setText(selected ? "✓" : "");
        tv.setTextColor(themedTextColor(Color.WHITE));
        tv.setTextSize(14);
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(roundedBg(selected ? accentColor() : Color.TRANSPARENT, 6, 2, selected ? accentColor() : strokeColor()));
        return tv;
    }

    public View createIndentLayout(int depth) {
        return StructureScreens.createIndentLayout(this, depth);
    }

    public void renderFolderChildren(LinearLayout list, @Nullable String parent, int depth) {
        StructureScreens.renderFolderChildren(this, list, parent, depth);
    }

    public View inlineAddRow(@Nullable String folderPath, int depth) {
        return StructureScreens.inlineAddRow(this, folderPath, depth);
    }

    public void focusEntityOnMap(Object entity) {
        selectedMapEntity = entity;
        selectedSection = "Карта";
        renderContent();
        updateBottomNav();
    }

    public View emptyStructureHint() {
        return StructureScreens.emptyStructureHint(this);
    }

    public View structureFolderRow(Category folder, int depth) {
        return StructureScreens.structureFolderRow(this, folder, depth);
    }

    public View structureNoteRow(Note note, int depth) {
        return StructureScreens.structureNoteRow(this, note, depth);
    }

    public View drillDownFolderRow(Category folder) {
        return StructureScreens.drillDownFolderRow(this, folder);
    }

    public View drillDownNoteRow(Note note) {
        return StructureScreens.drillDownNoteRow(this, note);
    }

    public View structureFileRow(LocalFileLink file, int depth) {
        return StructureScreens.structureFileRow(this, file, depth);
    }

    public View drillDownFileRow(LocalFileLink file) {
        return StructureScreens.drillDownFileRow(this, file);
    }

    public void addFileRowContent(LinearLayout row, LocalFileLink file) {
        StructureScreens.addFileRowContent(this, row, file);
    }

    public void showLocalFileDialog(LocalFileLink file) {
        LocalFileDialogs.showLocalFileDialog(this, file);
    }

    public void duplicateLocalFileBeside(LocalFileLink file) {
        LocalFileLink copy = new LocalFileLink(
                uniqueFileCopyTitle(file.title, file.folderPath),
                file.folderPath,
                file.uri,
                file.mimeType,
                file.size,
                System.currentTimeMillis()
        );
        localFilesList.add(copy);
        connectionsList.add(new Connection(file.nodeId(), copy.nodeId()));
        savePersistentData();
        reindexFileAsync(copy);
        pushGraphDataToWebView();
        Toast.makeText(this, str(R.string.str_file_duplicated), Toast.LENGTH_SHORT).show();
    }

    public void removeLocalFileLink(LocalFileLink file) {
        removeLocalFileLink(file, false);
    }

    public void removeLocalFileLink(LocalFileLink file, boolean deletePhysicalFile) {
        if (deletePhysicalFile) {
            com.mindscape.app.files.LocalFileOps.deletePhysicalFile(this, file);
        }
        String fileId = file.nodeId();
        localFilesList.remove(file);
        removeFileFromIndex(file);
        selectedLocalFiles.remove(file);
        connectionsList.removeIf(conn -> conn.source.equalsIgnoreCase(fileId) || conn.target.equalsIgnoreCase(fileId));
        if (selectedMapEntity == file) selectedMapEntity = null;
        savePersistentData();
        pushGraphDataToWebView();
    }

    public void confirmRemoveLocalFile(LocalFileLink file) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(14), dp(20), 0);
        layout.addView(text(str(R.string.str_delete), 20, primaryText(), true));
        layout.addView(spacer(8));
        View deleteFileSwitchView = createStyledSwitch("Также удалить кэш облачного файла", false);
        StyledToggleState deleteFileSwitch = (StyledToggleState) deleteFileSwitchView.getTag();
        layout.addView(deleteFileSwitchView);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(layout)
                .setPositiveButton(str(R.string.str_delete), (d, w) -> {
                    removeLocalFileLink(file, deleteFileSwitch.isChecked());
                    renderContent();
                    Toast.makeText(this, str(R.string.str_local_file_removed), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(str(R.string.str_cancel), null)
                .create();
        dialog.setOnShowListener(d -> styleFileDeleteDialog(dialog));
        dialog.show();
    }

    public void confirmRemoveLocalFiles(List<LocalFileLink> files) {
        if (files == null || files.isEmpty()) return;
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(14), dp(20), 0);
        layout.addView(text(str(R.string.str_delete), 20, primaryText(), true));
        layout.addView(spacer(8));
        View deleteFileSwitchView = createStyledSwitch("Также удалить кэш облачных файлов", false);
        StyledToggleState deleteFileSwitch = (StyledToggleState) deleteFileSwitchView.getTag();
        layout.addView(deleteFileSwitchView);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(layout)
                .setPositiveButton(str(R.string.str_delete), (d, w) -> {
                    boolean deletePhysical = deleteFileSwitch.isChecked();
                    for (LocalFileLink localFile : new ArrayList<>(files)) {
                        removeLocalFileLink(localFile, deletePhysical);
                    }
                    selectedLocalFiles.clear();
                    renderContent();
                })
                .setNegativeButton(str(R.string.str_cancel), null)
                .create();
        dialog.setOnShowListener(d -> styleFileDeleteDialog(dialog));
        dialog.show();
    }

    private void styleFileDeleteDialog(android.app.AlertDialog dialog) {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(roundedBg(surface(), 18, 1, strokeColor()));
        }
        Button positive = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
        Button negative = dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE);
        if (positive != null) {
            positive.setTextColor(accentColor());
            applyTextWeight(positive, true);
        }
        if (negative != null) {
            negative.setTextColor(secondaryText());
            applyTextWeight(negative, true);
        }
    }

    public String userFriendlyError(Throwable error) {
        String message = error == null || error.getMessage() == null ? "" : error.getMessage();
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("unable to resolve host") || lower.contains("failed to connect")
                || lower.contains("timeout") || lower.contains("timed out")
                || lower.contains("network is unreachable") || lower.contains("no address associated")) {
            return "ru".equals(activeLanguage)
                    ? "Подключитесь к интернету и попробуйте снова."
                    : "Connect to the internet and try again.";
        }
        if (lower.contains("401") || lower.contains("unauthorized")) {
            return "ru".equals(activeLanguage)
                    ? "Нет доступа к этому ресурсу."
                    : "You do not have access to this resource.";
        }
        if (lower.contains("403") || lower.contains("forbidden")) {
            return "ru".equals(activeLanguage)
                    ? "Нет доступа к этому файлу или папке."
                    : "No access to this file or folder.";
        }
        if (lower.contains("404") || lower.contains("not found")) {
            return "ru".equals(activeLanguage)
                    ? "Файл или папка не найдены."
                    : "File or folder not found.";
        }
        if (lower.contains("invalid_archive") || lower.contains("file is not a database") || lower.contains("no such table")) {
            return "ru".equals(activeLanguage)
                    ? "Это не backup MindScape или файл повреждён."
                    : "This is not a MindScape backup or the file is corrupted.";
        }
        if (com.mindscape.app.BuildConfig.DEBUG && (lower.contains("api-ключ") || lower.contains("провайдер") || lower.contains("лимит") || lower.contains("ai"))) {
            return message.length() > 180 ? message.substring(0, 180) + "..." : message;
        }
        return "ru".equals(activeLanguage)
                ? "Не удалось выполнить действие. Попробуйте позже."
                : "Could not complete the action. Try again later.";
    }

    public void showMoveLocalFileDialog(LocalFileLink file) {
        List<String> paths = getAllCategoryPaths();
        String noFolder = str(R.string.str_no_folder);
        paths.add(0, noFolder);
        showLocationPickerDialog(paths, file.folderPath == null ? noFolder : file.folderPath, selected -> {
            file.folderPath = selected.equals(noFolder) ? null : selected;
            savePersistentData();
            pushGraphDataToWebView();
            renderContent();
        });
    }

    public void openLocalFileExternal(LocalFileLink file) {
        com.mindscape.app.files.LocalFileOps.openLocalFileExternal(this, file);
    }

    public boolean isImageFile(LocalFileLink file) {
        return com.mindscape.app.files.FileFormats.isImage(file.mimeType, file.title);
    }

    public boolean isTextReadable(LocalFileLink file) {
        return com.mindscape.app.files.FileFormats.isTextReadable(file.mimeType, file.title);
    }

    public boolean isKnownBinaryDocument(String mime, String title) {
        return com.mindscape.app.files.FileFormats.isKnownBinaryDocument(mime, title);
    }

    public boolean isPdfFile(LocalFileLink file) {
        return com.mindscape.app.files.FileFormats.isPdf(file.mimeType, file.title);
    }

    public boolean isDocxFile(LocalFileLink file) {
        return com.mindscape.app.files.FileFormats.isDocx(file.mimeType, file.title);
    }

    public boolean isAiReadableFile(LocalFileLink file) {
        return com.mindscape.app.files.FileFormats.isAiReadable(file.mimeType, file.title);
    }

    public String readLocalFileText(LocalFileLink file, int maxChars) {
        try {
            return com.mindscape.app.files.LocalFileReader.readText(this, file, maxChars);
        } catch (Exception e) {
            return userFriendlyError(e);
        }
    }

    public void reindexFileAsync(LocalFileLink file) {
        if (file == null || !isAiReadableFile(file)) return;
        new Thread(() -> {
            try {
                fileSearchIndex().upsert(file, readLocalFileText(file, 120_000));
            } catch (Exception error) {
                android.util.Log.w("MindScape", "Unable to update file search index", error);
            }
        }).start();
    }

    public void reindexNotesAsync() {
        List<Note> snapshot = new ArrayList<>(notesList);
        new Thread(() -> {
            try {
                fileSearchIndex().indexNotes(snapshot);
            } catch (Exception error) {
                android.util.Log.w("MindScape", "Unable to update note content index", error);
            }
        }, "MindScapeNoteIndex").start();
    }

    public void removeFileFromIndex(LocalFileLink file) {
        try {
            fileSearchIndex().remove(file);
        } catch (Exception error) {
            android.util.Log.w("MindScape", "Unable to remove file from search index", error);
        }
    }

    public boolean fileMatchesSearch(LocalFileLink file, String query) {
        return fileSearchIndex().matches(file, query);
    }

    @Nullable
    public Uri saveTextToDownloadsMindscape(String name, String text) {
        try {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, name);
                values.put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/plain");
                values.put(android.provider.MediaStore.Downloads.RELATIVE_PATH, "Download/MindScape/");
                Uri uri = getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (java.io.OutputStream os = getContentResolver().openOutputStream(uri)) {
                        if (os != null) os.write(bytes);
                    }
                }
                return uri;
            } else {
                java.io.File dir = new java.io.File(
                        android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                        "MindScape");
                if (!dir.exists()) dir.mkdirs();
                java.io.File file = new java.io.File(dir, name);
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                    fos.write(bytes);
                }
                return Uri.fromFile(file);
            }
        } catch (Exception error) {
            android.util.Log.w("MindScape", "Unable to save text file to Downloads/MindScape", error);
            return null;
        }
    }

    public byte[] readStreamToBytes(@Nullable InputStream stream, long maxBytes) throws Exception {
        return com.mindscape.app.files.LocalFileReader.readStreamToBytes(stream, maxBytes);
    }

    public String extractReadableText(LocalFileLink file, byte[] bytes, int maxChars) throws Exception {
        return com.mindscape.app.files.LocalFileReader.extractReadableText(file, bytes, maxChars);
    }

    public String extractDocxText(byte[] bytes) throws Exception {
        return com.mindscape.app.files.LocalFileReader.extractDocxText(bytes);
    }

    public String limitText(String text, int maxChars) {
        return com.mindscape.app.files.LocalFileReader.limitText(text, maxChars);
    }

    public String fileMimeLabel(LocalFileLink file) {
        return file.mimeType == null || file.mimeType.isEmpty() ? str(R.string.str_unknown_format) : file.mimeType;
    }

    public String fileIconName(LocalFileLink file) {
        return com.mindscape.app.files.FileFormats.iconName(file.mimeType, file.title);
    }

    public int fileIconColor(LocalFileLink file) {
        return com.mindscape.app.files.FileFormats.iconColor(fileIconName(file));
    }

    public boolean hasFileExtension(String title, String... extensions) {
        return com.mindscape.app.files.FileFormats.hasExtension(title, extensions);
    }

    public String formatFileSize(long bytes) {
        return com.mindscape.app.util.Texts.formatFileSize(bytes, str(R.string.str_unknown_size));
    }

    public LinearLayout actionTile(String title, String desc, int bgColor, String drawableName) {
        return com.mindscape.app.screens.CoreScreens.actionTile(this, title, desc, bgColor, drawableName);
    }

    public List<Category> childFolders(@Nullable String parent) {
        return com.mindscape.app.tree.TreeOps.childFolders(categoriesList, parent);
    }

    public List<Note> notesInFolder(@Nullable String folderPath) {
        return com.mindscape.app.tree.TreeOps.notesInFolder(notesList, folderPath);
    }

    public List<LocalFileLink> localFilesInFolder(@Nullable String folderPath) {
        return com.mindscape.app.tree.TreeOps.localFilesInFolder(localFilesList, folderPath);
    }

    public String folderName(String path) {
        return com.mindscape.app.tree.TreeOps.folderName(path);
    }

    @Nullable
    public String parentPath(@Nullable String path) {
        return com.mindscape.app.tree.TreeOps.parentPath(path);
    }

    public boolean isLinked(Object entity) {
        return isNodeLinked(entity);
    }

    public android.widget.ImageView quickLinkIcon(String sourceId, boolean isFolder) {
        return quickIndicatorIcon("ic_node_link", Color.rgb(65, 120, 220),
                v -> showConnectionsDialog(sourceId, isFolder));
    }

    public android.widget.ImageView quickVisibilityIcon(Object entity) {
        return quickIndicatorIcon("ic_eye_off", Color.rgb(120, 126, 140),
                v -> showVisibilityQuickDialog(entity));
    }

    public android.widget.ImageView quickReminderIcon(Note note) {
        return quickIndicatorIcon("ic_clock", Color.rgb(65, 120, 220), v -> editNoteDialog(note));
    }

    public android.widget.ImageView quickIndicatorIcon(String drawableName, int color, View.OnClickListener listener) {
        android.widget.ImageView icon = new android.widget.ImageView(this);
        icon.setImageResource(getResources().getIdentifier(drawableName, "drawable", getPackageName()));
        icon.setColorFilter(color);
        icon.setPadding(dp(4), dp(4), dp(4), dp(4));
        icon.setClickable(true);
        icon.setFocusable(true);
        icon.setOnClickListener(listener);
        return icon;
    }

    public void showVisibilityQuickDialog(Object entity) {
        com.mindscape.app.ui.CoreDialogs.showVisibilityQuickDialog(this, entity);
    }

    public String entityTitle(Object entity) {
        if (entity instanceof Note) return ((Note) entity).title;
        if (entity instanceof Category) return ((Category) entity).title;
        if (entity instanceof String) return folderName((String) entity);
        return str(R.string.app_name);
    }

    public void showAddContentDialog(@Nullable String folderPath) {
        StructureDialogs.showAddContentDialog(this, folderPath);
    }

    public void createQuickContainerNote(EditText titleInput, EditText contentInput) {
        String title = titleInput.getText().toString().trim();
        String content = contentInput.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, str(R.string.str_enter_note_title), Toast.LENGTH_SHORT).show();
            return;
        }
        if (noteExists(title, null)) {
            Toast.makeText(this, str(R.string.str_note_already_exists), Toast.LENGTH_SHORT).show();
            return;
        }
        Note note = new Note(title, null, content);
        note.quickContainerNote = true;
        notesList.add(note);
        titleInput.setText("");
        contentInput.setText("");
        savePersistentData();
        renderContent();
        Toast.makeText(this, "ru".equals(activeLanguage) ? "Сохранено в контейнер" : "Saved to container", Toast.LENGTH_SHORT).show();
    }

    public void showQuickNotesContainerDialog() {
        List<Note> quickNotes = quickContainerNotes();
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(18), dp(14), dp(18), dp(8));
        boolean ru = "ru".equals(activeLanguage);
        layout.addView(text(ru ? "Контейнер быстрых заметок" : "Quick notes container", 18, primaryText(), true));
        layout.addView(text(ru ? "Здесь хранятся нераспределенные записи" : "Unassigned notes are stored here", 12, secondaryText(), false));
        layout.addView(spacer(10));

        final android.app.AlertDialog[] dialogRef = new android.app.AlertDialog[1];
        if (quickNotes.isEmpty()) {
            layout.addView(text(ru ? "Контейнер пуст" : "The container is empty", 14, secondaryText(), false));
        } else {
            for (Note note : quickNotes) {
                layout.addView(quickContainerNoteRow(note, dialogRef));
            }
        }

        Button close = createStyledButton(str(R.string.str_back));
        close.setOnClickListener(v -> {
            if (dialogRef[0] != null) dialogRef[0].dismiss();
        });
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        closeParams.setMargins(0, dp(4), 0, dp(2));
        layout.addView(close, closeParams);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(scrollableDialogContent(layout, 560))
                .create();
        dialogRef[0] = dialog;
        dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(roundedBg(surface(), 16, 0, Color.TRANSPARENT));
    }

    private View quickContainerNoteRow(Note note, android.app.AlertDialog[] parentDialogRef) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(8), dp(10));
        row.setBackground(roundedBg(softSurface(), 14, 1, strokeColor()));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(text(note.title, 15, primaryText(), true));
        String preview = note.content == null || note.content.trim().isEmpty()
                ? ("ru".equals(activeLanguage) ? "Без описания" : "No description")
                : note.content.trim();
        if (preview.length() > 90) preview = preview.substring(0, 90) + "...";
        copy.addView(text(preview, 12, secondaryText(), false));
        row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView dots = text("⋮", 24, secondaryText(), true);
        dots.setGravity(Gravity.CENTER);
        row.addView(dots, new LinearLayout.LayoutParams(dp(44), dp(44)));
        row.setOnClickListener(v -> {
            if (parentDialogRef[0] != null) parentDialogRef[0].dismiss();
            editNoteDialog(note);
        });
        dots.setOnClickListener(v -> showQuickContainerNoteActions(note, parentDialogRef));

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(row);
        wrapper.addView(spacer(8));
        return wrapper;
    }

    private void showQuickContainerNoteActions(Note note, android.app.AlertDialog[] parentDialogRef) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(18), dp(14), dp(18), dp(8));
        layout.addView(text(note.title, 18, primaryText(), true));
        layout.addView(spacer(8));

        final android.app.AlertDialog[] dialogRef = new android.app.AlertDialog[1];
        boolean ru = "ru".equals(activeLanguage);
        layout.addView(actionDialogRow(
                ru ? "Открыть" : "Open",
                ru ? "Редактировать текст заметки" : "Edit note text",
                "ic_create_note", v -> {
            if (dialogRef[0] != null) dialogRef[0].dismiss();
            if (parentDialogRef[0] != null) parentDialogRef[0].dismiss();
            editNoteDialog(note);
        }));
        layout.addView(actionDialogRow(
                ru ? "Переместить в..." : "Move to...",
                ru ? "Выбрать папку или центр" : "Choose a folder or center",
                "ic_create_category", v -> {
            if (dialogRef[0] != null) dialogRef[0].dismiss();
            if (parentDialogRef[0] != null) parentDialogRef[0].dismiss();
            showMoveQuickContainerNoteDialog(note);
        }));

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(layout)
                .create();
        dialogRef[0] = dialog;
        dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(roundedBg(surface(), 16, 0, Color.TRANSPARENT));
    }

    public void showAddFromContainerDialog(@Nullable String folderPath) {
        List<Note> quickNotes = quickContainerNotes();
        if (quickNotes.isEmpty()) {
            Toast.makeText(this, "ru".equals(activeLanguage) ? "Контейнер пуст" : "The container is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(18), dp(14), dp(18), dp(8));
        boolean ru = "ru".equals(activeLanguage);
        layout.addView(text(ru ? "Добавить из контейнера" : "Add from container", 18, primaryText(), true));
        layout.addView(text(folderPath == null ? (ru ? "Выберите заметку для главного центра" : "Choose a note for the main center") : folderPath.replace("/", " > "), 12, secondaryText(), false));
        layout.addView(spacer(10));

        final android.app.AlertDialog[] dialogRef = new android.app.AlertDialog[1];
        for (Note note : quickNotes) {
            String preview = note.content == null || note.content.trim().isEmpty() ? (ru ? "Без описания" : "No description") : note.content.trim();
            if (preview.length() > 90) preview = preview.substring(0, 90) + "...";
            View row = actionDialogRow(note.title, preview, "ic_create_note", v -> {
                if (dialogRef[0] != null) dialogRef[0].dismiss();
                moveQuickContainerNote(note, folderPath);
            });
            layout.addView(row);
        }

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(scrollableDialogContent(layout, 560))
                .create();
        dialogRef[0] = dialog;
        dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(roundedBg(surface(), 16, 0, Color.TRANSPARENT));
    }

    private void showMoveQuickContainerNoteDialog(Note note) {
        List<String> paths = getAllCategoryPaths();
        if (paths.isEmpty()) {
            Toast.makeText(this, str(R.string.str_create_categories_first), Toast.LENGTH_SHORT).show();
            return;
        }
        showLocationPickerDialog(paths, note.categoryPath, selected -> moveQuickContainerNote(note, selected));
    }

    private void moveQuickContainerNote(Note note, @Nullable String folderPath) {
        if (folderPath != null && noteExists(note.title, folderPath)) {
            Toast.makeText(this, str(R.string.str_note_already_exists), Toast.LENGTH_SHORT).show();
            return;
        }
        String oldPath = note.fullPath();
        note.categoryPath = folderPath;
        note.quickContainerNote = false;
        note.updatedAt = System.currentTimeMillis();
        String newPath = note.fullPath();
        if (!oldPath.equalsIgnoreCase(newPath)) {
            String oldKey = "note:" + oldPath;
            String newKey = "note:" + newPath;
            for (Connection conn : connectionsList) {
                if (conn.source.equalsIgnoreCase(oldKey)) conn.source = newKey;
                else if (conn.source.equalsIgnoreCase(oldPath)) conn.source = newPath;
                if (conn.target.equalsIgnoreCase(oldKey)) conn.target = newKey;
                else if (conn.target.equalsIgnoreCase(oldPath)) conn.target = newPath;
            }
            if (hiddenNodes.contains(oldKey)) { hiddenNodes.remove(oldKey); hiddenNodes.add(newKey); }
            if (hiddenNodes.contains(oldPath)) { hiddenNodes.remove(oldPath); hiddenNodes.add(newPath); }
        }
        savePersistentData();
        renderContent();
        Toast.makeText(this, "Заметка перенесена", Toast.LENGTH_SHORT).show();
    }

    private List<Note> quickContainerNotes() {
        List<Note> result = new ArrayList<>();
        for (Note note : notesList) {
            if (note.quickContainerNote && note.isUnbound()) result.add(note);
        }
        result.sort((a, b) -> Long.compare(Math.max(b.createdAt, b.updatedAt), Math.max(a.createdAt, a.updatedAt)));
        return result;
    }

    public int quickContainerNoteCount() {
        return quickContainerNotes().size();
    }

    public void showFolderActionsDialog(@Nullable String folderPath) {
        StructureDialogs.showFolderActionsDialog(this, folderPath);
    }

    public boolean categoryExistsGlobally(String title) {
        for (Category c : categoriesList) {
            if (c.title.equalsIgnoreCase(title)) return true;
        }
        return false;
    }

    public void duplicateCenter(Category originalCenter) {
        String newCenterTitle = originalCenter.title;
        int suffix = 1;
        while (categoryExists(newCenterTitle + " (" + suffix + ")", null)) {
            suffix++;
        }
        newCenterTitle = newCenterTitle + " (" + suffix + ")";

        Random r = new Random();
        int color = Color.rgb(100 + r.nextInt(120), 100 + r.nextInt(120), 100 + r.nextInt(120));
        Category newCenter = new Category(newCenterTitle, originalCenter.description, color, null, true);
        categoriesList.add(newCenter);

        duplicateCategoryContent(originalCenter.fullPath(), newCenter.fullPath());

        savePersistentData();
        pushGraphDataToWebView();
        renderContent();
        Toast.makeText(this, str(R.string.str_center_duplicated), Toast.LENGTH_SHORT).show();
    }

    public void duplicateCategoryContent(String oldPath, String newPath) {
        com.mindscape.app.tree.TreeOps.duplicateCategoryContent(this, oldPath, newPath);
    }

    public View actionDialogRow(String title, String subtitle, String iconName, View.OnClickListener listener) {
        return actionDialogRow(title, subtitle, iconName, Color.rgb(65, 120, 220), listener);
    }

    public View actionDialogRow(String title, String subtitle, String iconName, int iconTint, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));
        row.addView(iconAction(iconName, iconTint), new LinearLayout.LayoutParams(dp(40), dp(40)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(text(title, 15, primaryText(), true));
        copy.addView(text(subtitle, 12, secondaryText(), false));
        row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.setOnClickListener(listener);
        return row;
    }

    public void showStyledChoiceDialog(String title, String[] items, String iconName, StyledChoiceHandler handler) {
        com.mindscape.app.ui.CoreDialogs.showStyledChoiceDialog(this, title, items, iconName, handler);
    }

    public void showCreateFolderDialog(@Nullable String parent) {
        StructureDialogs.showCreateFolderDialog(this, parent);
    }

    public void showCreateCenterDialog() {
        StructureDialogs.showCreateCenterDialog(this);
    }

    public static final class ReminderDraft {
        public boolean enabled;
        public long at;

        public ReminderDraft() {
            enabled = false;
            at = defaultReminderTime();
        }

        public ReminderDraft(Note note) {
            enabled = note.reminderEnabled;
            at = note.reminderAt > 0 ? note.reminderAt : defaultReminderTime();
        }
    }

    public interface StyledChoiceHandler {
        void onChoice(int index);
    }

    public static long defaultReminderTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public View reminderEditor(ReminderDraft draft) {
        return NoteDialogs.reminderEditor(this, draft);
    }

    public View labelValueRow(String label, TextView value) {
        return NoteDialogs.labelValueRow(this, label, value);
    }

    public void showReminderDateTimePicker(ReminderDraft draft, TextView timeLabel) {
        NoteDialogs.showReminderDateTimePicker(this, draft, timeLabel);
    }

public String reminderTimeLabel(ReminderDraft draft) {
        return NoteDialogs.reminderTimeLabel(this, draft);
    }

    public void applyReminderDraft(Note note, ReminderDraft draft) {
        NoteDialogs.applyReminderDraft(note, draft);
    }

    public void showCreateStructureNoteDialog(@Nullable String folderPath) {
        NoteDialogs.showCreateStructureNoteDialog(this, folderPath);
    }

    public void showNoteActionsDialog(Note note) {
        StructureDialogs.showNoteActionsDialog(this, note);
    }

    public void showConnectionsDialog(String sourceId, boolean isFolder) {
        StructureDialogs.showConnectionsDialog(this, sourceId, isFolder);
    }

    public void showLinkDialog(String sourceId, boolean isFolder) {
        StructureDialogs.showLinkDialog(this, sourceId, isFolder);
    }

    public void showMoveNoteDialog(Note note) {
        StructureDialogs.showMoveNoteDialog(this, note);
    }

    public void showRenameFolderDialog(String folderPath) {
        StructureDialogs.showRenameFolderDialog(this, folderPath);
    }

    public void deleteFolderSilent(String folderPath, boolean deleteContents) {
        com.mindscape.app.tree.TreeOps.deleteFolderSilent(this, folderPath, deleteContents);
    }

    public void deleteFolder(String folderPath) {
        StructureDialogs.showDeleteFolderDialog(this, folderPath);
    }


    // --- NOTES TAB SCREEN ---
    public View notesTabScreen() {
        return NotesScreens.notesTabScreen(this);
    }

    public View filterChip(String filter, String label, String iconName) {
        boolean selected = activeNotesFilter.equals(filter);
        int idleBg = isDarkTheme() ? Color.rgb(32, 37, 46) : softSurface();
        
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setMinimumHeight(dp(34));
        chip.setPadding(dp(12), dp(6), dp(12), dp(6));
        chip.setBackground(roundedBg(selected ? Color.rgb(65, 120, 220) : idleBg, 12, selected ? 0 : 1, selected ? Color.TRANSPARENT : strokeColor()));
        chip.setOnClickListener(v -> {
            activeNotesFilter = filter;
            selectedNotes.clear();
            renderContent();
        });

        if (iconName != null && !iconName.isEmpty()) {
            android.widget.ImageView icon = new android.widget.ImageView(this);
            icon.setImageResource(getResources().getIdentifier(iconName, "drawable", getPackageName()));
            icon.setColorFilter(selected ? Color.WHITE : secondaryText());
            chip.addView(icon, new LinearLayout.LayoutParams(dp(18), dp(18)));
        }

        TextView titleView = text(label, 13, selected ? Color.WHITE : secondaryText(), true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (iconName != null && !iconName.isEmpty()) {
            titleParams.setMargins(dp(6), 0, 0, 0);
        }
        chip.addView(titleView, titleParams);

        return chip;
    }

    public String fileTypeFilterLabel() {
        switch (activeFileTypeFilter) {
            case "notes": return "Заметки";
            case "files": return "Файлы";
            case "pdf": return "PDF";
            case "word": return "Word";
            case "sheets": return "Таблицы";
            case "presentations": return "Презентации";
            case "images": return "Изображения";
            case "other": return "Другие";
            default: return "Все";
        }
    }

    public void showFileTypeFilterDialog() {
        com.mindscape.app.ui.CoreDialogs.showFileTypeFilterDialog(this);
    }

    public boolean fileMatchesActiveTypeFilter(LocalFileLink file) {
        return fileMatchesType(file, activeFileTypeFilter);
    }

    public boolean fileMatchesType(LocalFileLink file, String type) {
        if ("notes".equals(type)) return false;
        if ("all".equals(type) || "files".equals(type)) return true;
        String mime = file.mimeType == null ? "" : file.mimeType.toLowerCase(Locale.ROOT);
        String title = file.title == null ? "" : file.title.toLowerCase(Locale.ROOT);
        switch (type) {
            case "pdf":
                return mime.contains("pdf") || title.endsWith(".pdf");
            case "word":
                return mime.contains("word") || mime.contains("officedocument.wordprocessingml") || title.endsWith(".doc") || title.endsWith(".docx");
            case "sheets":
                return mime.contains("spreadsheet") || mime.contains("excel") || title.endsWith(".xls") || title.endsWith(".xlsx") || title.endsWith(".csv");
            case "presentations":
                return mime.contains("presentation") || mime.contains("powerpoint") || title.endsWith(".ppt") || title.endsWith(".pptx");
            case "images":
                return mime.startsWith("image/") || com.mindscape.app.files.FileFormats.hasExtension(title, ".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp", ".heic", ".heif", ".svg");
            case "other":
                return !fileMatchesType(file, "pdf")
                        && !fileMatchesType(file, "word")
                        && !fileMatchesType(file, "sheets")
                        && !fileMatchesType(file, "presentations")
                        && !fileMatchesType(file, "images");
            default:
                return true;
        }
    }

    public void addChipWithMargin(LinearLayout row, View chipView) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
        params.setMargins(0, 0, dp(8), 0);
        row.addView(chipView, params);
    }

    public void refreshCategoryPickerSelection(LinearLayout catList, @Nullable String selectedPath) {
        for (int i = 0; i < catList.getChildCount(); i++) {
            View child = catList.getChildAt(i);
            Object tag = child.getTag();
            if (!(tag instanceof String)) {
                continue;
            }
            String path = (String) tag;
            boolean selected = selectedPath != null && selectedPath.equals(path);
            child.setBackground(roundedBg(selected ? Color.rgb(235, 244, 255) : Color.TRANSPARENT, 10, selected ? 1 : 0, selected ? accentColor() : Color.TRANSPARENT));
            if (child instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) child;
                for (int j = 0; j < row.getChildCount(); j++) {
                    View inner = row.getChildAt(j);
                    if (inner instanceof TextView) {
                        applyTextWeight((TextView) inner, selected);
                    }
                }
            }
        }
    }

    public void deleteNote(Note note) {
        List<Note> single = new ArrayList<>();
        single.add(note);
        deleteNotes(single);
    }

    public void deleteNotesSilent(List<Note> notesToDelete) {
        com.mindscape.app.tree.TreeOps.deleteNotesSilent(this, notesToDelete);
    }

    public void deleteNotes(List<Note> notesToDelete) {
        deleteNotesSilent(notesToDelete);
        Toast.makeText(this,
                "ru".equals(activeLanguage) ? "Удалено: " + notesToDelete.size() : "Deleted: " + notesToDelete.size(),
                Toast.LENGTH_SHORT).show();
    }

    public int getProviderIndex(String baseUrl) {
        return com.mindscape.app.ai.AiProviders.indexOf(baseUrl);
    }
    /** Creates a clickable "dropdown" view styled like createStyledInput. Shows a dialog on tap. */
    public View createStyledDropdown(String currentValue, String[] items, int selectedIdx, java.util.function.IntConsumer onSelect) {
        return StyledControls.dropdown(this, themeState(), str(R.string.str_cancel), currentValue, items, selectedIdx, onSelect);
    }
    public View spacer(int sizeDp) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(sizeDp)));
        return view;
    }

    public View createStyledSlider(int max, int currentProgress, String unit, final android.widget.SeekBar.OnSeekBarChangeListener clientListener) {
        return StyledControls.slider(this, themeState(), max, currentProgress, unit, clientListener);
    }

    public EditText createStyledInput(String initialText, String hint) {
        return StyledControls.input(this, themeState(), initialText, hint);
    }

    public void configureLargeTextInput(EditText input, int heightDp) {
        StyledControls.configureLargeTextInput(this, input, heightDp);
    }

    public ScrollView scrollableDialogContent(View content, int maxHeightDp) {
        return StyledControls.scrollableDialogContent(this, content, maxHeightDp);
    }

    public View createStyledSwitch(String text, boolean checked) {
        return StyledControls.switchToggle(this, themeState(), text, checked);
    }

    public Button createStyledButton(String text) {
        return StyledControls.button(this, themeState(), text);
    }

    public Button createPrimaryButton(String text) {
        return StyledControls.primaryButton(this, themeState(), text);
    }


    // --- AI ASSISTANT VIEW ---
    public View aiScreen() {
        return AiScreens.aiScreen(this);
    }

    public View renderChatMessage(ChatMessage msg) {
        return AiScreens.renderChatMessage(this, msg);
    }

    public void updateAiMsgOnUi(ChatMessage msg, View msgView, ScrollView scroll, String rawText, boolean isDone) {
        AiScreens.updateAiMsgOnUi(this, msg, msgView, scroll, rawText, isDone);
    }

    public String filterAiCommands(String text) {
        return AiCommandParser.stripCommands(text);
    }

    public java.util.List<String> extractAiCommands(String text) {
        java.util.List<String> cmds = new java.util.ArrayList<>();
        for (AiCommandParser.Command command : AiCommandParser.parse(text)) {
            String type = command.type;
            String[] args = command.args;
            String name = command.firstArg();
            if (type.equals("CREATE_CENTER")) cmds.add(str(R.string.str_cmd_create_center) + name);
            else if (type.equals("MOVE_CENTER")) cmds.add(str(R.string.str_cmd_move_center) + name + (args.length > 1 ? " → " + args[1] : ""));
            else if (type.equals("CREATE_CATEGORY")) cmds.add(str(R.string.str_cmd_create_category) + name);
            else if (type.equals("CREATE_NOTE")) cmds.add(str(R.string.str_cmd_create_note) + name);
            else if (type.equals("CREATE_CONNECTION")) cmds.add(str(R.string.str_create_connection) + ": " + name);
            else if (type.equals("DELETE_NODE")) cmds.add(str(R.string.str_cmd_delete) + name);
            else if (type.equals("HIDE_NODE")) cmds.add(str(R.string.str_cmd_hide) + name);
            else if (type.equals("SHOW_NODE")) cmds.add(str(R.string.str_cmd_show) + name);
            else if (type.equals("DELETE_FILE_LINK")) cmds.add(str(R.string.str_cmd_delete) + str(R.string.str_menu_local_file) + ": " + name);
            else if (type.equals("MOVE_FILE_LINK")) cmds.add(str(R.string.str_menu_move) + ": " + name + (args.length > 1 ? " → " + args[1] : ""));
            else if (type.equals("COPY_FILE_LINK")) cmds.add(str(R.string.str_menu_duplicate) + ": " + name + (args.length > 1 ? " → " + args[1] : ""));
        }
        return cmds;
    }

    /** Check if a category with given title already exists under the given parent path. */
    public boolean categoryExists(String title, String parentPath) {
        return com.mindscape.app.tree.TreeOps.categoryExists(categoriesList, title, parentPath);
    }

    /** Check if a note with given title already exists (optionally in the given category). */
    public boolean noteExists(String title, String catPath) {
        return com.mindscape.app.tree.TreeOps.noteExists(notesList, title, catPath);
    }

    public String uniqueNoteCopyTitle(String originalTitle, @Nullable String catPath) {
        return uniqueCopyTitle(originalTitle, candidate -> noteExists(candidate, catPath));
    }

    public String uniqueFileCopyTitle(String originalTitle, @Nullable String folderPath) {
        return uniqueCopyTitle(originalTitle, candidate -> {
            for (LocalFileLink file : localFilesList) {
                boolean sameFolder = (folderPath == null && file.folderPath == null)
                        || (folderPath != null && folderPath.equals(file.folderPath));
                if (sameFolder && file.title.equalsIgnoreCase(candidate)) return true;
            }
            return false;
        });
    }

    private interface NameExists {
        boolean exists(String candidate);
    }

    public String uniqueCopyTitle(String originalTitle, NameExists exists) {
        String suffix = str(R.string.str_copy_suffix);
        String base = originalTitle + " " + suffix;
        if (!exists.exists(base)) return base;
        int index = 1;
        while (exists.exists(base + "(" + index + ")")) {
            index++;
        }
        return base + "(" + index + ")";
    }

    public void migrateLegacyConnectionsAndHiddenNodes() {
        com.mindscape.app.data.PersistenceManager.migrateLegacyConnectionsAndHiddenNodes(this);
    }

    public String cleanNodeId(String id) {
        return NodeStateManager.cleanNodeId(id);
    }

    public String normalizeConnectionNodeId(String id) {
        return NodeStateManager.normalizeConnectionNodeId(id);
    }

    public String resolvePrefixedNodeId(String rawName) {
        LocalFileLink file = findLocalFileLink(rawName);
        if (file != null) return file.nodeId();
        return NodeStateManager.resolvePrefixedNodeId(rawName, notesList, categoriesList);
    }

    public String displayNodeId(String nodeId) {
        if (nodeId == null) return "";
        nodeId = normalizeConnectionNodeId(nodeId);
        if (nodeId.startsWith("file:")) {
            for (LocalFileLink file : localFilesList) {
                if (file.nodeId().equalsIgnoreCase(nodeId)) return file.title;
            }
        }
        String cleanId = cleanNodeId(nodeId);
        return cleanId.contains("/") ? cleanId.replace("/", " > ") : cleanId;
    }

    public boolean isNodeLinked(Object entity) {
        return NodeStateManager.isNodeLinked(entity, connectionsList);
    }

    public boolean isNodeHidden(Object entity) {
        return NodeStateManager.isNodeHidden(entity, hiddenNodes);
    }

    public void toggleNodeHidden(Object entity) {
        if (entity instanceof Category) {
            com.mindscape.app.tree.HiddenNodesManager.toggleFolderSubtreeHidden(categoriesList, notesList, hiddenNodes, ((Category) entity).fullPath());
        } else if (entity instanceof String && categoryPathExists((String) entity)) {
            com.mindscape.app.tree.HiddenNodesManager.toggleFolderSubtreeHidden(categoriesList, notesList, hiddenNodes, (String) entity);
        } else if (entity instanceof Note) {
            Note note = (Note) entity;
            com.mindscape.app.tree.HiddenNodesManager.setNoteHidden(hiddenNodes, note, !isNodeHidden(note));
        } else {
            NodeStateManager.toggleNodeHidden(entity, hiddenNodes);
        }
    }

    public boolean categoryPathExists(String path) {
        return com.mindscape.app.tree.HiddenNodesManager.categoryPathExists(categoriesList, path);
    }

    public void toggleFolderSubtreeHidden(String folderPath) {
        com.mindscape.app.tree.HiddenNodesManager.toggleFolderSubtreeHidden(categoriesList, notesList, hiddenNodes, folderPath);
    }

    public void setFolderSubtreeHidden(String folderPath, boolean hidden) {
        com.mindscape.app.tree.HiddenNodesManager.setFolderSubtreeHidden(categoriesList, notesList, hiddenNodes, folderPath, hidden);
    }

    public boolean isPathInSubtree(@Nullable String path, String rootPath) {
        return com.mindscape.app.tree.HiddenNodesManager.isPathInSubtree(path, rootPath);
    }

    public void setCategoryHidden(Category category, boolean hidden) {
        com.mindscape.app.tree.HiddenNodesManager.setCategoryHidden(hiddenNodes, category, hidden);
    }

    public void setNoteHidden(Note note, boolean hidden) {
        com.mindscape.app.tree.HiddenNodesManager.setNoteHidden(hiddenNodes, note, hidden);
    }

    public long lastAiRequestTime() { return lastAiRequestTime; }
    public void lastAiRequestTime(long v) { lastAiRequestTime = v; }
    public long aiRequestCooldownMs() { return AI_REQUEST_COOLDOWN_MS; }
    public int consecutiveAiErrors() { return consecutiveAiErrors; }
    public void consecutiveAiErrors(int v) { consecutiveAiErrors = v; }

    public void executeAiCommands(String response) {
        com.mindscape.app.ai.AiCommandExecutor.execute(this, response);
    }

    @Nullable
    public LocalFileLink findLocalFileLink(String rawName) {
        if (rawName == null) return null;
        String qRaw = sanitizeInput(rawName, 500).toLowerCase(Locale.ROOT);
        for (LocalFileLink file : localFilesList) {
            if (file.nodeId().toLowerCase(Locale.ROOT).equals(qRaw)) {
                return file;
            }
        }
        String q = qRaw.startsWith("file:") ? qRaw.substring(5) : qRaw;
        LocalFileLink partial = null;
        for (LocalFileLink file : localFilesList) {
            String title = file.title == null ? "" : file.title.toLowerCase(Locale.ROOT);
            String uri = file.uri == null ? "" : file.uri.toLowerCase(Locale.ROOT);
            String pathTitle = (file.folderPath == null ? "" : file.folderPath + "/") + file.title;
            if (title.equals(q) || uri.equals(q) || pathTitle.toLowerCase(Locale.ROOT).equals(q)) {
                return file;
            }
            if (partial == null && (title.contains(q) || q.contains(title))) {
                partial = file;
            }
        }
        return partial;
    }

    @Nullable
    public String normalizeAiFolderTarget(String rawTarget) {
        if (rawTarget == null) return null;
        String target = sanitizeInput(rawTarget, 300).trim();
        if (target.isEmpty() || target.equalsIgnoreCase("null") || target.equalsIgnoreCase("none") || target.equalsIgnoreCase(str(R.string.str_no_folder))) {
            return null;
        }
        return target;
    }

    public void streamAiResponse(String query, ChatMessage aiMsg, View aiMsgView, ScrollView scroll) {
        com.mindscape.app.ai.AiStreamRunner.stream(this, query, aiMsg, aiMsgView, scroll);
    }

    public String formatAiHttpError(int code, String responseBody) {
        return com.mindscape.app.ai.AiHelpers.formatAiHttpError(code, responseBody, str(R.string.str_error));
    }



    public void showAiSessionsDialog() {
        AiScreens.showAiSessionsDialog(this);
    }

    // --- DIALOGS & UTILS ---

    public void createNoteDialog() {
        NoteDialogs.createNoteDialog(this);
    }

    public void editNoteDialog(Note note) {
        NoteDialogs.editNoteDialog(this, note);
    }

    public interface LocationSelected {
        void onSelected(String path);
    }

    public void showLocationPickerDialog(List<String> paths, @Nullable String currentPath, LocationSelected callback) {
        NoteDialogs.showLocationPickerDialog(this, paths, currentPath, callback::onSelected);
    }

    public void createCategoryDialog() {
        CategoryDialogs.createCategoryDialog(this);
    }

    public void createCategoryDialog(String initialParentPath) {
        CategoryDialogs.createCategoryDialog(this, initialParentPath);
    }

    public void renderTree(LinearLayout container, CatTreeNode node, int depth, String selectedPath) {
        CategoryDialogs.renderTree(this, container, node, depth, selectedPath);
    }

    public void createConnectionDialog() {
        CategoryDialogs.createConnectionDialog(this);
    }

    public void showUnboundNotesDialog() {
        CategoryDialogs.showUnboundNotesDialog(this);
    }

    public void showBindNoteDialog(@Nullable Note preselectedNote) {
        CategoryDialogs.showBindNoteDialog(this, preselectedNote);
    }

    public void importDialog() {
        CategoryDialogs.importDialog(this);
    }

    

    public String callRouterAi() throws Exception {
        return AiClient.callChatCompletion(
                aiProviderBaseUrl,
                aiProviderApiKey,
                aiProviderModel,
                aiMaxTokens,
                str(R.string.str_ai_test_prompt_1),
                str(R.string.str_ai_test_prompt_2_prefix) + notesSummary() + "\n" + localFilesSummary("")
                        + str(R.string.str_ai_test_prompt_2_suffix)
        );
    }

    public String readAll(InputStream stream) throws Exception {
        return com.mindscape.app.ai.AiHelpers.readAll(stream);
    }

    public String categoriesSummary() {
        return com.mindscape.app.ai.AiHelpers.categoriesSummary(categoriesList, notesList, str(R.string.str_no_categories_sys));
    }

    /** Считает количество подкатегорий и заметок внутри узла (рекурсивно, включая всё поддерево). */
    public int[] countContents(String rootPath) {
        return KnowledgeTreeService.countContents(categoriesList, notesList, rootPath);
    }

    public String notesSummary() {
        return com.mindscape.app.ai.AiHelpers.notesSummary(notesList, str(R.string.str_no_notes_sys));
    }

    public String localFilesSummary(String userQuery) {
        if (localFilesList.isEmpty()) {
            return "Локальные файлы: нет.\n";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Локальные и облачные файлы, добавленные в структуру как ссылки. Используй их как материал для ответа, если вопрос пользователя касается файлов. Если ниже есть блок 'Содержимое файла', файл уже прочитан и его можно обсуждать. Их можно связывать с папками, заметками и другими файлами через CREATE_CONNECTION.\n");
        String query = userQuery == null ? "" : userQuery.toLowerCase(Locale.ROOT);
        if (!query.trim().isEmpty()) {
            List<com.mindscape.app.files.FileSearchIndex.IndexedSnippet> snippets = fileSearchIndex().searchRelevantContent(query, 6);
            if (!snippets.isEmpty()) {
                sb.append("\nРелевантные фрагменты из SQLite-индекса заметок и файлов:\n");
                int used = 0;
                java.util.Set<String> seenSnippetTexts = new java.util.HashSet<>();
                for (com.mindscape.app.files.FileSearchIndex.IndexedSnippet snippet : snippets) {
                    if (used >= 24000) break;
                    String content = snippet.content == null ? "" : snippet.content;
                    int max = Math.min(content.length(), 4000);
                    String contentFingerprint = content.substring(0, max).trim();
                    if (!seenSnippetTexts.add(contentFingerprint)) continue;
                    used += max;
                    String safeContent = sanitizeAiContextText(content.substring(0, max));
                    sb.append("- ").append(snippet.title)
                            .append(" [").append(snippet.kind).append(snippet.folder == null || snippet.folder.isEmpty() ? "" : ", " + snippet.folder).append("]\n")
                            .append(safeContent)
                            .append("\n---\n");
                }
            }
        }
        boolean broadFileRequest = query.contains("файл") || query.contains("документ") || query.contains("file") || query.contains("document")
                || query.contains("прочитай") || query.contains("читай") || query.contains("read")
                || query.contains("содержимое") || query.contains("содержание") || query.contains("content")
                || query.contains("обсуди") || query.contains("разбери") || query.contains("проанализируй") || query.contains("анализ")
                || query.contains("его") || query.contains("нем") || query.contains("нём") || query.contains("ней") || query.contains("них")
                || query.contains("этот") || query.contains("дальше") || query.contains("подробнее") || query.contains("this") || query.contains("it")
                || query.contains("что в") || query.contains("what") || query.contains("открой") || query.contains("open")
                || query.contains("pdf") || query.contains("пдф") || query.contains("doc") || query.contains("xls") || query.contains("ppt")
                || query.contains("txt") || query.contains("rtf") || query.contains("csv") || query.contains("json") || query.contains("xml") || query.contains("код");
        int totalChars = 0;
        int contentBudget = broadFileRequest ? 96000 : 48000;
        java.util.Set<String> includedContentUris = new java.util.HashSet<>();
        for (int i = 0; i < localFilesList.size(); i++) {
            LocalFileLink file = localFilesList.get(i);
            String titleLower = file.title == null ? "" : file.title.toLowerCase(Locale.ROOT);
            sb.append(i + 1).append(". ").append(file.title)
                    .append(" [").append(file.displayFolder()).append("]")
                    .append(" type=").append(fileMimeLabel(file))
                    .append(", size=").append(formatFileSize(file.size))
                    .append("\n");
            boolean includeContent = isAiReadableFile(file) && totalChars < contentBudget && (broadFileRequest || (!query.isEmpty() && (query.contains(titleLower) || titleLower.contains(query))));
            if (includeContent) {
                String contentKey = file.uri == null || file.uri.isEmpty() ? file.nodeId() : file.uri;
                if (!includedContentUris.add(contentKey)) {
                    sb.append("Содержимое не повторяется: это ссылка на тот же файл, текст уже добавлен выше.\n");
                    continue;
                }
                int max = Math.min(broadFileRequest ? 32000 : 16000, contentBudget - totalChars);
                String text = readLocalFileText(file, max);
                if (text == null || text.trim().isEmpty()) {
                    sb.append("Содержимое файла ").append(file.title).append(" не извлечено: файл пустой, защищён или содержит только изображение/скан без OCR.\n---\n");
                } else {
                    totalChars += Math.min(text.length(), max);
                    sb.append("Содержимое файла ").append(file.title).append(":\n")
                            .append(sanitizeAiContextText(text))
                            .append("\n---\n");
                }
            } else if (!isAiReadableFile(file)) {
                sb.append("Содержимое не прочитано: формат пока недоступен для текстового извлечения, доступно открытие/просмотр через приложение.\n");
            }
        }
        return sb.toString();
    }

    private String sanitizeAiContextText(String text) {
        if (text == null || text.isEmpty()) return "";
        return text
                .replace("<CREATE_", "&lt;CREATE_")
                .replace("<DELETE_", "&lt;DELETE_")
                .replace("<MOVE_", "&lt;MOVE_")
                .replace("<COPY_", "&lt;COPY_")
                .replace("<HIDE_", "&lt;HIDE_")
                .replace("<SHOW_", "&lt;SHOW_")
                .replace("<create_", "&lt;create_")
                .replace("<delete_", "&lt;delete_")
                .replace("<move_", "&lt;move_")
                .replace("<copy_", "&lt;copy_")
                .replace("<hide_", "&lt;hide_")
                .replace("<show_", "&lt;show_");
    }

    public String fileCommandPrompt() {
        return "\n\nLOCAL FILE LINK COMMANDS:\n"
                + "- <CREATE_CONNECTION: SourceNameOrFileTitle | TargetNameOrFileTitle> — link any two nodes, including local files, notes, and folders.\n"
                + "- <DELETE_FILE_LINK: FileTitle> — remove only the file link from MindScape, never delete the real file.\n"
                + "- <MOVE_FILE_LINK: FileTitle | TargetFolderPath> — move the link to another folder. Use exact folder path, or none/null for no folder.\n"
                + "- <COPY_FILE_LINK: FileTitle | TargetFolderPath> — create another link to the same local file in another folder.\n"
                + "For local files, these commands manipulate only MindScape links/symlinks, not the original files.\n";
    }

    public void seedInitialData() {
        com.mindscape.app.data.PersistenceManager.seedInitialData(this);
    }

    public List<String> getAllCategoryPaths() {
        return KnowledgeTreeService.getAllCategoryPaths(categoriesList);
    }

    public List<Note> unboundNotes() {
        return KnowledgeTreeService.unboundNotes(notesList);
    }

    public String ruRootLabel() {
        return str(R.string.str_personal_center);
    }

    /** Все центры (корневые категории с флагом isCenter) в порядке создания. */
    public List<Category> getCenters() {
        return com.mindscape.app.tree.TreeOps.getCenters(categoriesList);
    }

    /** Top-level категории, не являющиеся центрами (legacy/свободные). */
    public List<Category> getLegacyTopLevel() {
        return com.mindscape.app.tree.TreeOps.getLegacyTopLevel(categoriesList);
    }

    /** Переместить центр вверх (dir=-1) или вниз (dir=+1) в иерархии центров.
     *  Порядок в categoriesList определяет порядок отображения и главный центр (первый). */
    public void moveCenter(Category center, int dir) {
        com.mindscape.app.tree.TreeOps.moveCenter(this, center, dir);
    }

    public CatTreeNode buildCategoryTree() {
        return com.mindscape.app.tree.TreeOps.buildCategoryTree(categoriesList, ROOT_NODE_TITLE, ROOT_GROUP);
    }

    public int dp(float value) {
        return UiKit.dp(this, value);
    }

    public int dp(int value) {
        return UiKit.dp(this, value);
    }

    public int statusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return resourceId > 0 ? getResources().getDimensionPixelSize(resourceId) : 0;
    }

    public int navigationBarHeight() {
        int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return resourceId > 0 ? getResources().getDimensionPixelSize(resourceId) : 0;
    }

    // --- LOCALIZATION HELPERS ---

    /** Rebuilds the localized context whenever activeLanguage changes. */
    public void updateLocalizedContext() {
        java.util.Locale locale = new java.util.Locale(activeLanguage);
        java.util.Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration(getResources().getConfiguration());
        config.setLocale(locale);
        localizedContext = createConfigurationContext(config);
    }

    /** Returns string in activeLanguage regardless of system locale. */
    public String str(int resId) {
        if (localizedContext != null) return localizedContext.getString(resId);
        return getString(resId);
    }

    /** Returns formatted string in activeLanguage regardless of system locale. */
    public String str(int resId, Object... args) {
        if (localizedContext != null) return localizedContext.getString(resId, args);
        return getString(resId, args);
    }


    // --- INPUT SANITIZATION ---
    public String sanitizeInput(String input, int maxLength) {
        return com.mindscape.app.util.Texts.sanitizeInput(input, maxLength);
    }

}
