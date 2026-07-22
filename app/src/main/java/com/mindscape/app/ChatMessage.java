package com.mindscape.app;

public final class ChatMessage {
    public boolean isAi;
    public String text;

    public ChatMessage(boolean isAi, String text) {
        this.isAi = isAi;
        this.text = text;
    }
}
