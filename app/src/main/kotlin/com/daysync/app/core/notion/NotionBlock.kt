package com.daysync.app.core.notion

/** A run of text with its Notion annotations. Lets the UI render bold/italic/code. */
data class RichSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false,
)

data class TableRow(val cells: List<List<RichSpan>>)

/**
 * The subset of Notion blocks the weekly summary uses. Keeping the structure
 * (instead of flattening to a markdown string) lets the card render real
 * headings, bullets and tables — the tables were previously dropped entirely.
 */
sealed interface NotionBlock {
    data class Heading(val level: Int, val spans: List<RichSpan>) : NotionBlock
    data class Paragraph(val spans: List<RichSpan>) : NotionBlock
    data class BulletItem(val spans: List<RichSpan>) : NotionBlock
    data class NumberedItem(val number: Int, val spans: List<RichSpan>) : NotionBlock
    data object Divider : NotionBlock
    data class Table(val hasColumnHeader: Boolean, val rows: List<TableRow>) : NotionBlock
}

/** Plain-text flattening, used for logging/fallbacks. */
fun List<RichSpan>.plainText(): String = joinToString("") { it.text }
