package com.daysync.app.feature.ai.data

import android.util.Log
import com.daysync.app.feature.ai.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface AiRepository {
    fun askQuestion(
        question: String,
        conversationHistory: List<ChatMessage>,
    ): Flow<String>
}

class AiRepositoryImpl(
    private val geminiChatService: GeminiChatService,
    private val groqChatService: GroqChatService,
    private val dataContextBuilder: DataContextBuilder,
) : AiRepository {

    companion object {
        private const val TAG = "AiRepository"
        private const val MAX_TURNS = 20
        private const val SYSTEM_PROMPT_TEMPLATE = """You are DaySync AI, a personal daily life analyst. You have access to the user's health, nutrition, expense, workout, journal, media, and sports data.

<data_context>
%s
</data_context>

Instructions:
- Answer based ONLY on the data provided above
- If data is missing for a section, say so
- Use specific numbers and dates from the data
- For trends, compare across days
- Keep responses concise but insightful
- Format with markdown (bold, bullet points) for readability
- Use ₹ for currency amounts"""
    }

    override fun askQuestion(
        question: String,
        conversationHistory: List<ChatMessage>,
    ): Flow<String> = flow {
        val dataContext = dataContextBuilder.buildContextForQuestion(question)
        val systemPrompt = SYSTEM_PROMPT_TEMPLATE.format(dataContext)
        val trimmedHistory = trimConversation(conversationHistory)

        try {
            geminiChatService.generateStream(trimmedHistory, systemPrompt)
                .collect { chunk -> emit(chunk) }
        } catch (e: Exception) {
            Log.w(TAG, "Gemini failed, falling back to Groq", e)
            try {
                val response = groqChatService.generate(trimmedHistory, systemPrompt)
                emit(response)
            } catch (groqError: Exception) {
                Log.e(TAG, "Groq fallback also failed", groqError)
                throw groqError
            }
        }
    }

    private fun trimConversation(messages: List<ChatMessage>): List<ChatMessage> {
        // 20-turn cap: each turn = 1 user + 1 assistant = 2 messages = max 40 messages
        if (messages.size <= MAX_TURNS * 2) return messages
        // Keep first 2 messages (initial context) + last 36 messages
        return messages.take(2) + messages.takeLast(MAX_TURNS * 2 - 4)
    }
}
