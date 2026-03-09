package com.daysync.app.feature.ai.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daysync.app.feature.ai.model.AiChatUiState
import com.daysync.app.feature.ai.ui.components.ChatInputBar
import com.daysync.app.feature.ai.ui.components.ChatMessageBubble

@Composable
fun AiChatSheet(
    viewModel: AiViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new messages
    val messages = when (uiState) {
        is AiChatUiState.Chat -> (uiState as AiChatUiState.Chat).messages
        is AiChatUiState.Error -> (uiState as AiChatUiState.Error).messages
        AiChatUiState.Empty -> emptyList()
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "DaySync AI",
                style = MaterialTheme.typography.titleLarge,
            )
            if (uiState !is AiChatUiState.Empty) {
                IconButton(onClick = { viewModel.clearChat() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear chat")
                }
            }
        }

        // Content
        Box(modifier = Modifier.weight(1f)) {
            when (uiState) {
                AiChatUiState.Empty -> {
                    WelcomeContent(
                        onSuggestionClick = { viewModel.sendMessage(it) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                is AiChatUiState.Chat, is AiChatUiState.Error -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(messages, key = { it.id }) { message ->
                            ChatMessageBubble(message = message)
                        }
                    }

                    // Error snackbar
                    if (uiState is AiChatUiState.Error) {
                        val error = uiState as AiChatUiState.Error
                        Snackbar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                            action = {
                                TextButton(onClick = { viewModel.retryLast() }) {
                                    Text("Retry")
                                }
                            },
                            dismissAction = {
                                TextButton(onClick = { viewModel.dismissError() }) {
                                    Text("Dismiss")
                                }
                            },
                        ) {
                            Text(error.message)
                        }
                    }
                }
            }
        }

        // Input bar
        val isGenerating = uiState is AiChatUiState.Chat &&
            (uiState as AiChatUiState.Chat).isGenerating

        ChatInputBar(
            onSend = { viewModel.sendMessage(it) },
            enabled = !isGenerating,
        )
    }
}

@Composable
private fun WelcomeContent(
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val suggestions = listOf(
        "How did I sleep this week?",
        "What's my calorie trend?",
        "Summarize my expenses this month",
        "How do my workouts affect my sleep?",
        "What am I currently reading/watching?",
        "Show me data for last 10 days",
    )

    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Ask me anything about your data",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "I can analyze your health, nutrition, expenses, workouts, journal, and more.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Try asking:",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        suggestions.forEach { suggestion ->
            AssistChip(
                onClick = { onSuggestionClick(suggestion) },
                label = { Text(suggestion) },
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}
