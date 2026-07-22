package com.mindscape.app;

public final class Category {
    public String title;
    public String description;
    public int color;
    public String parent;
    public boolean isCenter;

    public Category(String title, String description, int color, String parent) {
        this(title, description, color, parent, false);
    }

    public Category(String title, String description, int color, String parent, boolean isCenter) {
        this.title = title;
        this.description = description;
        this.color = color;
        this.parent = parent;
        this.isCenter = isCenter;
    }

    public String fullPath() {
        if (parent == null || parent.isEmpty()) return title;
        return parent + "/" + title;
    }
}
