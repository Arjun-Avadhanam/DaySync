package com.daysync.app.feature.ai.data

import com.daysync.app.feature.ai.model.ChatMessage
import com.daysync.app.feature.ai.model.Role
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Method

class AiRepositoryTest {

    private lateinit var instance: Any
    private lateinit var trimMethod: Method

    @Before
    fun setUp() {
        val clazz = AiRepositoryImpl::class.java
        val unsafe = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null) as sun.misc.Unsafe
        instance = unsafe.allocateInstance(clazz)

        trimMethod = clazz.getDeclaredMethod("trimConversation", List::class.java)
        trimMethod.isAccessible = true
    }

    @Suppress("UNCHECKED_CAST")
    private fun trimConversation(messages: List<ChatMessage>): List<ChatMessage> {
        return trimMethod.invoke(instance, messages) as List<ChatMessage>
    }

    private fun makeMessages(count: Int): List<ChatMessage> {
        return (1..count).map { i ->
            ChatMessage(
                id = "msg-$i",
                content = "Message $i",
                role = if (i % 2 == 1) Role.USER else Role.ASSISTANT,
            )
        }
    }

    // ── trimConversation ────────────────────────────────

    @Test
    fun `under 40 messages returns unchanged`() {
        val messages = makeMessages(10)
        val result = trimConversation(messages)
        assertEquals(10, result.size)
        assertEquals(messages, result)
    }

    @Test
    fun `exactly 40 messages returns unchanged`() {
        val messages = makeMessages(40)
        val result = trimConversation(messages)
        assertEquals(40, result.size)
    }

    @Test
    fun `41 messages trims to 38 - first 2 plus last 36`() {
        val messages = makeMessages(41)
        val result = trimConversation(messages)
        assertEquals(38, result.size)
        assertEquals("Message 1", result[0].content)
        assertEquals("Message 2", result[1].content)
        assertEquals("Message 6", result[2].content)
        assertEquals("Message 41", result.last().content)
    }

    @Test
    fun `60 messages trims to 38`() {
        val messages = makeMessages(60)
        val result = trimConversation(messages)
        assertEquals(38, result.size)
        assertEquals("Message 1", result[0].content)
        assertEquals("Message 2", result[1].content)
        assertEquals("Message 60", result.last().content)
    }

    @Test
    fun `empty list returns empty`() {
        val result = trimConversation(emptyList())
        assertEquals(0, result.size)
    }

    @Test
    fun `single message returns unchanged`() {
        val messages = makeMessages(1)
        val result = trimConversation(messages)
        assertEquals(1, result.size)
    }

    @Test
    fun `preserves first 2 messages for context`() {
        val messages = makeMessages(50)
        val result = trimConversation(messages)
        assertEquals("Message 1", result[0].content)
        assertEquals("Message 2", result[1].content)
    }

    @Test
    fun `preserves last 36 messages for recency`() {
        val messages = makeMessages(50)
        val result = trimConversation(messages)
        assertEquals("Message 15", result[2].content)
        assertEquals("Message 50", result.last().content)
    }
}
