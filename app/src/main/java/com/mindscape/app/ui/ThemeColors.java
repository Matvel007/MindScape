package com.mindscape.app.ui;

import android.graphics.Color;

/**
 * Все цветовые токены приложения и хелперы адаптации под тёмную тему.
 * Статические методы принимают флаг {@code dark}, чтобы быть независимыми
 * от Activity и переиспользоваться в любом экране/диалоге.
 *
 * Источник: вынесено из MainActivity.java (методы appBg/surface/.../themedStrokeColor).
 */
public final class ThemeColors {

    private ThemeColors() {}

    public static int appBg(boolean dark) {
        return dark ? Color.rgb(18, 20, 24) : Color.rgb(232, 234, 233);
    }

    public static int surface(boolean dark) {
        return dark ? Color.rgb(30, 33, 39) : Color.rgb(243, 244, 242);
    }

    public static int softSurface(boolean dark) {
        return dark ? Color.rgb(39, 43, 51) : Color.rgb(224, 227, 226);
    }

    public static int primaryText(boolean dark) {
        return dark ? Color.rgb(235, 239, 247) : Color.rgb(32, 38, 48);
    }

    public static int secondaryText(boolean dark) {
        return dark ? Color.rgb(166, 176, 192) : Color.rgb(106, 116, 132);
    }

    public static int strokeColor(boolean dark) {
        return dark ? Color.rgb(58, 64, 76) : Color.rgb(201, 204, 202);
    }

    public static int accentColor(boolean dark) {
        return dark ? Color.rgb(117, 168, 255) : Color.rgb(65, 120, 220);
    }

    /**
     * Адаптирует цвет фона под текущую тему: слишком светлые цвета
     * заменяются на surface/softSurface, чтобы не выжигать глаза.
     */
    public static int themedBgColor(int color, boolean dark) {
        if (color == Color.TRANSPARENT) return color;
        int avg = (Color.red(color) + Color.green(color) + Color.blue(color)) / 3;
        if (dark) {
            if (avg > 238) return surface(true);
            if (avg > 215) return softSurface(true);
            return color;
        }
        if (avg > 250) return surface(false);
        return color;
    }

    /**
     * Адаптирует цвет текста под тёмную тему: слишком тёмные цвета
     * заменяются на primaryText/secondaryText.
     */
    public static int themedTextColor(int color, boolean dark) {
        if (!dark) return color;
        int avg = (Color.red(color) + Color.green(color) + Color.blue(color)) / 3;
        if (avg < 90) return primaryText(true);
        if (avg < 170) return secondaryText(true);
        return color;
    }

    /**
     * Адаптирует цвет обводки под тёмную тему: слишком светлые «сероватые» цвета
     * заменяются на дефолтный strokeColor.
     */
    public static int themedStrokeColor(int color, boolean dark) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        int avg = (r + g + b) / 3;
        if (!dark) {
            if (avg > 228 && Math.abs(r - g) < 28 && Math.abs(g - b) < 28) {
                return strokeColor(false);
            }
            return color;
        }
        if (avg > 160 && Math.abs(r - g) < 28 && Math.abs(g - b) < 28) {
            return strokeColor(true);
        }
        return color;
    }
}
