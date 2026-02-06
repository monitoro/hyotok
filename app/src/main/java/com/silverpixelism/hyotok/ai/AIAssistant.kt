package com.silverpixelism.hyotok.ai

import com.google.ai.client.generativeai.GenerativeModel

class AIAssistant(apiKey: String) {
    private val generativeModel = GenerativeModel(
        modelName = "gemini-pro",
        apiKey = apiKey
    )

    suspend fun generateResponse(prompt: String): String {
        return try {
            val response = generativeModel.generateContent(prompt)
            response.text ?: "죄송합니다. 답변을 만들지 못했습니다."
        } catch (e: Exception) {
            "오류가 발생했습니다: ${e.message}"
        }
    }
}
