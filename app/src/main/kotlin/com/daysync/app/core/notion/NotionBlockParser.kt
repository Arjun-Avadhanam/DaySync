package com.daysync.app.core.notion

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Pure JSON -> [NotionBlock] conversion. Kept free of networking so it can be
 * unit-tested; [NotionSummaryReader] does the fetching and hands the JSON here.
 */
object NotionBlockParser {

    /** Ids of `table` blocks, whose rows live in a separate children call. */
    fun tableBlockIds(results: JsonArray): List<String> =
        results.mapNotNull { element ->
            val obj = element.jsonObject
            if (obj.type() != "table") return@mapNotNull null
            obj["id"]?.jsonPrimitive?.contentOrNullSafe()
        }

    /**
     * @param results the page's child blocks
     * @param tableRows table block id -> that table's `table_row` children.
     *   A table missing from this map (its fetch failed) is skipped rather than
     *   failing the whole summary.
     */
    fun parse(results: JsonArray, tableRows: Map<String, JsonArray>): List<NotionBlock> {
        val blocks = mutableListOf<NotionBlock>()
        var numberCounter = 0

        for (element in results) {
            val obj = element.jsonObject
            val type = obj.type() ?: continue
            val data = obj[type]?.jsonObject

            // Numbering only runs across consecutive numbered items.
            if (type != "numbered_list_item") numberCounter = 0

            when (type) {
                "heading_1", "heading_2", "heading_3" -> {
                    val level = type.substringAfter('_').toIntOrNull() ?: 2
                    val spans = data.spans()
                    if (spans.isNotEmpty()) blocks += NotionBlock.Heading(level, spans)
                }
                "paragraph" -> {
                    val spans = data.spans()
                    if (spans.any { it.text.isNotBlank() }) blocks += NotionBlock.Paragraph(spans)
                }
                "bulleted_list_item" -> {
                    val spans = data.spans()
                    if (spans.isNotEmpty()) blocks += NotionBlock.BulletItem(spans)
                }
                "numbered_list_item" -> {
                    val spans = data.spans()
                    if (spans.isNotEmpty()) blocks += NotionBlock.NumberedItem(++numberCounter, spans)
                }
                "divider" -> blocks += NotionBlock.Divider
                "table" -> {
                    val id = obj["id"]?.jsonPrimitive?.contentOrNullSafe() ?: continue
                    val rows = tableRows[id]?.let { parseTableRows(it) }.orEmpty()
                    if (rows.isNotEmpty()) {
                        val hasHeader = data?.get("has_column_header")
                            ?.jsonPrimitive?.booleanOrNull ?: false
                        blocks += NotionBlock.Table(hasColumnHeader = hasHeader, rows = rows)
                    }
                }
            }
        }
        return blocks
    }

    private fun parseTableRows(rows: JsonArray): List<TableRow> =
        rows.mapNotNull { element ->
            val obj = element.jsonObject
            if (obj.type() != "table_row") return@mapNotNull null
            val cells = obj["table_row"]?.jsonObject?.get("cells")?.jsonArray ?: return@mapNotNull null
            TableRow(cells = cells.map { cell -> cell.jsonArray.toSpans() })
        }

    private fun JsonObject.type(): String? = this["type"]?.jsonPrimitive?.contentOrNullSafe()

    private fun JsonObject?.spans(): List<RichSpan> =
        this?.get("rich_text")?.jsonArray?.toSpans().orEmpty()

    private fun JsonArray.toSpans(): List<RichSpan> = mapNotNull { element ->
        val obj = element.jsonObject
        val text = obj["plain_text"]?.jsonPrimitive?.contentOrNullSafe() ?: return@mapNotNull null
        if (text.isEmpty()) return@mapNotNull null
        val ann = obj["annotations"]?.jsonObject
        RichSpan(
            text = text,
            bold = ann?.get("bold")?.jsonPrimitive?.booleanOrNull ?: false,
            italic = ann?.get("italic")?.jsonPrimitive?.booleanOrNull ?: false,
            code = ann?.get("code")?.jsonPrimitive?.booleanOrNull ?: false,
        )
    }

    // jsonPrimitive.content throws on JsonNull; guard it.
    private fun kotlinx.serialization.json.JsonPrimitive.contentOrNullSafe(): String? =
        if (this is kotlinx.serialization.json.JsonNull) null else content
}
