package com.mindscape.app.files;

import android.graphics.Color;
import android.net.Uri;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.mindscape.app.LocalFileLink;
import com.mindscape.app.MainActivity;
import com.mindscape.app.R;

import java.util.Locale;

public final class LocalFileDialogs {
    private LocalFileDialogs() {}

    public static void showLocalFileDialog(MainActivity host, LocalFileLink file) {
        LinearLayout layout = new LinearLayout(host);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(host.dp(18), host.dp(14), host.dp(18), host.dp(8));
        final android.app.AlertDialog[] dialogRef = new android.app.AlertDialog[1];

        layout.addView(host.text(file.title, 18, host.primaryText(), true));
        layout.addView(host.text(file.displayFolder(), 12, host.secondaryText(), false));
        layout.addView(host.text(host.fileMimeLabel(file) + " · " + host.formatFileSize(file.size), 12, host.secondaryText(), false));
        layout.addView(host.spacer(10));

        Uri uri = Uri.parse(file.uri);
        if (host.isImageFile(file)) {
            android.widget.ImageView image = new android.widget.ImageView(host);
            image.setAdjustViewBounds(true);
            image.setMaxHeight(host.dp(240));
            image.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            image.setImageURI(uri);
            layout.addView(image, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        } else if (host.isTextReadable(file)) {
            TextView preview = host.text(host.readLocalFileText(file, 120000), 12, host.primaryText(), false);
            preview.setTextIsSelectable(true);
            ScrollView scroll = new ScrollView(host);
            scroll.setBackground(host.roundedBg(host.softSurface(), 12, 0, Color.TRANSPARENT));
            scroll.setPadding(host.dp(10), host.dp(10), host.dp(10), host.dp(10));
            scroll.addView(preview);
            layout.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, host.dp(320)));
        } else {
            layout.addView(host.text(host.str(R.string.str_local_file_binary_preview), 13, host.secondaryText(), false));
        }

        layout.addView(host.spacer(10));
        layout.addView(host.actionDialogRow(host.str(R.string.str_connections), host.str(R.string.str_link_with), "ic_node_link", Color.rgb(65, 120, 220), v -> {
            if (dialogRef[0] != null) dialogRef[0].dismiss();
            host.showConnectionsDialog(file.nodeId(), false);
        }));
        boolean hidden = host.isNodeHidden(file);
        layout.addView(host.actionDialogRow(
                hidden ? host.str(R.string.str_menu_show_on_map) : host.str(R.string.str_menu_hide_from_map),
                hidden ? host.str(R.string.str_menu_show_on_map_sub) : host.str(R.string.str_menu_hide_from_map_sub),
                hidden ? "ic_eye" : "ic_eye_off",
                v -> {
                    host.toggleNodeHidden(file);
                    host.pushGraphDataToWebView();
                    if (dialogRef[0] != null) dialogRef[0].dismiss();
                    host.renderContent();
                }));
        if (!isApk(file)) {
            layout.addView(host.actionDialogRow(host.str(R.string.str_open_system_viewer), host.fileMimeLabel(file), host.fileIconName(file), host.fileIconColor(file), v -> host.openLocalFileExternal(file)));
        }
        layout.addView(host.actionDialogRow(host.str(R.string.str_menu_move), host.str(R.string.str_file_move_sub), "ic_create_category", v -> {
            if (dialogRef[0] != null) dialogRef[0].dismiss();
            host.showMoveLocalFileDialog(file);
        }));
        layout.addView(host.actionDialogRow(host.str(R.string.str_menu_duplicate), host.str(R.string.str_file_duplicate_sub), "ic_create_template", v -> {
            host.duplicateLocalFileBeside(file);
            if (dialogRef[0] != null) dialogRef[0].dismiss();
            host.renderContent();
        }));
        layout.addView(host.actionDialogRow(host.str(R.string.str_remove_link), host.str(R.string.str_file_delete_sub), "ic_trash", v -> {
            if (dialogRef[0] != null) dialogRef[0].dismiss();
            host.confirmRemoveLocalFile(file);
        }));

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(host)
                .setView(host.scrollableDialogContent(layout, 620))
                .setNegativeButton(host.str(R.string.str_close), null)
                .create();
        dialogRef[0] = dialog;
        dialog.getWindow().setBackgroundDrawable(host.roundedBg(host.surface(), 16, 0, Color.TRANSPARENT));
        dialog.show();
    }

    private static boolean isApk(LocalFileLink file) {
        return "application/vnd.android.package-archive".equals(file.mimeType)
                || (file.title != null && file.title.toLowerCase(java.util.Locale.ROOT).endsWith(".apk"));
    }
}
