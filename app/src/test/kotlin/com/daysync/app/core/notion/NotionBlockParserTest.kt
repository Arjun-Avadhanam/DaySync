package com.daysync.app.core.notion

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotionBlockParserTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun results(raw: String): JsonArray =
        json.parseToJsonElement(raw).jsonArray

    @Test
    fun `parses headings, bold spans, bullets, numbering and divider`() {
        val blocks = NotionBlockParser.parse(
            results(
                """
                [
                  {"type":"heading_2","heading_2":{"rich_text":[
                    {"plain_text":"1. Week Overview","annotations":{"bold":false,"italic":false,"code":false}}]}},
                  {"type":"paragraph","paragraph":{"rich_text":[
                    {"plain_text":"Highlight: ","annotations":{"bold":true,"italic":false,"code":false}},
                    {"plain_text":"a good week","annotations":{"bold":false,"italic":false,"code":false}}]}},
                  {"type":"divider","divider":{}},
                  {"type":"bulleted_list_item","bulleted_list_item":{"rich_text":[
                    {"plain_text":"Morning weight down","annotations":{"bold":false,"italic":false,"code":false}}]}},
                  {"type":"numbered_list_item","numbered_list_item":{"rich_text":[
                    {"plain_text":"Fix sleep","annotations":{"bold":false,"italic":false,"code":false}}]}},
                  {"type":"numbered_list_item","numbered_list_item":{"rich_text":[
                    {"plain_text":"Add a rest day","annotations":{"bold":false,"italic":false,"code":false}}]}}
                ]
                """.trimIndent()
            ),
            tableRows = emptyMap(),
        )

        assertEquals(6, blocks.size)
        assertEquals(NotionBlock.Heading(2, listOf(RichSpan("1. Week Overview"))), blocks[0])

        val para = blocks[1] as NotionBlock.Paragraph
        assertEquals(listOf(RichSpan("Highlight: ", bold = true), RichSpan("a good week")), para.spans)

        assertTrue(blocks[2] is NotionBlock.Divider)
        assertEquals(NotionBlock.BulletItem(listOf(RichSpan("Morning weight down"))), blocks[3])

        // numbering restarts per run and increments
        assertEquals(1, (blocks[4] as NotionBlock.NumberedItem).number)
        assertEquals(2, (blocks[5] as NotionBlock.NumberedItem).number)
    }

    @Test
    fun `parses a table with its header row from the fetched child rows`() {
        val tableBlocks = results(
            """
            [{"type":"table","id":"tbl-1","has_children":true,
              "table":{"table_width":2,"has_column_header":true}}]
            """.trimIndent()
        )
        val rowsJson = results(
            """
            [
              {"type":"table_row","table_row":{"cells":[
                [{"plain_text":"Metric","annotations":{"bold":false,"italic":false,"code":false}}],
                [{"plain_text":"Average","annotations":{"bold":false,"italic":false,"code":false}}]]}},
              {"type":"table_row","table_row":{"cells":[
                [{"plain_text":"Daily steps","annotations":{"bold":false,"italic":false,"code":false}}],
                [{"plain_text":"15,376","annotations":{"bold":true,"italic":false,"code":false}}]]}}
            ]
            """.trimIndent()
        )

        val blocks = NotionBlockParser.parse(tableBlocks, tableRows = mapOf("tbl-1" to rowsJson))

        assertEquals(1, blocks.size)
        val table = blocks[0] as NotionBlock.Table
        assertTrue(table.hasColumnHeader)
        assertEquals(2, table.rows.size)
        assertEquals(listOf(RichSpan("Metric")), table.rows[0].cells[0])
        assertEquals(listOf(RichSpan("15,376", bold = true)), table.rows[1].cells[1])
    }

    @Test
    fun `a table whose rows failed to load is skipped rather than breaking the summary`() {
        val blocks = NotionBlockParser.parse(
            results(
                """
                [
                  {"type":"table","id":"tbl-missing","has_children":true,"table":{"has_column_header":true}},
                  {"type":"paragraph","paragraph":{"rich_text":[
                    {"plain_text":"still here","annotations":{"bold":false,"italic":false,"code":false}}]}}
                ]
                """.trimIndent()
            ),
            tableRows = emptyMap(),
        )
        assertEquals(1, blocks.size)
        assertEquals(NotionBlock.Paragraph(listOf(RichSpan("still here"))), blocks[0])
    }

    @Test
    fun `collects table block ids so the reader knows which children to fetch`() {
        val ids = NotionBlockParser.tableBlockIds(
            results(
                """
                [
                  {"type":"table","id":"t1","table":{}},
                  {"type":"paragraph","paragraph":{"rich_text":[]}},
                  {"type":"table","id":"t2","table":{}}
                ]
                """.trimIndent()
            )
        )
        assertEquals(listOf("t1", "t2"), ids)
    }
}
