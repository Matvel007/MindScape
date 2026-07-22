package com.mindscape.app

import org.junit.Assert.assertEquals
import org.junit.Test

class RouterAiResponseParserTest {
    @Test
    fun extractsContentWhenPresent() {
        val json = """
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "Готовый ответ",
                    "reasoning": "Черновик"
                  }
                }
              ]
            }
        """.trimIndent()

        assertEquals("Готовый ответ", RouterAiResponseParser.extractMessage(json))
    }

    @Test
    fun fallsBackToReasoningWhenContentIsEmpty() {
        val json = """
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "",
                    "reasoning": "Промежуточный ответ RouterAI"
                  }
                }
              ]
            }
        """.trimIndent()

        assertEquals("Промежуточный ответ RouterAI", RouterAiResponseParser.extractMessage(json))
    }

    @Test
    fun removesSimpleMarkdownBoldMarkers() {
        val json = """
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "1. **Квантовая физика — Наблюдатель**: связь",
                    "reasoning": ""
                  }
                }
              ]
            }
        """.trimIndent()

        assertEquals(
            "1. Квантовая физика — Наблюдатель: связь",
            RouterAiResponseParser.extractMessage(json)
        )
    }
}
