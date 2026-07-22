package com.mindscape.app;

public final class KnowledgeNode {
    public final String title;
    public final String group;
    public final float x;
    public final float y;
    public final int level;
    public final int color;
    public final int badgeCount;

    public KnowledgeNode(String title, String group, float x, float y, int level, int color) {
        this(title, group, x, y, level, color, 0);
    }

    public KnowledgeNode(String title, String group, float x, float y, int level, int color, int badgeCount) {
        this.title = title;
        this.group = group;
        this.x = x;
        this.y = y;
        this.level = level;
        this.color = color;
        this.badgeCount = badgeCount;
    }
}
