package com.mindscape.app.screens;

import android.graphics.Color;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.mindscape.app.MainActivity;
import com.mindscape.app.R;

public final class HomeScreens {
    private HomeScreens() {}

    public static View homeScreen(MainActivity host) {
        boolean ru = "ru".equals(host.activeLanguage());
        FrameLayout frame = new FrameLayout(host);
        ScrollView scroll = new ScrollView(host);
        scroll.setClipToPadding(false);
        int baseScrollBottomPadding = host.dp(28);
        scroll.setPadding(0, 0, 0, baseScrollBottomPadding);
        LinearLayout body = new LinearLayout(host);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(host.dp(18), host.dp(8), host.dp(18), host.dp(18));
        scroll.addView(body);
        host.addHomeHeader(body);
        frame.addView(scroll);

        LinearLayout hero = host.card(host.surface());

        FrameLayout miniContainer = new FrameLayout(host);
        miniContainer.setBackground(host.roundedBg(host.softSurface(), 16, 0, Color.TRANSPARENT));
        miniContainer.setClipToOutline(true);

        if (host.miniMapWebView() == null) {
            WebView webView = new WebView(host);
            host.miniMapWebView(webView);
            WebSettings mws = webView.getSettings();
            mws.setJavaScriptEnabled(true);
            mws.setAllowFileAccess(false);
            mws.setAllowContentAccess(false);
            mws.setAllowFileAccessFromFileURLs(false);
            mws.setAllowUniversalAccessFromFileURLs(false);
            mws.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                mws.setSafeBrowsingEnabled(true);
            }
            webView.addJavascriptInterface(new Object() {
                @android.webkit.JavascriptInterface
                public void onReady() {
                    String base64 = android.util.Base64.encodeToString(host.buildGraphJson().getBytes(java.nio.charset.StandardCharsets.UTF_8), android.util.Base64.NO_WRAP);
                    host.runOnUiThread(() -> {
                        webView.evaluateJavascript("setGraphData(decodeURIComponent(escape(window.atob('" + base64 + "')))); setDarkMode(" + host.isDarkTheme() + ");", null);
                        webView.evaluateJavascript("centerMiniMap()", null);
                    });
                }
                @android.webkit.JavascriptInterface
                public void onNodeClick(String title) {}
            }, "AndroidBridge");
            webView.setBackgroundColor(Color.TRANSPARENT);
            webView.setVerticalScrollBarEnabled(false);
            webView.setHorizontalScrollBarEnabled(false);
            webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            webView.loadUrl("file:///android_asset/graph.html");
            webView.setOnTouchListener((v, event) -> true);
        } else {
            String base64 = android.util.Base64.encodeToString(host.buildGraphJson().getBytes(java.nio.charset.StandardCharsets.UTF_8), android.util.Base64.NO_WRAP);
            host.miniMapWebView().evaluateJavascript("setGraphData(decodeURIComponent(escape(window.atob('" + base64 + "')))); setDarkMode(" + host.isDarkTheme() + ");", null);
            host.miniMapWebView().evaluateJavascript("centerMiniMap()", null);
        }

        if (host.miniMapWebView().getParent() != null) {
            ((ViewGroup) host.miniMapWebView().getParent()).removeView(host.miniMapWebView());
        }

        FrameLayout.LayoutParams miniParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, host.dp(150));
        miniParams.setMargins(0, 0, host.dp(-2), 0);
        miniContainer.addView(host.miniMapWebView(), miniParams);

        View interceptor = new View(host);
        interceptor.setBackgroundColor(Color.TRANSPARENT);
        miniContainer.addView(interceptor, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        hero.addView(miniContainer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, host.dp(150)));

        LinearLayout heroLine = new LinearLayout(host);
        heroLine.setGravity(Gravity.CENTER_VERTICAL);
        heroLine.setPadding(host.dp(4), host.dp(8), host.dp(4), host.dp(4));
        LinearLayout heroText = new LinearLayout(host);
        heroText.setOrientation(LinearLayout.VERTICAL);

        heroText.addView(host.text(host.str(R.string.str_main_map), 17, host.primaryText(), true));
        heroLine.addView(heroText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ImageView openMap = host.iconAction("ic_chevron_right", host.secondaryText());
        heroLine.addView(openMap, new LinearLayout.LayoutParams(host.dp(44), host.dp(44)));
        hero.addView(heroLine);
        hero.setOnClickListener(v -> {
            host.selectedSection("Карта");
            host.renderContent();
            host.updateBottomNav();
        });
        body.addView(hero);

        LinearLayout statsRow = new LinearLayout(host);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setWeightSum(2f);
        LinearLayout.LayoutParams statsParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statsParams.setMargins(0, host.dp(12), 0, host.dp(12));
        statsRow.setLayoutParams(statsParams);

        LinearLayout nodesCard = host.card(host.surface());
        nodesCard.setBackground(host.roundedBg(host.surface(), 16, 1, host.strokeColor()));
        nodesCard.setPadding(host.dp(16), host.dp(9), host.dp(16), host.dp(9));
        nodesCard.setOrientation(LinearLayout.VERTICAL);
        nodesCard.setGravity(Gravity.CENTER_VERTICAL);
        nodesCard.addView(host.text(host.str(R.string.str_graph_nodes), 12, host.secondaryText(), false));
        nodesCard.addView(host.text(String.valueOf(host.categoriesList().size() + host.notesList().size()), 24, host.primaryText(), true));
        LinearLayout.LayoutParams nodeCardParams = new LinearLayout.LayoutParams(0, host.dp(72), 1f);
        nodeCardParams.setMargins(0, 0, host.dp(6), 0);
        statsRow.addView(nodesCard, nodeCardParams);

        LinearLayout aiCard = host.card(host.surface());
        aiCard.setBackground(host.roundedBg(host.surface(), 16, 1, host.strokeColor()));
        aiCard.setPadding(host.dp(14), host.dp(9), host.dp(14), host.dp(9));
        aiCard.setOrientation(LinearLayout.HORIZONTAL);
        aiCard.setGravity(Gravity.CENTER_VERTICAL);
        ImageView aiIcon = new ImageView(host);
        aiIcon.setImageResource(host.getResources().getIdentifier("ic_ai", "drawable", host.getPackageName()));
        aiIcon.setColorFilter(host.accentColor());
        aiIcon.setPadding(host.dp(7), host.dp(7), host.dp(7), host.dp(7));
        aiIcon.setBackground(host.roundedBg(host.isDarkTheme() ? Color.rgb(45, 54, 68) : host.softSurface(), 12, 0, Color.TRANSPARENT));
        aiCard.addView(aiIcon, new LinearLayout.LayoutParams(host.dp(34), host.dp(34)));
        LinearLayout aiText = new LinearLayout(host);
        aiText.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams aiTextParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        aiTextParams.setMargins(host.dp(10), 0, 0, 0);
        aiText.addView(host.text(host.str(R.string.str_ai_assistant), 13, host.primaryText(), true));
        aiText.addView(host.text(host.str(R.string.str_ask_assistant), 11, host.secondaryText(), false));
        aiCard.addView(aiText, aiTextParams);
        aiCard.setOnClickListener(v -> {
            host.selectedSection("AI");
            host.renderContent();
            host.updateBottomNav();
        });
        LinearLayout.LayoutParams aiCardParams = new LinearLayout.LayoutParams(0, host.dp(72), 1f);
        aiCardParams.setMargins(host.dp(6), 0, 0, 0);
        statsRow.addView(aiCard, aiCardParams);
        body.addView(statsRow);

        LinearLayout aiToolsCard = host.card(host.surface());
        aiToolsCard.setPadding(host.dp(16), host.dp(14), host.dp(16), host.dp(14));
        aiToolsCard.setOrientation(LinearLayout.HORIZONTAL);
        aiToolsCard.setGravity(Gravity.CENTER_VERTICAL);
        ImageView toolsIcon = new ImageView(host);
        toolsIcon.setImageResource(host.getResources().getIdentifier("ic_ai_tools", "drawable", host.getPackageName()));
        toolsIcon.setColorFilter(host.accentColor());
        toolsIcon.setPadding(host.dp(8), host.dp(8), host.dp(8), host.dp(8));
        toolsIcon.setBackground(host.roundedBg(host.isDarkTheme() ? Color.rgb(45, 54, 68) : host.softSurface(), 14, 0, Color.TRANSPARENT));
        aiToolsCard.addView(toolsIcon, new LinearLayout.LayoutParams(host.dp(42), host.dp(42)));
        LinearLayout toolsText = new LinearLayout(host);
        toolsText.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams toolsTextParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        toolsTextParams.setMargins(host.dp(12), 0, host.dp(10), 0);
        toolsText.addView(host.text(ru ? "ИИ-инструменты" : "AI tools", 16, host.primaryText(), true));
        toolsText.addView(host.text(ru ? "Транскрибация аудио и OCR изображений" : "Audio transcription and image OCR", 12, host.secondaryText(), false));
        aiToolsCard.addView(toolsText, toolsTextParams);
        ImageView toolsChevron = host.iconAction("ic_chevron_right", host.secondaryText());
        aiToolsCard.addView(toolsChevron, new LinearLayout.LayoutParams(host.dp(40), host.dp(40)));
        aiToolsCard.setOnClickListener(v -> {
            host.selectedSection("AI_TOOLS");
            host.renderContent();
            host.updateBottomNav();
        });
        LinearLayout.LayoutParams toolsParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        toolsParams.setMargins(0, 0, 0, host.dp(12));
        body.addView(aiToolsCard, toolsParams);

        LinearLayout quickNote = host.card(host.surface());
        quickNote.setPadding(host.dp(16), host.dp(14), host.dp(16), host.dp(14));
        quickNote.setOrientation(LinearLayout.VERTICAL);
        quickNote.addView(host.text(ru ? "Создать заметку" : "Create note", 17, host.primaryText(), true));
        quickNote.addView(host.spacer(10));
        EditText quickTitle = host.createStyledInput("", ru ? "Название" : "Title");
        quickTitle.setSingleLine(true);
        quickNote.addView(quickTitle);
        quickNote.addView(host.spacer(8));

        FrameLayout contentBox = new FrameLayout(host);
        EditText quickContent = host.createStyledInput("", host.str(R.string.str_description));
        host.configureLargeTextInput(quickContent, 126);
        quickContent.setPadding(host.dp(14), host.dp(10), host.dp(54), host.dp(42));
        contentBox.addView(quickContent, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ImageView saveQuick = new ImageView(host);
        saveQuick.setImageResource(host.getResources().getIdentifier("ic_save", "drawable", host.getPackageName()));
        saveQuick.setColorFilter(host.accentColor());
        saveQuick.setContentDescription(host.str(R.string.str_save));
        saveQuick.setPadding(host.dp(9), host.dp(9), host.dp(9), host.dp(9));
        saveQuick.setBackground(host.roundedBg(host.isDarkTheme() ? Color.rgb(45, 54, 68) : host.softSurface(), 14, 1, host.strokeColor()));
        saveQuick.setOnClickListener(v -> host.createQuickContainerNote(quickTitle, quickContent));
        FrameLayout.LayoutParams saveParams = new FrameLayout.LayoutParams(host.dp(42), host.dp(42), Gravity.BOTTOM | Gravity.RIGHT);
        saveParams.setMargins(0, 0, host.dp(8), host.dp(8));
        contentBox.addView(saveQuick, saveParams);
        quickNote.addView(contentBox);
        body.addView(quickNote);

        View.OnFocusChangeListener quickFocusListener = (v, hasFocus) -> {
            if (hasFocus) {
                scroll.postDelayed(() -> scroll.smoothScrollTo(0, quickNote.getBottom()), 220);
            }
        };
        quickTitle.setOnFocusChangeListener(quickFocusListener);
        quickContent.setOnFocusChangeListener(quickFocusListener);
        frame.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            View decor = host.getWindow().getDecorView();
            Rect visibleFrame = new Rect();
            decor.getWindowVisibleDisplayFrame(visibleFrame);
            int screenHeight = decor.getRootView().getHeight();
            int keyboardHeight = screenHeight - visibleFrame.bottom;
            boolean keyboardOpen = keyboardHeight > screenHeight * 0.15f;
            if (keyboardOpen) {
                scroll.setPadding(0, 0, 0, keyboardHeight + host.dp(24));
                if (quickTitle.hasFocus() || quickContent.hasFocus()) {
                    scroll.post(() -> scroll.smoothScrollTo(0, quickNote.getBottom()));
                }
            } else if (scroll.getPaddingBottom() != baseScrollBottomPadding) {
                scroll.setPadding(0, 0, 0, baseScrollBottomPadding);
            }
        });
        return frame;
    }
}
