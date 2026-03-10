package com.daysync.app.feature.ai.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daysync.app.feature.ai.model.ChatMessage
import com.daysync.app.feature.ai.model.Role

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == Role.USER
    val maxWidth = (LocalConfiguration.current.screenWidthDp * 0.8).dp

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = maxWidth),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp,
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            tonalElevation = if (isUser) 0.dp else 1.dp,
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.content.isEmpty() && message.isStreaming) {
                    StreamingIndicator()
                } else {
                    SimpleMarkdownText(
                        text = message.content,
                        color = if (isUser) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    if (message.isStreaming) {
                        StreamingCursor()
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingIndicator() {
    val transition = rememberInfiniteTransition(label = "streaming")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )
    Text(
        text = "Thinking...",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.alpha(alpha),
    )
}

@Composable
private fun StreamingCursor() {
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cursorAlpha",
    )
    Text(
        text = "\u2588",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.alpha(alpha),
    )
}

@Composable
private fun SimpleMarkdownText(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    // Simple markdown rendering: handle **bold** and bullet points
    val lines = text.split("\n")
    Column(modifier = modifier) {
        lines.forEachIndexed { index, line ->
            when {
                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row(modifier = Modifier.padding(start = 4.dp)) {
                        Text(
                            text = "\u2022 ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = color,
                        )
                        BoldAwareText(line.removePrefix("- ").removePrefix("* "), color)
                    }
                }
                line.startsWith("### ") -> {
                    if (index > 0) Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = line.removePrefix("### "),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = color,
                    )
                }
                line.startsWith("## ") -> {
                    if (index > 0) Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = line.removePrefix("## "),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color,
                    )
                }
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                else -> {
                    BoldAwareText(line, color)
                }
            }
        }
    }
}

@Composable
private fun BoldAwareText(
    text: String,
    color: androidx.compose.ui.graphics.Color,
) {
    // Parse **bold** segments
    val parts = text.split("**")
    val annotatedString = androidx.compose.ui.text.buildAnnotatedString {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
                append(part)
                pop()
            } else {
                append(part)
            }
        }
    }
    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
    )
}
