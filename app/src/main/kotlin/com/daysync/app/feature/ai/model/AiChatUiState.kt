package com.daysync.app.feature.ai.model

sealed interface AiChatUiState {
    data object Empty : AiChatUiState
    data class Chat(
        val messages: List<ChatMessage>,
        val isGenerating: Boolean,
    ) : AiChatUiState
    data class Error(
        val message: String,
        val messages: List<ChatMessage>,
    ) : AiChatUiState
}
