package com.mindscape.app.ai;

/**
 * Каталог ИИ-провайдеров: имена, base URL-ы, индексы.
 * Источник: вынесено из MainActivity.java (AI_PROVIDER_NAMES, AI_PROVIDER_URLS, getProviderIndex).
 */
public final class AiProviders {

    public static final String[] NAMES = {
            "RouterAI", "AITunnel", "OpenAI", "OpenRouter",
            "DeepSeek", "Mistral", "TogetherAI", "Groq", "Custom server"
    };

    public static final String[] URLS = {
            "https://routerai.ru/api/v1",
            "https://api.aitunnel.ru/v1",
            "https://api.openai.com/v1",
            "https://openrouter.ai/api/v1",
            "https://api.deepseek.com",
            "https://api.mistral.ai/v1",
            "https://api.together.xyz/v1",
            "https://api.groq.com/openai/v1",
            ""
    };

    private AiProviders() {}

    /** Индекс провайдера по base URL; последний индекс = «свой сервер». */
    public static int indexOf(String baseUrl) {
        for (int i = 0; i < URLS.length - 1; i++) {
            if (URLS[i].equals(baseUrl)) return i;
        }
        return URLS.length - 1;
    }
}
