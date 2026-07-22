package com.mindscape.app.files;

import android.content.Intent;
import android.content.ClipData;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.mindscape.app.MainActivity;
import com.mindscape.app.R;
import com.mindscape.app.LocalFileLink;
import com.mindscape.app.data.MindScapeArchive;

import java.io.File;
import java.io.FileOutputStream;

public class LocalFileOps {

    public static void addLocalFileLink(MainActivity host, Intent data) {
        Uri uri = data.getData();
        if (uri == null) return;
        int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if ((flags & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION)) == 0) {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
        }
        try {
            host.getContentResolver().takePersistableUriPermission(uri, flags);
        } catch (Exception error) {
            android.util.Log.w("MindScape", "Unable to persist file URI permission", error);
        }

        String title = host.queryDisplayName(uri);
        String mime = host.getContentResolver().getType(uri);
        long size = host.queryFileSize(uri);
        LocalFileLink link = new LocalFileLink(
                title == null || title.trim().isEmpty() ? uri.getLastPathSegment() : title,
                host.pendingLocalFileFolderPath,
                uri.toString(),
                mime == null ? "" : mime,
                size,
                System.currentTimeMillis()
        );
        host.localFilesList.add(link);
        host.pendingLocalFileFolderPath = null;
        host.savePersistentData();
        host.reindexFileAsync(link);
        host.pushGraphDataToWebView();
        host.renderContent();
        Toast.makeText(host, host.str(R.string.str_local_file_added), Toast.LENGTH_SHORT).show();
    }

    public static void openLocalFileExternal(MainActivity host, LocalFileLink file) {
        if (MindScapeArchive.isSqlarUri(file.uri)) {
            new Thread(() -> {
                try {
                    Uri uri = extractSqlarFileToCache(host, file);
                    Intent intent = viewIntent(host, file, uri);
                    host.runOnUiThread(() -> startViewer(host, file, intent));
                } catch (Exception e) {
                    host.runOnUiThread(() -> showOpenError(host, file, e));
                }
            }).start();
            return;
        }
        try {
            Uri uri = Uri.parse(file.uri);
            if ("file".equals(uri.getScheme())) {
                java.io.File local = new java.io.File(uri.getPath());
                if (!local.exists()) throw new java.io.FileNotFoundException(local.getAbsolutePath());
                uri = FileProvider.getUriForFile(host, host.getPackageName() + ".fileprovider", local);
            }
            Intent intent = viewIntent(host, file, uri);
            startViewer(host, file, intent);
        } catch (Exception e) {
            showOpenError(host, file, e);
        }
    }

    private static void startViewer(MainActivity host, LocalFileLink file, Intent intent) {
        try {
            host.startActivity(Intent.createChooser(intent, host.str(R.string.str_open_system_viewer)));
        } catch (Exception e) {
            showOpenError(host, file, e);
        }
    }

    private static void showOpenError(MainActivity host, LocalFileLink file, Throwable error) {
        Toast.makeText(host, host.userFriendlyError(error), Toast.LENGTH_LONG).show();
    }

    private static Intent viewIntent(MainActivity host, LocalFileLink file, Uri uri) {
        String mime = file.mimeType == null || file.mimeType.isEmpty() ? "*/*" : file.mimeType;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mime);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newUri(host.getContentResolver(), file.title, uri));
        return intent;
    }

    private static Uri extractSqlarFileToCache(MainActivity host, LocalFileLink file) throws Exception {
        String entry = MindScapeArchive.sqlarEntryName(file.uri);
        Uri archiveUri = MindScapeArchive.sqlarArchiveUri(file.uri);
        byte[] bytes = MindScapeArchive.readSqlarBytes(host, archiveUri, entry, MindScapeArchive.MAX_EMBEDDED_FILE_BYTES);
        if (bytes == null) throw new java.io.FileNotFoundException(entry);
        File dir = new File(host.getCacheDir(), "restored-files");
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("Unable to create restore cache");
        File outFile = new File(dir, safeFileName(file));
        try (FileOutputStream out = new FileOutputStream(outFile)) {
            out.write(bytes);
        }
        return FileProvider.getUriForFile(host, host.getPackageName() + ".fileprovider", outFile);
    }

    private static String safeFileName(LocalFileLink file) {
        String raw = file.title == null || file.title.trim().isEmpty() ? "restored-file" : file.title.trim();
        String cleaned = raw.replaceAll("[\\\\/\\p{Cntrl}]", "_").replace("..", "_");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        if (cleaned.isEmpty()) cleaned = "restored-file";
        return cleaned.length() > 120 ? cleaned.substring(0, 120) : cleaned;
    }

    public static boolean deletePhysicalFile(MainActivity host, LocalFileLink file) {
        try {
            // SAF/local links point to user-owned originals. MindScape must only remove the link.
            return true;
        } catch (Exception error) {
            android.util.Log.w("MindScape", "Unable to delete linked physical file", error);
            return false;
        }
    }

}
