package com.mindscape.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.function.IntConsumer;

/**
 * Стилизованные контролы приложения: input, switch, slider, button, dropdown.
 * Все методы статические, принимают {@code Context} + {@link ThemeState}.
 * {@code createStyledSwitch} возвращает View, в {@code getTag()} которого
 * лежит {@link StyledToggleState}.
 *
 * Источник: вынесено из MainActivity.java.
 */
public final class StyledControls {

    private StyledControls() {}

    public static View slider(Context ctx, ThemeState theme, int max, int currentProgress, String unit, final SeekBar.OnSeekBarChangeListener clientListener) {
        boolean dark = theme.isDarkTheme();
        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(UiKit.dp(ctx, 12), UiKit.dp(ctx, 10), UiKit.dp(ctx, 12), UiKit.dp(ctx, 10));
        container.setBackground(UiKit.roundedBg(ctx, ThemeColors.surface(dark), 12, 1, ThemeColors.strokeColor(dark), dark));

        final TextView valText = UiKit.text(ctx, String.valueOf(currentProgress) + " " + unit, 13, Color.rgb(65, 120, 220), true, theme);
        valText.setPadding(0, 0, 0, UiKit.dp(ctx, 4));
        container.addView(valText);

        SeekBar sb = new SeekBar(ctx);
        sb.setMax(max);
        sb.setProgress(currentProgress);
        int accent = ThemeColors.accentColor(dark);
        sb.setProgressTintList(ColorStateList.valueOf(accent));
        sb.setThumbTintList(ColorStateList.valueOf(accent));

        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valText.setText(String.valueOf(progress) + " " + unit);
                if (clientListener != null) clientListener.onProgressChanged(seekBar, progress, fromUser);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (clientListener != null) clientListener.onStartTrackingTouch(seekBar);
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (clientListener != null) clientListener.onStopTrackingTouch(seekBar);
            }
        });

        container.addView(sb);
        container.setTag(sb);
        return container;
    }

    public static EditText input(Context ctx, ThemeState theme, String initialText, String hint) {
        boolean dark = theme.isDarkTheme();
        EditText input = new EditText(ctx);
        input.setText(initialText);
        input.setHint(hint);
        input.setTextSize(14);
        input.setTextColor(ThemeColors.primaryText(dark));
        input.setHintTextColor(ThemeColors.secondaryText(dark));
        UiKit.applyTextWeight(input, false, theme);
        input.setPadding(UiKit.dp(ctx, 14), UiKit.dp(ctx, 12), UiKit.dp(ctx, 14), UiKit.dp(ctx, 12));
        input.setBackground(UiKit.roundedBg(ctx, ThemeColors.softSurface(dark), 12, 1, ThemeColors.strokeColor(dark), dark));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, UiKit.dp(ctx, 6), 0, UiKit.dp(ctx, 6));
        input.setLayoutParams(params);
        return input;
    }

    public static void configureLargeTextInput(Context ctx, EditText input, int heightDp) {
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setMinLines(4);
        input.setMaxLines(8);
        input.setVerticalScrollBarEnabled(true);
        input.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        input.setSingleLine(false);
        input.setHorizontallyScrolling(false);
        input.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(ctx, heightDp)));
    }

    public static ScrollView scrollableDialogContent(Context ctx, View content, int maxHeightDp) {
        ScrollView scroll = new ScrollView(ctx);
        scroll.setFillViewport(false);
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        scroll.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(ctx, maxHeightDp)));
        return scroll;
    }

    public static View switchToggle(Context ctx, ThemeState theme, String label, boolean checked) {
        boolean dark = theme.isDarkTheme();
        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER_VERTICAL);
        container.setPadding(UiKit.dp(ctx, 14), UiKit.dp(ctx, 12), UiKit.dp(ctx, 14), UiKit.dp(ctx, 12));
        container.setBackground(UiKit.roundedBg(ctx, ThemeColors.surface(dark), 12, 1, ThemeColors.strokeColor(dark), dark));

        TextView labelView = UiKit.text(ctx, label, 14, ThemeColors.primaryText(dark), true, theme);
        container.addView(labelView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        FrameLayout toggle = new FrameLayout(ctx);
        toggle.setPadding(UiKit.dp(ctx, 3), UiKit.dp(ctx, 3), UiKit.dp(ctx, 3), UiKit.dp(ctx, 3));
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(UiKit.dp(ctx, 54), UiKit.dp(ctx, 30));
        toggleParams.setMargins(UiKit.dp(ctx, 12), 0, 0, 0);
        container.addView(toggle, toggleParams);

        View thumb = new View(ctx);
        FrameLayout.LayoutParams thumbParams = new FrameLayout.LayoutParams(UiKit.dp(ctx, 24), UiKit.dp(ctx, 24));
        toggle.addView(thumb, thumbParams);

        java.util.function.Consumer<Boolean> refresh = animate -> {
            boolean state = Boolean.TRUE.equals(toggle.getTag());
            toggle.setBackground(UiKit.roundedBg(ctx,
                    state ? ThemeColors.accentColor(dark) : (dark ? Color.rgb(62, 70, 86) : Color.rgb(205, 211, 220)),
                    15, 0, Color.TRANSPARENT, dark));
            thumb.setBackground(UiKit.roundedBg(ctx,
                    state ? Color.WHITE : (dark ? Color.rgb(210, 216, 225) : Color.WHITE),
                    12, state ? 0 : 1, state ? Color.TRANSPARENT : Color.rgb(185, 192, 202), dark));
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) thumb.getLayoutParams();
            lp.gravity = state ? (Gravity.END | Gravity.CENTER_VERTICAL) : (Gravity.START | Gravity.CENTER_VERTICAL);
            thumb.setLayoutParams(lp);
            if (Boolean.TRUE.equals(animate)) {
                thumb.animate().scaleX(0.9f).scaleY(0.9f).setDuration(70).withEndAction(() ->
                        thumb.animate().scaleX(1f).scaleY(1f).setDuration(110).start()
                ).start();
            }
        };
        toggle.setTag(checked);
        refresh.accept(false);

        StyledToggleState state = new StyledToggleState(checked);
        state.setVisualListener(isChecked -> {
            toggle.setTag(isChecked);
            refresh.accept(true);
        });

        container.setOnClickListener(v -> state.setChecked(!state.isChecked()));

        container.setTag(state);
        return container;
    }

    public static Button button(Context ctx, ThemeState theme, String text) {
        return button(ctx, theme, text, false);
    }

    public static Button primaryButton(Context ctx, ThemeState theme, String text) {
        return button(ctx, theme, text, true);
    }

    private static Button button(Context ctx, ThemeState theme, String text, boolean primary) {
        boolean dark = theme.isDarkTheme();
        Button b = new Button(ctx);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(UiKit.dp(ctx, 50));
        b.setMinimumHeight(UiKit.dp(ctx, 50));
        b.setPadding(UiKit.dp(ctx, 16), UiKit.dp(ctx, 10), UiKit.dp(ctx, 16), UiKit.dp(ctx, 10));
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setIncludeFontPadding(false);
        b.setStateListAnimator(null);
        b.setElevation(0f);
        UiKit.applyTextWeight(b, true, theme);
        if (primary) {
            b.setTextColor(ThemeColors.themedTextColor(Color.WHITE, dark));
            b.setBackground(UiKit.roundedBg(ctx, ThemeColors.accentColor(dark), 12, 0, Color.TRANSPARENT, dark));
        } else {
            b.setTextColor(ThemeColors.primaryText(dark));
            b.setBackground(UiKit.roundedBg(ctx, ThemeColors.softSurface(dark), 12, 1, ThemeColors.strokeColor(dark), dark));
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        b.setLayoutParams(params);
        return b;
    }

    public static View dropdown(Context ctx, ThemeState theme, String cancelLabel,
                                 String currentValue, String[] items, int selectedIdx, IntConsumer onSelect) {
        boolean dark = theme.isDarkTheme();
        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER_VERTICAL);
        container.setPadding(UiKit.dp(ctx, 14), UiKit.dp(ctx, 12), UiKit.dp(ctx, 14), UiKit.dp(ctx, 12));
        container.setBackground(UiKit.roundedBg(ctx, ThemeColors.surface(dark), 12, 1, ThemeColors.strokeColor(dark), dark));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, UiKit.dp(ctx, 6), 0, UiKit.dp(ctx, 6));
        container.setLayoutParams(lp);

        TextView label = new TextView(ctx);
        label.setText(currentValue);
        label.setTextSize(14);
        label.setTextColor(ThemeColors.primaryText(dark));
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        container.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = new TextView(ctx);
        arrow.setText("▼");
        arrow.setTextSize(10);
        arrow.setTextColor(ThemeColors.secondaryText(dark));
        container.addView(arrow);

        container.setOnClickListener(v -> {
            LinearLayout dialogLayout = new LinearLayout(ctx);
            dialogLayout.setOrientation(LinearLayout.VERTICAL);
            dialogLayout.setPadding(UiKit.dp(ctx, 8), UiKit.dp(ctx, 8), UiKit.dp(ctx, 8), UiKit.dp(ctx, 8));
            ScrollView sv = new ScrollView(ctx);
            sv.addView(dialogLayout);

            AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
            builder.setView(sv);
            builder.setNegativeButton(cancelLabel, null);
            AlertDialog dlg = builder.create();
            dlg.getWindow().setBackgroundDrawable(UiKit.roundedBg(ctx, ThemeColors.surface(dark), 16, 0, Color.TRANSPARENT, dark));

            for (int i = 0; i < items.length; i++) {
                final int idx = i;
                LinearLayout row = new LinearLayout(ctx);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(UiKit.dp(ctx, 16), UiKit.dp(ctx, 14), UiKit.dp(ctx, 16), UiKit.dp(ctx, 14));
                if (i == selectedIdx) {
                    row.setBackground(UiKit.roundedBg(ctx, dark ? Color.rgb(45, 55, 80) : Color.rgb(230, 238, 252), 12, 0, Color.TRANSPARENT, dark));
                }

                TextView itemTv = new TextView(ctx);
                itemTv.setText(items[i]);
                itemTv.setTextSize(15);
                itemTv.setTextColor(ThemeColors.primaryText(dark));
                row.addView(itemTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                if (i == selectedIdx) {
                    TextView check = new TextView(ctx);
                    check.setText("✓");
                    check.setTextSize(16);
                    check.setTextColor(Color.rgb(65, 120, 220));
                    row.addView(check);
                }

                row.setOnClickListener(rv -> {
                    dlg.dismiss();
                    onSelect.accept(idx);
                });
                dialogLayout.addView(row);
                if (i < items.length - 1) {
                    View divider = new View(ctx);
                    divider.setBackgroundColor(ThemeColors.strokeColor(dark));
                    dialogLayout.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(ctx, 1)));
                }
            }
            dlg.show();
        });
        container.setTag(label);
        return container;
    }
}
