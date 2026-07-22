package com.mindscape.app.screens;

import android.graphics.Color;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.text.TextUtils;

import com.mindscape.app.Category;
import com.mindscape.app.Connection;
import com.mindscape.app.LocalFileLink;

import java.util.Locale;
import com.mindscape.app.MainActivity;
import com.mindscape.app.Note;
import com.mindscape.app.R;
import com.mindscape.app.graph.GraphBridge;

public final class MapScreens {
    private MapScreens() {}

    public static View mapScreen(MainActivity host) {
        // The map intentionally continues behind the floating navigation panel.
        host.contentHost.setPadding(0, 0, 0, 0);
        LinearLayout screen = new LinearLayout(host);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(Color.TRANSPARENT);

        LinearLayout mapHeader = new LinearLayout(host);
        mapHeader.setOrientation(LinearLayout.HORIZONTAL);
        mapHeader.setGravity(Gravity.CENTER_VERTICAL);
        mapHeader.setPadding(host.dp(14), host.dp(10), host.dp(10), host.dp(8));
        mapHeader.setBackgroundColor(host.isDarkTheme() ? Color.rgb(20, 24, 33) : host.surface());

        Object selected = host.selectedMapEntity();
        String mapTitle = host.str(R.string.str_knowledge_map);
        if (selected instanceof Category) mapTitle = ((Category) selected).title;
        else if (selected instanceof Note) mapTitle = ((Note) selected).deepestCategory();
        else if (selected instanceof LocalFileLink) mapTitle = ((LocalFileLink) selected).title;

        if (selected != null) {
            android.widget.ImageView backBtn = host.iconAction("ic_arrow_back", Color.rgb(65, 120, 220));
            backBtn.setOnClickListener(v -> {
                host.selectedMapEntity(null);
                host.renderContent();
            });
            mapHeader.addView(backBtn);
        }
        TextView titleView = host.text(mapTitle, 18, host.primaryText(), true);
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        mapHeader.addView(titleView,
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageView zoomInBtn = mapControlButton(host, "ic_zoom_in");
        zoomInBtn.setOnClickListener(v -> {
            if (host.mapWebView() != null) host.mapWebView().evaluateJavascript("zoomIn()", null);
        });
        mapHeader.addView(zoomInBtn, mapControlParams(host));

        ImageView zoomOutBtn = mapControlButton(host, "ic_zoom_out");
        zoomOutBtn.setOnClickListener(v -> {
            if (host.mapWebView() != null) host.mapWebView().evaluateJavascript("zoomOut()", null);
        });
        mapHeader.addView(zoomOutBtn, mapControlParams(host));

        ImageView resetBtn = mapControlButton(host, "ic_reset_view");
        resetBtn.setOnClickListener(v -> {
            if (host.mapWebView() != null) host.mapWebView().evaluateJavascript("resetView()", null);
        });
        mapHeader.addView(resetBtn, mapControlParams(host));
        screen.addView(mapHeader);

        if (host.mapWebView() == null) {
            WebView webView = new WebView(host);
            host.mapWebView(webView);
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setAllowFileAccess(false);
            settings.setAllowContentAccess(false);
            settings.setAllowFileAccessFromFileURLs(false);
            settings.setAllowUniversalAccessFromFileURLs(false);
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                settings.setSafeBrowsingEnabled(true);
            }
            webView.addJavascriptInterface(new GraphBridge(host, host.notesList(), host.categoriesList(), host.localFilesList()), "AndroidBridge");
            webView.loadUrl("file:///android_asset/graph.html");
        }
        if (host.mapWebView().getParent() != null) {
            ((ViewGroup) host.mapWebView().getParent()).removeView(host.mapWebView());
        }
        FrameLayout mapLayer = new FrameLayout(host);
        screen.addView(mapLayer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        mapLayer.addView(host.mapWebView(), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (host.mapWebView() != null) {
            String base64 = android.util.Base64.encodeToString(host.buildGraphJson().getBytes(java.nio.charset.StandardCharsets.UTF_8), android.util.Base64.NO_WRAP);
            host.mapWebView().evaluateJavascript(host.graphOptionsJs() + "if(typeof setGraphData!=='undefined'){setGraphData(decodeURIComponent(escape(window.atob('" + base64 + "'))))}", null);
            host.mapWebView().evaluateJavascript("if(typeof setDarkMode!=='undefined'){setDarkMode(" + host.isDarkTheme() + ")}", null);

            if (selected != null) {
                String id = "";
                if (selected instanceof Note) id = "note:" + ((Note) selected).fullPath();
                else if (selected instanceof Category) id = "folder:" + ((Category) selected).fullPath();
                else if (selected instanceof LocalFileLink) id = ((LocalFileLink) selected).nodeId();
                host.mapWebView().evaluateJavascript("if(typeof centerNode!=='undefined'){centerNode('" + id.replace("\\", "\\\\").replace("'", "\\'") + "')}", null);
            } else {
                host.mapWebView().evaluateJavascript("if(typeof centerMainCenter!=='undefined'){centerMainCenter()}", null);
            }
        }

        if (selected != null) {
            LinearLayout detailCard = host.card(host.surface());
            detailCard.setOrientation(LinearLayout.VERTICAL);
            detailCard.setPadding(host.dp(16), host.dp(14), host.dp(16), host.dp(14));
            LinearLayout headerRow = new LinearLayout(host);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout titleGroup = new LinearLayout(host);
            titleGroup.setOrientation(LinearLayout.VERTICAL);

            if (selected instanceof Note) {
                Note note = (Note) selected;
                titleGroup.addView(host.text(note.title, 18, host.primaryText(), true));
                titleGroup.addView(host.chip(note.displayCategory()));
                headerRow.addView(titleGroup, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                boolean noteHidden = host.isNodeHidden(note);
                android.widget.ImageView eyeBtn = host.iconAction(noteHidden ? "ic_eye_off" : "ic_eye",
                        noteHidden ? Color.rgb(140, 150, 165) : Color.rgb(65, 120, 220));
                eyeBtn.setOnClickListener(v -> {
                    host.toggleNodeHidden(note);
                    host.pushGraphDataToWebView();
                    host.renderContent();
                });
                headerRow.addView(eyeBtn);
                android.widget.ImageView closeBtn = host.iconAction("ic_close", host.secondaryText());
                closeBtn.setOnClickListener(v -> {
                    host.selectedMapEntity(null);
                    host.renderContent();
                });
                headerRow.addView(closeBtn);
                detailCard.addView(headerRow);
                detailCard.addView(scrollableTextSection(host, host.str(R.string.str_note_text), note.content, host.dp(96)));

                String noteKeyPref = "note:" + note.fullPath();
                String noteKeyLegacy = note.fullPath();
                StringBuilder rels = new StringBuilder();
                for (Connection conn : host.connectionsList()) {
                    boolean srcIsThis = conn.source.equalsIgnoreCase(noteKeyPref) || conn.source.equalsIgnoreCase(noteKeyLegacy);
                    boolean tgtIsThis = conn.target.equalsIgnoreCase(noteKeyPref) || conn.target.equalsIgnoreCase(noteKeyLegacy);
                    if (srcIsThis || tgtIsThis) {
                        String cleanOther = host.cleanNodeId(srcIsThis ? conn.target : conn.source);
                        String displayName = cleanOther.contains("/") ? cleanOther.substring(cleanOther.lastIndexOf("/") + 1) : cleanOther;
                        if (rels.length() > 0) rels.append(" • ");
                        rels.append(displayName);
                    }
                }
                if (rels.length() > 0) detailCard.addView(scrollableTextSection(host, host.str(R.string.str_connected_with), rels.toString(), host.dp(70)));
                LinearLayout actionRow = new LinearLayout(host);
                actionRow.setPadding(0, host.dp(10), 0, 0);
                Button editBtn = host.createStyledButton(host.str(R.string.str_edit));
                editBtn.setOnClickListener(v -> host.editNoteDialog(note));
                actionRow.addView(editBtn, new LinearLayout.LayoutParams(0, host.dp(50), 1f));
                detailCard.addView(actionRow);
            } else if (selected instanceof Category) {
                Category cat = (Category) selected;
                String catPath = cat.fullPath();
                int subNotesCount = 0;
                for (Note n : host.notesList()) {
                    if (n.isUnbound() || n.categoryPath == null) continue;
                    if (n.categoryPath.equals(catPath) || n.categoryPath.startsWith(catPath + "/")) subNotesCount++;
                }
                titleGroup.addView(host.text(cat.title, 18, host.primaryText(), true));
                titleGroup.addView(host.text(("ru".equals(host.activeLanguage()) ? subNotesCount + " заметок" : subNotesCount + " notes"), 12, host.secondaryText(), false));
                headerRow.addView(titleGroup, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                boolean catHidden = host.isNodeHidden(cat);
                android.widget.ImageView eyeBtn2 = host.iconAction(catHidden ? "ic_eye_off" : "ic_eye",
                        catHidden ? Color.rgb(140, 150, 165) : Color.rgb(65, 120, 220));
                eyeBtn2.setOnClickListener(v -> {
                    host.toggleNodeHidden(cat);
                    host.pushGraphDataToWebView();
                    host.renderContent();
                });
                headerRow.addView(eyeBtn2);
                android.widget.ImageView editIcon2 = host.iconAction("ic_wrench", Color.rgb(65, 120, 220));
                editIcon2.setOnClickListener(v -> host.showRenameFolderDialog(cat.fullPath()));
                headerRow.addView(editIcon2);
                android.widget.ImageView closeBtn2 = host.iconAction("ic_close", host.secondaryText());
                closeBtn2.setOnClickListener(v -> {
                    host.selectedMapEntity(null);
                    host.renderContent();
                });
                headerRow.addView(closeBtn2);
                detailCard.addView(headerRow);
                detailCard.addView(scrollableTextSection(host, host.str(R.string.str_description), cat.description, host.dp(120)));
            } else if (selected instanceof LocalFileLink) {
                LocalFileLink file = (LocalFileLink) selected;
                titleGroup.addView(host.text(file.title, 18, host.primaryText(), true));
                titleGroup.addView(host.chip(file.displayFolder()));
                headerRow.addView(host.iconAction(host.fileIconName(file), host.fileIconColor(file)), new LinearLayout.LayoutParams(host.dp(34), host.dp(34)));
                headerRow.addView(titleGroup, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                boolean fileHidden = host.isNodeHidden(file);
                android.widget.ImageView eyeBtn = host.iconAction(fileHidden ? "ic_eye_off" : "ic_eye",
                        fileHidden ? Color.rgb(140, 150, 165) : Color.rgb(65, 120, 220));
                eyeBtn.setOnClickListener(v -> {
                    host.toggleNodeHidden(file);
                    host.pushGraphDataToWebView();
                    host.renderContent();
                });
                headerRow.addView(eyeBtn);
                android.widget.ImageView closeBtn = host.iconAction("ic_close", host.secondaryText());
                closeBtn.setOnClickListener(v -> {
                    host.selectedMapEntity(null);
                    host.renderContent();
                });
                headerRow.addView(closeBtn);
                detailCard.addView(headerRow);
                if (host.isImageFile(file)) {
                    ImageView image = new ImageView(host);
                    image.setAdjustViewBounds(false);
                    image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    image.setImageURI(Uri.parse(file.uri));
                    LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
                    imgParams.setMargins(0, host.dp(8), 0, host.dp(6));
                    detailCard.addView(image, imgParams);
                } else if (host.isAiReadableFile(file)) {
                    TextView preview = host.text("", 13, host.secondaryText(), false);
                    preview.setText("ru".equals(host.activeLanguage()) ? "Загружаем предпросмотр..." : "Loading preview...");
                    ScrollView previewScroll = new ScrollView(host);
                    previewScroll.setFillViewport(false);
                    previewScroll.addView(preview, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
                    previewParams.setMargins(0, host.dp(8), 0, host.dp(6));
                    detailCard.addView(previewScroll, previewParams);
                    new Thread(() -> {
                        String text = host.readLocalFileText(file, 5000);
                        host.runOnUiThread(() -> preview.setText(text == null || text.trim().isEmpty()
                                ? ("ru".equals(host.activeLanguage()) ? "Предпросмотр недоступен." : "Preview unavailable.")
                                : text));
                    }, "MindScapeMapFilePreview").start();
                }
                detailCard.addView(compactDescription(host, host.fileMimeLabel(file) + " · " + host.formatFileSize(file.size)));

                LinearLayout actionRow = new LinearLayout(host);
                actionRow.setPadding(0, host.dp(10), 0, 0);
                if (!isApk(file)) {
                    Button openBtn = host.createStyledButton(host.str(R.string.str_open_system_viewer));
                    openBtn.setOnClickListener(v -> host.showLocalFileDialog(file));
                    actionRow.addView(openBtn, new LinearLayout.LayoutParams(0, host.dp(52), 1f));
                }
                detailCard.addView(actionRow);
            }

            int panelHeight = Math.min(host.dp(360), Math.max(host.dp(280), host.getResources().getDisplayMetrics().heightPixels / 3));
            FrameLayout.LayoutParams detailParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, panelHeight, Gravity.BOTTOM);
            detailParams.setMargins(host.dp(16), host.dp(8), host.dp(16), host.dp(8));
            mapLayer.addView(detailCard, detailParams);
            mapLayer.post(() -> {
                if (host.bottomNav == null || host.bottomNav.getVisibility() != View.VISIBLE) return;
                int[] mapLocation = new int[2];
                int[] navLocation = new int[2];
                mapLayer.getLocationOnScreen(mapLocation);
                host.bottomNav.getLocationOnScreen(navLocation);
                int reserve = Math.max(host.dp(8), mapLocation[1] + mapLayer.getHeight() - navLocation[1] + host.dp(8));
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) detailCard.getLayoutParams();
                if (params.bottomMargin != reserve) {
                    params.bottomMargin = reserve;
                    detailCard.setLayoutParams(params);
                }
            });
        }
        return screen;
    }

    private static View compactDescription(MainActivity host, String text) {
        TextView view = host.text(text, 13, host.secondaryText(), false);
        view.setSingleLine(true);
        view.setEllipsize(TextUtils.TruncateAt.END);
        return view;
    }

    private static View scrollableTextSection(MainActivity host, String title, String body, int maxHeight) {
        LinearLayout section = new LinearLayout(host);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(0, host.dp(8), 0, 0);
        section.addView(host.text(title, 13, host.secondaryText(), false));
        TextView text = host.text(body == null || body.trim().isEmpty() ? "—" : body, 14, host.primaryText(), false);
        ScrollView scroll = new ScrollView(host);
        scroll.addView(text, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maxHeight);
        params.setMargins(0, host.dp(4), 0, 0);
        section.addView(scroll, params);
        return section;
    }

    private static ImageView mapControlButton(MainActivity host, String iconName) {
        ImageView button = host.iconAction(iconName, host.primaryText());
        button.setBackground(host.roundedBg(host.isDarkTheme() ? Color.rgb(30, 36, 50) : host.surface(), 8, 1, host.strokeColor()));
        button.setPadding(host.dp(9), host.dp(9), host.dp(9), host.dp(9));
        return button;
    }

    private static LinearLayout.LayoutParams mapControlParams(MainActivity host) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(host.dp(42), host.dp(42));
        params.setMargins(host.dp(4), 0, 0, 0);
        return params;
    }

    private static boolean isApk(com.mindscape.app.LocalFileLink file) {
        return "application/vnd.android.package-archive".equals(file.mimeType)
                || (file.title != null && file.title.toLowerCase(java.util.Locale.ROOT).endsWith(".apk"));
    }

}
