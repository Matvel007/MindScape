package com.mindscape.app;

public final class LocalFileLink {
    public String title;
    public String folderPath;
    public String uri;
    public String mimeType;
    public long size;
    public long addedAt;

    public LocalFileLink(String title, String folderPath, String uri, String mimeType, long size, long addedAt) {
        this.title = title;
        this.folderPath = folderPath;
        this.uri = uri;
        this.mimeType = mimeType;
        this.size = size;
        this.addedAt = addedAt;
    }

    public boolean isUnbound() {
        return folderPath == null || folderPath.trim().isEmpty();
    }

    public String displayFolder() {
        return isUnbound() ? "-" : folderPath.replace("/", " > ");
    }

    public String nodeId() {
        return "file:" + addedAt + ":" + uri;
    }
}
