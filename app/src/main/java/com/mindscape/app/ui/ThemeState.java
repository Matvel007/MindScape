package com.mindscape.app.ui;

/**
 * Контейнер UI-состояния темы: передаётся в UI-хелперы вместо ссылки на Activity.
 * Пока используется только для темы/локали; позже расширится настройками графа.
 */
public final class ThemeState {
    public int themeMode = 2;          // 0 = Светлая, 1 = Тёмная, 2 = Системная
    public String activeLanguage = "en"; // "ru" или "en"
    public float textSizeRatio = 1f;
    public boolean boldTextEnabled = false;

    public boolean isDarkTheme() {
        if (themeMode == 1) return true;
        if (themeMode == 0) return false;
        // themeMode == 2: системная — делегируется вызывающей стороной через setSystemDark()
        return systemDark;
    }

    public boolean systemDark = false;

    public ThemeState() {}
}
