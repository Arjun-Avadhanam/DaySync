package com.daysync.app.core.notion

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/** Renders Notion blocks as real Compose elements (headings, lists, tables). */
@Composable
fun NotionBlocks(
    blocks: List<NotionBlock>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        blocks.forEach { block ->
            when (block) {
                is NotionBlock.Heading -> HeadingBlock(block)
                is NotionBlock.Paragraph -> Text(
                    text = block.spans.toAnnotatedString(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                is NotionBlock.BulletItem -> ListItemRow("•", block.spans)
                is NotionBlock.NumberedItem -> ListItemRow("${block.number}.", block.spans)
                NotionBlock.Divider -> HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = LocalContentColor.current.copy(alpha = 0.25f),
                )
                is NotionBlock.Table -> TableBlock(block)
            }
        }
    }
}

@Composable
private fun HeadingBlock(block: NotionBlock.Heading) {
    val style = when (block.level) {
        1 -> MaterialTheme.typography.titleLarge
        2 -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.titleSmall
    }
    Text(
        text = block.spans.toAnnotatedString(),
        style = style,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
    )
}

@Composable
private fun ListItemRow(marker: String, spans: List<RichSpan>) {
    Row(modifier = Modifier.padding(bottom = 3.dp)) {
        Text(
            text = marker,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 6.dp),
        )
        Text(
            text = spans.toAnnotatedString(),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * Header row + data rows. Wide tables (the workout log runs to six columns)
 * scroll sideways rather than being squeezed into an unreadable grid.
 */
@Composable
private fun TableBlock(table: NotionBlock.Table) {
    val columnCount = table.rows.maxOfOrNull { it.cells.size } ?: return
    Column(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .horizontalScroll(rememberScrollState()),
    ) {
        table.rows.forEachIndexed { index, row ->
            val isHeader = table.hasColumnHeader && index == 0
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                repeat(columnCount) { col ->
                    val cell = row.cells.getOrNull(col).orEmpty()
                    Text(
                        text = cell.toAnnotatedString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.widthIn(min = 72.dp, max = 200.dp),
                    )
                }
            }
            if (isHeader || index < table.rows.lastIndex) {
                HorizontalDivider(color = LocalContentColor.current.copy(alpha = 0.15f))
            }
        }
    }
}

private fun List<RichSpan>.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
    this@toAnnotatedString.forEach { span ->
        withStyle(
            SpanStyle(
                fontWeight = if (span.bold) FontWeight.Bold else null,
                fontStyle = if (span.italic) FontStyle.Italic else null,
                fontFamily = if (span.code) FontFamily.Monospace else null,
            )
        ) {
            append(span.text)
        }
    }
}
