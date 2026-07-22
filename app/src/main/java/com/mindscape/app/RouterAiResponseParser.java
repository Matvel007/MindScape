package com.mindscape.app;

import org.json.JSONObject;

public final class RouterAiResponseParser {
    private RouterAiResponseParser() {
    }

    public static String extractMessage(String responseJson) throws Exception {
        JSONObject json = new JSONObject(responseJson);
        JSONObject message = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message");
        String content = message.optString("content").trim();
        if (content.isEmpty()) {
            content = message.optString("reasoning").trim();
        }
        if (content.isEmpty()) {
            return "RouterAI вернул пустой ответ.";
        }
        return content.replace("**", "").trim();
    }
}
