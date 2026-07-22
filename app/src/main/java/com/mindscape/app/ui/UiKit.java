package com.mindscape.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Базовые UI-фабрики приложения: {@code roundedBg}, {@code card}, {@code text},
 * {@code chip}, {@code section}, {@code row}, {@code dp}, {@code spacer}.
 *
 * Все методы статические и принимают {@code Context} + флаг тёмной темы
 * (или {@link ThemeState} там, где нужна доп. информация о тексте).
 * Источник: вынесено из MainActivity.java.
 */
public final class UiKit {

    private UiKit() {}

    public static int dp(Context ctx, float value) {
        return (int) (value * ctx.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static int dp(Context ctx, int value) {
        return dp(ctx, (float) value);
    }

    public static GradientDrawable roundedBg(Context ctx, int color, int radiusDp, int strokeDp, int strokeColor, boolean dark) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(ThemeColors.themedBgColor(color, dark));
        drawable.setCornerRadius(dp(ctx, radiusDp > 0 ? radiusDp : 12));
        if (strokeDp > 0) {
            drawable.setStroke(dp(ctx, strokeDp), ThemeColors.themedStrokeColor(strokeColor, dark));
        }
        return drawable;
    }

    public static LinearLayout card(Context ctx, int color, boolean dark) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(ctx, 14), dp(ctx, 12), dp(ctx, 14), dp(ctx, 12));
        card.setBackground(roundedBg(ctx, color, 12, 1, ThemeColors.strokeColor(dark), dark));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(ctx, 5), 0, dp(ctx, 8));
        card.setLayoutParams(params);
        return card;
    }

    public static TextView text(Context ctx, String value, int sp, int color, boolean bold, ThemeState theme) {
        TextView view = new TextView(ctx);
        view.setText(value);
        view.setTextSize(sp * theme.textSizeRatio);
        view.setTextColor(ThemeColors.themedTextColor(color, theme.isDarkTheme()));
        view.setIncludeFontPadding(true);
        applyTextWeight(view, bold, theme);
        return view;
    }

    public static void applyTextWeight(TextView view, boolean bold, ThemeState theme) {
        view.setTypeface(view.getTypeface(), theme.boldTextEnabled ? Typeface.BOLD : Typeface.NORMAL);
    }

    public static TextView chip(Context ctx, String value, ThemeState theme) {
        boolean dark = theme.isDarkTheme();
        TextView chip = text(ctx, value, 13, ThemeColors.secondaryText(dark), true, theme);
        chip.setPadding(dp(ctx, 10), dp(ctx, 5), dp(ctx, 10), dp(ctx, 5));
        chip.setBackground(roundedBg(ctx, ThemeColors.softSurface(dark), 12, 1, ThemeColors.strokeColor(dark), dark));
        return chip;
    }

    public static TextView section(Context ctx, String title, String body, ThemeState theme) {
        TextView view = text(ctx, title + "\n" + body, 14, Color.rgb(68, 75, 88), false, theme);
        view.setPadding(0, dp(ctx, 14), 0, dp(ctx, 4));
        view.setLineSpacing(dp(ctx, 2), 1f);
        return view;
    }

    public static LinearLayout row(Context ctx) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        return row;
    }
}
