package com.mindscape.app.graph;

import com.mindscape.app.Category;
import com.mindscape.app.LocalFileLink;
import com.mindscape.app.Note;

import androidx.annotation.Nullable;

/**
 * Контракт между {@link GraphBridge} и Activity: предоставляет данные графа,
 * опции отображения и колбэки разрешения клика по узлу.
 */
public interface GraphHost {
    String buildGraphJson();
    String graphOptionsJs();
    boolean isDarkTheme();
    String rootTitle();

    /** Выполнить JS в WebView карты (на UI-потоке). */
    void evalMapJs(String js);

    // колбэки клика
    void onRootSelected();
    void onEntitySelected(@Nullable Object entity);
}