package com.mindscape.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class AiClient {
    public interface StreamListener {
        void onDelta(String accumulated);
    }

    public static final class HttpException extends Exception {
        public final int code;
        public final String responseBody;

        public HttpException(int code, String responseBody) {
            super("HTTP " + code + ": " + responseBody);
            this.code = code;
            this.responseBody = responseBody;
        }
    }

    private AiClient() {
    }

    public static String callChatCompletion(
            String baseUrl,
            String apiKey,
            String model,
            float maxTokens,
            String systemPrompt,
            String userPrompt
    ) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", model);
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
        messages.put(new JSONObject().put("role", "user").put("content", userPrompt));
        body.put("messages", messages);
        body.put("temperature", 0.2);
        body.put("max_tokens", (int) maxTokens);

        return callChatCompletionOnce(baseUrl, apiKey, body);
    }

    private static String callChatCompletionOnce(
            String baseUrl,
            String apiKey,
            JSONObject body
    ) throws Exception {
        HttpURLConnection connection = openPostConnection(baseUrl, apiKey, 45000);
        writeBody(connection, body);

        int code = connection.getResponseCode();
        String response = readAll(code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream());
        if (code < 200 || code >= 300) {
            throw new HttpException(code, response);
        }
        return RouterAiResponseParser.extractMessage(response);
    }

    public static String streamChatCompletion(
            String baseUrl,
            String apiKey,
            String model,
            float maxTokens,
            String systemPrompt,
            List<ChatMessage> history,
            ChatMessage skipMessage,
            StreamListener listener
    ) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("stream", true);
        body.put("max_tokens", (int) maxTokens);

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
        int start = Math.max(0, history.size() - 20);
        for (int i = start; i < history.size(); i++) {
            ChatMessage histMsg = history.get(i);
            if (histMsg == skipMessage) continue;
            String content = histMsg.isAi ? AiCommandParser.stripCommands(histMsg.text) : histMsg.text;
            if (content == null || content.isEmpty()) continue;
            int limit = histMsg.isAi ? 4000 : 6000;
            if (content.length() > limit) {
                content = content.substring(0, limit) + "\n...";
            }
            messages.put(new JSONObject()
                    .put("role", histMsg.isAi ? "assistant" : "user")
                    .put("content", content));
        }
        body.put("messages", messages);
        body.put("temperature", 0.7);

        return streamChatCompletionOnce(baseUrl, apiKey, body, listener);
    }

    private static String streamChatCompletionOnce(
            String baseUrl,
            String apiKey,
            JSONObject body,
            StreamListener listener
    ) throws Exception {
        HttpURLConnection connection = openPostConnection(baseUrl, apiKey, 60000);
        writeBody(connection, body);

        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new HttpException(code, readAll(connection.getErrorStream()));
        }

        StringBuilder accumulated = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ") && !line.equals("data: [DONE]")) {
                    String delta = parseStreamDelta(line.substring(6));
                    if (!delta.isEmpty()) {
                        accumulated.append(delta);
                        listener.onDelta(accumulated.toString());
                    }
                }
            }
        }
        return accumulated.toString();
    }

    private static HttpURLConnection openPostConnection(String baseUrl, String apiKey, int readTimeoutMs) throws Exception {
        URL url = new URL(baseUrl + "/chat/completions");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(readTimeoutMs);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        if (apiKey != null && !apiKey.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        }
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("User-Agent", "MindScape/1.0");
        return connection;
    }

    public static String extractImageText(String baseUrl, String apiKey, String model, String mimeType, byte[] bytes) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", model);
        JSONArray content = new JSONArray();
        content.put(new JSONObject().put("type", "text").put("text", "Extract all readable text from this image. Return only the extracted text."));
        String dataUrl = "data:" + (mimeType == null || mimeType.isEmpty() ? "image/jpeg" : mimeType)
                + ";base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
        content.put(new JSONObject().put("type", "image_url").put("image_url", new JSONObject().put("url", dataUrl)));
        body.put("messages", new JSONArray().put(new JSONObject().put("role", "user").put("content", content)));
        return callChatCompletionOnce(baseUrl, apiKey, body);
    }

    public static String transcribeAudio(String baseUrl, String apiKey, String model, String fileName, String mimeType, byte[] bytes) throws Exception {
        String boundary = "----MindScape" + System.currentTimeMillis();
        HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + "/audio/transcriptions").openConnection();
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(120000);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        try (OutputStream out = connection.getOutputStream()) {
            writePart(out, boundary, "model", model);
            out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\nContent-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(bytes);
            out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        }
        int code = connection.getResponseCode();
        String response = readAll(code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream());
        if (code < 200 || code >= 300) throw new HttpException(code, response);
        return new JSONObject(response).optString("text", "");
    }

    private static void writePart(OutputStream out, String boundary, String name, String value) throws Exception {
        out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private static void writeBody(HttpURLConnection connection, JSONObject body) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8))) {
            writer.write(body.toString());
        }
    }

    private static String parseStreamDelta(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray choices = obj.optJSONArray("choices");
            if (choices == null || choices.length() == 0) return "";
            JSONObject delta = choices.getJSONObject(0).optJSONObject("delta");
            if (delta == null || delta.isNull("content")) return "";
            return delta.optString("content", "");
        } catch (Exception error) {
            android.util.Log.w("MindScape", "Unable to parse AI stream delta", error);
            return "";
        }
    }

    private static String readAll(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }
}
