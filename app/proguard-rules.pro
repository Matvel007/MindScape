-dontwarn javax.annotation.concurrent.GuardedBy
-dontwarn javax.annotation.Nullable
-dontwarn com.gemalto.jp2.JP2Decoder

# Keep WebView JavaScript interfaces
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep data model classes
-keep class com.mindscape.app.MainActivity$Note { *; }
-keep class com.mindscape.app.MainActivity$Category { *; }
-keep class com.mindscape.app.MainActivity$Connection { *; }
-keep class com.mindscape.app.MainActivity$KnowledgeNode { *; }
-keep class com.mindscape.app.MainActivity$ChatMessage { *; }
-keep class com.mindscape.app.MainActivity$ChatSession { *; }

# Remove all Log calls in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# EncryptedSharedPreferences
-keepclassmembers class * extends androidx.security.crypto.EncryptedSharedPreferences {
    <init>(...);
}

# Obfuscation
-repackageclasses 'com.mindscape.core'
-allowaccessmodification
-optimizationpasses 5
