package com.mindscape.app.screens;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.text.TextUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.mindscape.app.MainActivity;
import com.mindscape.app.Note;
import com.mindscape.app.R;

public class CoreScreens {

    public static View buildUi(MainActivity host) {
        FrameLayout root = new FrameLayout(host);
        root.setBackgroundColor(host.appBg());
        root.setPadding(0, host.statusBarHeight(), 0, 0);

        final int[] appliedTopInset = new int[]{host.statusBarHeight()};
        final int[] appliedBottomMargin = new int[]{host.dp(16)};
        final int[] contentBottomPadding = new int[]{host.dp(76)};

        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            android.graphics.Rect r = new android.graphics.Rect();
            root.getWindowVisibleDisplayFrame(r);
            int screenHeight = root.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;
            boolean screenUsesFloatingNav = !"Карта".equals(host.selectedSection()) && !"AI".equals(host.selectedSection());
            int contentBottom = screenUsesFloatingNav ? contentBottomPadding[0] : 0;
            if (keypadHeight > screenHeight * 0.15) {
                if ("AI".equals(host.selectedSection())) contentBottom = keypadHeight;
                if (host.bottomNav != null) host.bottomNav.setVisibility(View.GONE);
            } else {
                if (host.bottomNav != null) host.bottomNav.setVisibility("AI".equals(host.selectedSection()) ? View.GONE : View.VISIBLE);
            }
            if (host.contentHost != null && host.contentHost.getPaddingBottom() != contentBottom) {
                host.contentHost.setPadding(0, 0, 0, contentBottom);
            }
        });

        host.contentHost = new android.widget.FrameLayout(host);
        root.addView(host.contentHost, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        host.bottomNav = new LinearLayout(host);
        host.bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        host.bottomNav.setGravity(Gravity.CENTER);
        host.bottomNav.setPadding(host.dp(10), host.dp(7), host.dp(10), host.dp(8));
        host.bottomNav.setBackground(host.roundedBg(host.surface(), 18, 1, host.strokeColor()));

        FrameLayout.LayoutParams bottomParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
        );
        bottomParams.setMargins(host.dp(16), 0, host.dp(16), host.dp(16));
        root.addView(host.bottomNav, bottomParams);

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets systemBars = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars());
            int topInset = Math.max(host.statusBarHeight(), systemBars.top);
            if (appliedTopInset[0] != topInset || root.getPaddingBottom() != 0) {
                appliedTopInset[0] = topInset;
                root.setPadding(0, topInset, 0, 0);
            }
            int bottomMargin = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars()).bottom + host.dp(16);
            if (appliedBottomMargin[0] != bottomMargin) {
                appliedBottomMargin[0] = bottomMargin;
                bottomParams.bottomMargin = bottomMargin;
                host.bottomNav.setLayoutParams(bottomParams);
            }
            int nextContentBottomPadding = bottomMargin + host.dp(60);
            if (contentBottomPadding[0] != nextContentBottomPadding) {
                contentBottomPadding[0] = nextContentBottomPadding;
                if (host.contentHost != null && !"Карта".equals(host.selectedSection()) && !"AI".equals(host.selectedSection())
                        && host.contentHost.getPaddingBottom() != nextContentBottomPadding) {
                    host.contentHost.setPadding(0, 0, 0, nextContentBottomPadding);
                }
            }
            return insets;
        });
        root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) {
                ViewCompat.requestApplyInsets(root);
            }

            @Override
            public void onViewDetachedFromWindow(View view) {}
        });
        ViewCompat.requestApplyInsets(root);

        host.refreshBottomTabs();
        host.renderContent();

        return root;
    }


    public static LinearLayout dashboardTile(MainActivity host, String title, String detail, int color, String target, String drawableName) {
        LinearLayout tile = host.card(color);
        tile.setPadding(host.dp(14), host.dp(14), host.dp(14), host.dp(14));
        LinearLayout top = new LinearLayout(host);
        top.setGravity(Gravity.CENTER_VERTICAL);

        android.widget.ImageView icon = new android.widget.ImageView(host);
        int resId = host.getResources().getIdentifier(drawableName, "drawable", host.getPackageName());
        if (resId != 0) icon.setImageResource(resId);
        icon.setColorFilter(host.accentColor());
        icon.setBackground(host.roundedBg(Color.rgb(235, 241, 255), 12, 0, Color.TRANSPARENT));
        icon.setPadding(host.dp(7), host.dp(7), host.dp(7), host.dp(7));
        top.addView(icon, new LinearLayout.LayoutParams(host.dp(34), host.dp(34)));
        tile.addView(top);

        TextView titleView = host.text(title, 15, host.primaryText(), true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(0, host.dp(8), 0, 0);
        tile.addView(titleView, titleParams);
        tile.addView(host.text(detail, 12, Color.rgb(92, 103, 120), false));
        tile.setOnClickListener(v -> {
            host.selectedSection = target;
            host.renderContent();
            updateBottomNav(host);
            if (host.statusText != null) {
                host.statusText.setText("ru".equals(host.themeState().activeLanguage) ? "Статус: раздел " + target : "Status: tab " + target);
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(host.dp(0), host.dp(4), host.dp(0), host.dp(4));
        tile.setLayoutParams(params);
        return tile;
    }

    public static View recentItem(MainActivity host, Note note) {
        LinearLayout item = new LinearLayout(host);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(host.dp(8), host.dp(8), host.dp(8), host.dp(8));
        TextView icon = host.text("⌘", 18, Color.rgb(65, 120, 220), false);
        item.addView(icon, new LinearLayout.LayoutParams(host.dp(36), ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout copy = new LinearLayout(host);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(host.text(note.title, 14, host.primaryText(), true));
        copy.addView(host.text(note.displayCategory() + " • " + host.formatTimeAgo(Math.max(note.createdAt, note.updatedAt)), 12, Color.rgb(90, 112, 142), false));
        item.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView dots = host.text("⋮", 20, Color.rgb(86, 96, 112), false);
        dots.setOnClickListener(v -> host.editNoteDialog(note));
        if (note.reminderEnabled) {
            item.addView(host.quickReminderIcon(note), new LinearLayout.LayoutParams(host.dp(28), host.dp(28)));
        }
        item.addView(dots);

        item.setOnClickListener(v -> {
            host.selectedMapEntity = note;
            host.selectedSection = "Карта";
            host.renderContent();
            updateBottomNav(host);
        });
        return item;
    }

    public static LinearLayout actionTile(MainActivity host, String title, String desc, int bgColor, String drawableName) {
        LinearLayout tile = host.card(bgColor);
        tile.setPadding(host.dp(16), host.dp(14), host.dp(16), host.dp(14));

        LinearLayout horizontalLayout = new LinearLayout(host);
        horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
        horizontalLayout.setGravity(Gravity.CENTER_VERTICAL);

        android.widget.ImageView icon = new android.widget.ImageView(host);
        int resId = host.getResources().getIdentifier(drawableName, "drawable", host.getPackageName());
        if (resId != 0) {
            icon.setImageResource(resId);
        }
        icon.setPadding(0, 0, host.dp(14), 0);
        icon.setColorFilter(Color.rgb(65, 120, 220));
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(host.dp(38), host.dp(24));
        horizontalLayout.addView(icon, imgParams);

        LinearLayout textCol = new LinearLayout(host);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.addView(host.text(title, 15, host.primaryText(), true));
        textCol.addView(host.text(desc, 12, host.secondaryText(), false));
        horizontalLayout.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        tile.addView(horizontalLayout);
        return tile;
    }

    public static void addBottomItem(MainActivity host, String section, String label, String drawableName) {
        LinearLayout item = new LinearLayout(host);
        item.setTag(section);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(host.dp(5), host.dp(7), host.dp(5), host.dp(7));

        android.widget.ImageView icon = new android.widget.ImageView(host);
        int resId = host.getResources().getIdentifier(drawableName, "drawable", host.getPackageName());
        if (resId != 0) {
            icon.setImageResource(resId);
        }
        icon.setTag("icon");
        item.addView(icon, new LinearLayout.LayoutParams(host.dp(22), host.dp(22)));

        TextView caption = host.text(label, host.getResources().getDisplayMetrics().widthPixels < host.dp(390) ? 10 : 11, Color.rgb(82, 92, 110), false);
        caption.setTag("label");
        caption.setGravity(Gravity.CENTER);
        caption.setSingleLine(true);
        caption.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelParams.setMargins(0, host.dp(3), 0, 0);
        item.addView(caption, labelParams);

        item.setOnClickListener(v -> {
            if (section.equals(host.selectedSection) && "Структура".equals(section)) {
                host.currentStructurePath = null;
            } else if (section.equals(host.selectedSection) && "Карта".equals(section)) {
                return;
            }
            host.selectedSection = section;
            host.renderContent();
            updateBottomNav(host);
            if (host.statusText != null) {
                host.statusText.setText("ru".equals(host.themeState().activeLanguage) ? "Статус: раздел " + section : "Status: tab " + section);
            }
        });

        LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        navParams.setMargins(host.dp(1), 0, host.dp(1), 0);
        if (host.bottomNav != null) host.bottomNav.addView(item, navParams);
        host.bottomItems.add(item);
    }

    public static void updateBottomNav(MainActivity host) {
        for (View itemView : host.bottomItems) {
            LinearLayout item = (LinearLayout) itemView;
            boolean selected = host.selectedSection.equals(item.getTag());
            int active = Color.rgb(65, 120, 220);
            int inactive = Color.rgb(92, 100, 116);
            item.setBackground(selected ? host.roundedBg(Color.rgb(235, 244, 255), 16, 0, Color.TRANSPARENT) : host.roundedBg(Color.TRANSPARENT, 16, 0, Color.TRANSPARENT));
            for (int i = 0; i < item.getChildCount(); i++) {
                View child = item.getChildAt(i);
                if ("icon".equals(child.getTag()) && child instanceof android.widget.ImageView) {
                    ((android.widget.ImageView) child).setColorFilter(selected ? active : inactive);
                    child.setAlpha(selected ? 1f : 0.72f);
                } else if ("label".equals(child.getTag()) && child instanceof TextView) {
                    TextView label = (TextView) child;
                    label.setTextColor(host.themedTextColor(selected ? active : inactive));
                    host.applyTextWeight(label, selected);
                }
            }
        }
    }
}
