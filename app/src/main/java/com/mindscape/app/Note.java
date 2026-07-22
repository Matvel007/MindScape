package com.mindscape.app;

public final class Note {
    public String title;
    public String categoryPath;
    public String content;
    public boolean favorite;
    public long createdAt;
    public long updatedAt;
    public boolean reminderEnabled;
    public long reminderAt;
    public boolean reminderTriggered;
    public boolean quickContainerNote;

    public Note(String title, String categoryPath, String content) {
        this.title = title;
        this.categoryPath = categoryPath;
        this.content = content;
        this.favorite = false;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
        this.reminderEnabled = false;
        this.reminderAt = 0L;
        this.reminderTriggered = false;
        this.quickContainerNote = false;
    }

    public Note(String title, String categoryPath, String content, long createdAt, long updatedAt) {
        this.title = title;
        this.categoryPath = categoryPath;
        this.content = content;
        this.favorite = false;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.reminderEnabled = false;
        this.reminderAt = 0L;
        this.reminderTriggered = false;
        this.quickContainerNote = false;
    }

    public String deepestCategory() {
        if (categoryPath == null || categoryPath.isEmpty()) return "";
        int last = categoryPath.lastIndexOf('/');
        return last >= 0 ? categoryPath.substring(last + 1) : categoryPath;
    }

    public String displayCategory() {
        return isUnbound() ? "—" : categoryPath.replace("/", " > ");
    }

    public boolean isUnbound() {
        return categoryPath == null || categoryPath.trim().isEmpty();
    }

    public String fullPath() {
        if (categoryPath == null || categoryPath.isEmpty()) return title;
        return categoryPath + "/" + title;
    }
}
