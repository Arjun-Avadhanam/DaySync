package com.daysync.app.feature.ai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daysync.app.feature.ai.data.AiRepository
import com.daysync.app.feature.ai.model.AiChatUiState
import com.daysync.app.feature.ai.model.ChatMessage
import com.daysync.app.feature.ai.model.Role
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Clock

@HiltViewModel
class AiViewModel @Inject constructor(
    private val repository: AiRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AiChatUiState>(AiChatUiState.Empty)
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    private val _messages = mutableListOf<ChatMessage>()
    private var generateJob: Job? = null

    fun sendMessage(text: String) {
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) return

        // Cancel any in-flight generation
        generateJob?.cancel()

        // Add user message
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = Role.USER,
            content = trimmedText,
            timestamp = Clock.System.now(),
        )
        _messages.add(userMessage)

        // Add placeholder assistant message
        val assistantMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = Role.ASSISTANT,
            content = "",
            timestamp = Clock.System.now(),
            isStreaming = true,
        )
        _messages.add(assistantMessage)
        _uiState.value = AiChatUiState.Chat(
            messages = _messages.toList(),
            isGenerating = true,
        )

        generateJob = viewModelScope.launch {
            val assistantIndex = _messages.lastIndex
            val contentBuilder = StringBuilder()

            try {
                repository.askQuestion(trimmedText, _messages.dropLast(1))
                    .collect { chunk ->
                        contentBuilder.append(chunk)
                        _messages[assistantIndex] = _messages[assistantIndex].copy(
                            content = contentBuilder.toString(),
                            isStreaming = true,
                        )
                        _uiState.value = AiChatUiState.Chat(
                            messages = _messages.toList(),
                            isGenerating = true,
                        )
                    }

                // Mark streaming complete
                _messages[assistantIndex] = _messages[assistantIndex].copy(
                    isStreaming = false,
                )
                _uiState.value = AiChatUiState.Chat(
                    messages = _messages.toList(),
                    isGenerating = false,
                )
            } catch (e: Exception) {
                Log.e("AiChat", "AI generation failed", e)
                // Remove the empty assistant placeholder on error
                if (_messages[assistantIndex].content.isEmpty()) {
                    _messages.removeAt(assistantIndex)
                } else {
                    _messages[assistantIndex] = _messages[assistantIndex].copy(isStreaming = false)
                }
                val errorMsg = when {
                    e.message?.contains("API key", ignoreCase = true) == true -> "API key error: ${e.message}"
                    e.message?.contains("network", ignoreCase = true) == true -> "Network error — check your connection"
                    e.message?.contains("timeout", ignoreCase = true) == true -> "Request timed out — try again"
                    else -> "AI error: ${e.message ?: "Unknown error"}"
                }
                _uiState.value = AiChatUiState.Error(
                    message = errorMsg,
                    messages = _messages.toList(),
                )
            }
        }
    }

    fun clearChat() {
        generateJob?.cancel()
        _messages.clear()
        _uiState.value = AiChatUiState.Empty
    }

    fun retryLast() {
        val lastUserMessage = _messages.lastOrNull { it.role == Role.USER } ?: return
        // Remove last assistant message if it exists
        if (_messages.lastOrNull()?.role == Role.ASSISTANT) {
            _messages.removeAt(_messages.lastIndex)
        }
        // Remove last user message (sendMessage will re-add it)
        _messages.removeAt(_messages.lastIndex)
        sendMessage(lastUserMessage.content)
    }

    fun dismissError() {
        _uiState.value = if (_messages.isEmpty()) {
            AiChatUiState.Empty
        } else {
            AiChatUiState.Chat(messages = _messages.toList(), isGenerating = false)
        }
    }
}
