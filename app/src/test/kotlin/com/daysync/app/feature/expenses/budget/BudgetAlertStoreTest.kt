package com.daysync.app.feature.expenses.budget

import org.junit.Assert.assertEquals
import org.junit.Test

private class InMemoryAlertStore : BudgetAlertLevels {
    val map = mutableMapOf<String, Int>()
    override fun getLevel(key: String) = map[key] ?: 0
    override fun setLevel(key: String, level: Int) { map[key] = level }
    override fun keys() = map.keys.toSet()
    override fun prune(validKeys: Set<String>) { map.keys.retainAll(validKeys) }
}

class BudgetAlertStoreTest {
    @Test
    fun `get returns 0 for unknown key and stored value after set`() {
        val store = InMemoryAlertStore()
        assertEquals(0, store.getLevel("MONTHLY:2026-07"))
        store.setLevel("MONTHLY:2026-07", 75)
        assertEquals(75, store.getLevel("MONTHLY:2026-07"))
    }

    @Test
    fun `prune drops keys not in the valid set`() {
        val store = InMemoryAlertStore()
        store.setLevel("A", 50); store.setLevel("B", 100)
        store.prune(setOf("B"))
        assertEquals(0, store.getLevel("A"))
        assertEquals(100, store.getLevel("B"))
    }
}
