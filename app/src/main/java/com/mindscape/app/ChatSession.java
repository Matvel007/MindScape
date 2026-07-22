package com.mindscape.app;

import java.util.ArrayList;
import java.util.List;

public final class ChatSession {
    public String id = java.util.UUID.randomUUID().toString();
    public String title;
    public List<ChatMessage> messages = new ArrayList<>();

public ChatSession(String title) {
        this.title = title;
    }
}
