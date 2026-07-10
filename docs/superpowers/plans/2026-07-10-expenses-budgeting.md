# Expenses Budgeting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add range-based budgeting to the Expenses section — monthly, day-of-month "week" blocks, and arbitrary custom date ranges — with a live "money left" banner, a Week|Month browse toggle, and 50/75/100% notifications, where budget progress is always derived from current expenses so edits/deletes recompute automatically.

**Architecture:** A single `budgets` Room table holds every budget as a recurring template or a specific-month instance. Pure Kotlin logic (`MonthWeeks`, `BudgetResolver`, `BudgetThresholds`) is unit-tested in isolation. "Spent" is never stored — it is a live `SUM(totalAmount)` over each budget's date range, so add/edit/delete recompute for free. A `BudgetAlertEvaluator` runs on every expense mutation (all capture paths pass through `ExpenseRepositoryImpl`) plus a daily safety-net worker; a device-local SharedPreferences store dedups alerts and re-arms them downward. Budgets sync to Supabase like other user data.

**Tech Stack:** Kotlin (JDK 17), Room (SQLite), Hilt, Jetpack Compose + Material 3, WorkManager, kotlinx-datetime, kotlinx.serialization, Supabase Kotlin SDK. Tests: JUnit 4 + MockK + kotlinx-coroutines-test (JVM unit tests; no device in CI).

## Global Constraints

- Build env: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew <task>`.
- Room schema changes require: entity + `version` bump in `AppDatabase.kt` + an explicit `Migration` in `DatabaseModule.kt` (never rely on `fallbackToDestructiveMigration`) + a numbered Supabase SQL file under `supabase/migrations/`. Sync DTO new fields default to `null`.
- Current Room version is **6**; this feature bumps it to **7**.
- Timezone always via `UserPreferences.kotlinTimeZone`; currency always via `UserPreferences.formatCurrency(amount)`. Never hardcode IST/INR.
- Overall budget only in v1. `BudgetEntity.category` exists (nullable, `null` = overall) but is never set — reserved for a future per-category feature.
- Weekly = **day-of-month blocks**: 1–7, 8–14, 15–21, 22–28, 29–end; block 5 omitted when the month has ≤ 28 days.
- Alert thresholds fixed at **50 / 75 / 100**.
- Notification dedup state is **device-local** (SharedPreferences), never synced.
- No `Co-Authored-By` trailer in commits (user preference).
- Commit messages: imperative, concise, no attribution trailer.

---

### Task 1: `MonthWeeks` day-of-month block math

**Files:**
- Create: `app/src/main/kotlin/com/daysync/app/feature/expenses/budget/model/MonthWeeks.kt`
- Test: `app/src/test/kotlin/com/daysync/app/feature/expenses/budget/model/MonthWeeksTest.kt`

**Interfaces:**
- Produces:
  - `data class WeekBlock(val index: Int, val start: LocalDate, val end: LocalDate)`
  - `object MonthWeeks { fun blocksFor(year: Int, month: Int): List<WeekBlock>; fun blockContaining(date: LocalDate): WeekBlock; fun daysInMonth(year: Int, month: Int): Int }`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.daysync.app.feature.expenses.budget.model

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class MonthWeeksTest {
    @Test
    fun `31-day month has five blocks with correct bounds`() {
        val blocks = MonthWeeks.blocksFor(2026, 7) // July = 31 days
        assertEquals(5, blocks.size)
        assertEquals(WeekBlock(1, LocalDate(2026, 7, 1), LocalDate(2026, 7, 7)), blocks[0])
        assertEquals(WeekBlock(2, LocalDate(2026, 7, 8), LocalDate(2026, 7, 14)), blocks[1])
        assertEquals(WeekBlock(3, LocalDate(2026, 7, 15), LocalDate(2026, 7, 21)), blocks[2])
        assertEquals(WeekBlock(4, LocalDate(2026, 7, 22), LocalDate(2026, 7, 28)), blocks[3])
        assertEquals(WeekBlock(5, LocalDate(2026, 7, 29), LocalDate(2026, 7, 31)), blocks[4])
    }

    @Test
    fun `28-day February has four blocks and no block five`() {
        val blocks = MonthWeeks.blocksFor(2026, 2) // 2026 Feb = 28 days
        assertEquals(4, blocks.size)
        assertEquals(WeekBlock(4, LocalDate(2026, 2, 22), LocalDate(2026, 2, 28)), blocks[3])
    }

    @Test
    fun `29-day leap February has a one-day block five`() {
        val blocks = MonthWeeks.blocksFor(2028, 2) // 2028 leap = 29 days
        assertEquals(5, blocks.size)
        assertEquals(WeekBlock(5, LocalDate(2028, 2, 29), LocalDate(2028, 2, 29)), blocks[4])
    }

    @Test
    fun `blockContaining finds the block whose range holds the date`() {
        assertEquals(2, MonthWeeks.blockContaining(LocalDate(2026, 7, 10)).index)
        assertEquals(5, MonthWeeks.blockContaining(LocalDate(2026, 7, 31)).index)
        assertEquals(1, MonthWeeks.blockContaining(LocalDate(2026, 7, 1)).index)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew :app:testDebugUnitTest --tests "com.daysync.app.feature.expenses.budget.model.MonthWeeksTest"`
Expected: FAIL — `MonthWeeks` unresolved / compile error.

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.daysync.app.feature.expenses.budget.model

import kotlinx.datetime.LocalDate

data class WeekBlock(val index: Int, val start: LocalDate, val end: LocalDate)

object MonthWeeks {
    fun daysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        else -> 30
    }

    fun blocksFor(year: Int, month: Int): List<WeekBlock> {
        val n = daysInMonth(year, month)
        val startDays = listOf(1, 8, 15, 22, 29).filter { it <= n }
        return startDays.mapIndexed { i, startDay ->
            val endDay = minOf(startDay + 6, n)
            WeekBlock(i + 1, LocalDate(year, month, startDay), LocalDate(year, month, endDay))
        }
    }

    fun blockContaining(date: LocalDate): WeekBlock =
        blocksFor(date.year, date.monthNumber).first { date >= it.start && date <= it.end }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: same command as Step 2.
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/daysync/app/feature/expenses/budget/model/MonthWeeks.kt app/src/test/kotlin/com/daysync/app/feature/expenses/budget/model/MonthWeeksTest.kt
git commit -m "Add MonthWeeks day-of-month block math"
```

---

### Task 2: `BudgetThresholds` alert-decision logic

**Files:**
- Create: `app/src/main/kotlin/com/daysync/app/feature/expenses/budget/BudgetThresholds.kt`
- Test: `app/src/test/kotlin/com/daysync/app/feature/expenses/budget/BudgetThresholdsTest.kt`

**Interfaces:**
- Produces:
  - `data class AlertDecision(val notifyLevel: Int?, val newStoredLevel: Int)`
  - `object BudgetThresholds { val LEVELS: List<Int>; fun crossedLevel(spent: Double, amount: Double): Int; fun evaluate(spent: Double, amount: Double, lastNotified: Int): AlertDecision }`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.daysync.app.feature.expenses.budget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetThresholdsTest {
    @Test
    fun `crossedLevel returns highest reached band`() {
        assertEquals(0, BudgetThresholds.crossedLevel(10.0, 100.0))
        assertEquals(50, BudgetThresholds.crossedLevel(50.0, 100.0))
        assertEquals(75, BudgetThresholds.crossedLevel(80.0, 100.0))
        assertEquals(100, BudgetThresholds.crossedLevel(100.0, 100.0))
        assertEquals(100, BudgetThresholds.crossedLevel(140.0, 100.0))
    }

    @Test
    fun `zero or negative amount never crosses`() {
        assertEquals(0, BudgetThresholds.crossedLevel(50.0, 0.0))
    }

    @Test
    fun `notifies only when crossing a higher band`() {
        assertEquals(AlertDecision(50, 50), BudgetThresholds.evaluate(60.0, 100.0, lastNotified = 0))
        // already notified 50, now at 80 -> notify 75
        assertEquals(AlertDecision(75, 75), BudgetThresholds.evaluate(80.0, 100.0, lastNotified = 50))
        // still at 80, already notified 75 -> no notify, stays 75
        assertEquals(AlertDecision(null, 75), BudgetThresholds.evaluate(80.0, 100.0, lastNotified = 75))
    }

    @Test
    fun `re-arms downward without notifying when spend drops`() {
        // was at 100 (notified 100), an edit drops spend to 60% -> stored re-arms to 50, no notify
        assertEquals(AlertDecision(null, 50), BudgetThresholds.evaluate(60.0, 100.0, lastNotified = 100))
        // then it climbs back to 100 -> notifies 100 again
        assertEquals(AlertDecision(100, 100), BudgetThresholds.evaluate(100.0, 100.0, lastNotified = 50))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew :app:testDebugUnitTest --tests "com.daysync.app.feature.expenses.budget.BudgetThresholdsTest"`
Expected: FAIL — unresolved reference.

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.daysync.app.feature.expenses.budget

data class AlertDecision(val notifyLevel: Int?, val newStoredLevel: Int)

object BudgetThresholds {
    val LEVELS = listOf(50, 75, 100)

    fun crossedLevel(spent: Double, amount: Double): Int {
        if (amount <= 0.0) return 0
        val pct = spent / amount * 100.0
        return LEVELS.filter { pct >= it }.maxOrNull() ?: 0
    }

    fun evaluate(spent: Double, amount: Double, lastNotified: Int): AlertDecision {
        val current = crossedLevel(spent, amount)
        return if (current > lastNotified) AlertDecision(current, current)
        else AlertDecision(null, current) // re-arm downward, never notify on a drop
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: same as Step 2. Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/daysync/app/feature/expenses/budget/BudgetThresholds.kt app/src/test/kotlin/com/daysync/app/feature/expenses/budget/BudgetThresholdsTest.kt
git commit -m "Add budget threshold alert-decision logic"
```

---

### Task 3: `BudgetEntity` + Room registration (v6 → v7)

**Files:**
- Create: `app/src/main/kotlin/com/daysync/app/core/database/entity/BudgetEntity.kt`
- Modify: `app/src/main/kotlin/com/daysync/app/core/database/AppDatabase.kt` (add entity to `entities`, bump `version` to 7, add `abstract fun budgetDao()`)
- Modify: `app/src/main/kotlin/com/daysync/app/core/di/DatabaseModule.kt` (add `MIGRATION_6_7`, register it, add `provideBudgetDao`)
- (BudgetDao created in Task 4; this task references it in AppDatabase/DatabaseModule, so do Task 3 and 4 together before compiling — see Step notes.)

**Interfaces:**
- Produces: `BudgetEntity` with fields:
  `id: String`, `type: String`, `category: String? = null`, `amount: Double`, `recurring: Boolean`, `yearMonth: String? = null`, `weekBlock: Int? = null`, `startDate: LocalDate? = null`, `endDate: LocalDate? = null`, `label: String? = null`, plus `syncStatus`, `lastModified`, `isDeleted` from `SyncableEntity`.

- [ ] **Step 1: Create the entity**

```kotlin
package com.daysync.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.daysync.app.core.sync.SyncStatus
import com.daysync.app.core.sync.SyncableEntity
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

@Entity(
    tableName = "budgets",
    indices = [Index("type"), Index("yearMonth")],
)
data class BudgetEntity(
    @PrimaryKey val id: String,
    val type: String,                 // "MONTHLY" | "WEEKLY" | "CUSTOM"
    val category: String? = null,     // null = overall (reserved for future per-category)
    val amount: Double,
    val recurring: Boolean,           // true = template applied every period; false = specific instance
    val yearMonth: String? = null,    // "YYYY-MM" for month-specific rows and all CUSTOM; null for recurring templates
    val weekBlock: Int? = null,       // 1..5 for WEEKLY rows; null otherwise
    val startDate: LocalDate? = null, // set for CUSTOM only
    val endDate: LocalDate? = null,   // set for CUSTOM only
    val label: String? = null,        // optional label for CUSTOM
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val lastModified: Instant = Clock.System.now(),
    override val isDeleted: Boolean = false,
) : SyncableEntity
```

- [ ] **Step 2: Register in `AppDatabase.kt`**

Add import near the other entity imports (after line 22 area):
```kotlin
import com.daysync.app.core.database.entity.BudgetEntity
```
Add `BudgetEntity::class,` to the `entities` array (extend the `// Expenses (2)` group to `(3)`):
```kotlin
        // Expenses (3)
        ExpenseEntity::class,
        PayeeRuleEntity::class,
        BudgetEntity::class,
```
Change `version = 6,` to `version = 7,`.
Add DAO accessor import and method:
```kotlin
import com.daysync.app.core.database.dao.BudgetDao
```
```kotlin
    // Expenses
    abstract fun expenseDao(): ExpenseDao
    abstract fun payeeRuleDao(): PayeeRuleDao
    abstract fun budgetDao(): BudgetDao
```

- [ ] **Step 3: Add migration + DI provider in `DatabaseModule.kt`**

Add below `MIGRATION_5_6` (after line 40):
```kotlin
    // v6 → v7: add budgets table
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `budgets` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `type` TEXT NOT NULL,
                    `category` TEXT,
                    `amount` REAL NOT NULL,
                    `recurring` INTEGER NOT NULL,
                    `yearMonth` TEXT,
                    `weekBlock` INTEGER,
                    `startDate` TEXT,
                    `endDate` TEXT,
                    `label` TEXT,
                    `syncStatus` TEXT NOT NULL,
                    `lastModified` INTEGER NOT NULL,
                    `isDeleted` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_type` ON `budgets` (`type`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_yearMonth` ON `budgets` (`yearMonth`)")
        }
    }
```
Register it in the builder (line 50):
```kotlin
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
```
Add DAO import and provider (near the expense DAO providers, ~line 71):
```kotlin
import com.daysync.app.core.database.dao.BudgetDao
```
```kotlin
    @Provides fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()
```

- [ ] **Step 4: (Do Task 4 now, then) compile**

`BudgetDao` must exist before this compiles. Complete Task 4 Step 1 (create `BudgetDao`), then run:
Run: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew :app:kspDebugKotlin`
Expected: BUILD SUCCESSFUL, and a new schema file `app/schemas/com.daysync.app.core.database.AppDatabase/7.json` is generated.

- [ ] **Step 5: Commit** (bundled with Task 4)

---

### Task 4: `BudgetDao`

**Files:**
- Create: `app/src/main/kotlin/com/daysync/app/core/database/dao/BudgetDao.kt`
- Modify: `app/src/main/kotlin/com/daysync/app/core/database/dao/ExpenseDao.kt` (add suspend one-shot range sum)

**Interfaces:**
- Consumes: `BudgetEntity` (Task 3).
- Produces:
  - `BudgetDao`: `upsert(entity)`, `upsertAll(entities)`, `getAllActive(): Flow<List<BudgetEntity>>`, `getAllActiveList(): List<BudgetEntity>`, `getById(id): BudgetEntity?`, `softDelete(id, now)`, `getPendingSync(): List<BudgetEntity>`, `markAsSynced(ids)`, `deleteRecurringWeeklyBlocks()`, `deletePerMonthWeeklyBlocks(yearMonth)`.
  - `ExpenseDao.getTotalInRangeOnce(startDate, endDate): Double` (suspend).

- [ ] **Step 1: Create `BudgetDao`**

```kotlin
package com.daysync.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.daysync.app.core.database.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Upsert
    suspend fun upsert(entity: BudgetEntity)

    @Upsert
    suspend fun upsertAll(entities: List<BudgetEntity>)

    @Query("SELECT * FROM budgets WHERE isDeleted = 0")
    fun getAllActive(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE isDeleted = 0")
    suspend fun getAllActiveList(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getById(id: String): BudgetEntity?

    @Query("UPDATE budgets SET isDeleted = 1, syncStatus = 'PENDING', lastModified = :nowMillis WHERE id = :id")
    suspend fun softDelete(id: String, nowMillis: Long)

    @Query("SELECT * FROM budgets WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<BudgetEntity>

    @Query("UPDATE budgets SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)

    // Hard-deletes for replacing weekly patterns (these rows are transient config,
    // superseded wholesale; soft-delete would leave stale resolved rows).
    @Query("DELETE FROM budgets WHERE type = 'WEEKLY' AND recurring = 1 AND weekBlock IS NOT NULL")
    suspend fun deleteRecurringWeeklyBlocks()

    @Query("DELETE FROM budgets WHERE type = 'WEEKLY' AND recurring = 0 AND yearMonth = :yearMonth")
    suspend fun deletePerMonthWeeklyBlocks(yearMonth: String)
}
```

Note: `lastModified` stores epoch millis (the `Converters.fromInstant` converter maps `Instant` ↔ `Long`), so `softDelete` takes `nowMillis: Long`.

- [ ] **Step 2: Add suspend range sum to `ExpenseDao.kt`**

Add after `getMonthlyTotal` (line 63):
```kotlin
    @Query(
        "SELECT COALESCE(SUM(totalAmount), 0.0) FROM expenses " +
            "WHERE date >= :startDate AND date <= :endDate AND isDeleted = 0"
    )
    suspend fun getTotalInRangeOnce(startDate: LocalDate, endDate: LocalDate): Double
```

- [ ] **Step 3: Compile (this covers Task 3 Step 4 too)**

Run: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew :app:kspDebugKotlin`
Expected: BUILD SUCCESSFUL; `app/schemas/.../7.json` generated.

- [ ] **Step 4: Commit (Tasks 3 + 4 together)**

```bash
git add app/src/main/kotlin/com/daysync/app/core/database/entity/BudgetEntity.kt \
  app/src/main/kotlin/com/daysync/app/core/database/dao/BudgetDao.kt \
  app/src/main/kotlin/com/daysync/app/core/database/dao/ExpenseDao.kt \
  app/src/main/kotlin/com/daysync/app/core/database/AppDatabase.kt \
  app/src/main/kotlin/com/daysync/app/core/di/DatabaseModule.kt \
  app/schemas/com.daysync.app.core.database.AppDatabase/7.json
git commit -m "Add budgets table, DAO, and v6->v7 migration"
```

---

### Task 5: `BudgetResolver` + resolved-budget model

**Files:**
- Create: `app/src/main/kotlin/com/daysync/app/feature/expenses/budget/model/ResolvedBudget.kt`
- Create: `app/src/main/kotlin/com/daysync/app/feature/expenses/budget/BudgetResolver.kt`
- Test: `app/src/test/kotlin/com/daysync/app/feature/expenses/budget/BudgetResolverTest.kt`

**Interfaces:**
- Consumes: `BudgetEntity` (Task 3), `MonthWeeks`/`WeekBlock` (Task 1).
- Produces:
  - `enum class BudgetKind { MONTHLY, WEEKLY, CUSTOM }`
  - `data class ResolvedBudget(val instanceKey: String, val kind: BudgetKind, val label: String, val start: LocalDate, val end: LocalDate, val amount: Double)`
  - `object BudgetResolver { fun monthlyFor(budgets, year, month): ResolvedBudget?; fun weeklyBlocksFor(budgets, year, month): List<ResolvedBudget>; fun customFor(budgets, year, month): List<ResolvedBudget>; fun coveringDate(budgets, date): List<ResolvedBudget> }`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.daysync.app.feature.expenses.budget

import com.daysync.app.core.database.entity.BudgetEntity
import com.daysync.app.core.database.entity.BudgetEntity as B
import com.daysync.app.feature.expenses.budget.model.BudgetKind
import com.daysync.app.core.sync.SyncStatus
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetResolverTest {
    private fun budget(
        id: String, type: String, amount: Double, recurring: Boolean,
        yearMonth: String? = null, weekBlock: Int? = null,
        start: LocalDate? = null, end: LocalDate? = null, label: String? = null,
    ) = BudgetEntity(
        id = id, type = type, amount = amount, recurring = recurring,
        yearMonth = yearMonth, weekBlock = weekBlock, startDate = start, endDate = end, label = label,
        syncStatus = SyncStatus.SYNCED, lastModified = Instant.fromEpochMilliseconds(0L),
    )

    @Test
    fun `monthly override wins over recurring monthly`() {
        val budgets = listOf(
            budget("r", "MONTHLY", 40000.0, recurring = true),
            budget("o", "MONTHLY", 50000.0, recurring = false, yearMonth = "2026-07"),
        )
        val r = BudgetResolver.monthlyFor(budgets, 2026, 7)!!
        assertEquals(50000.0, r.amount, 0.0)
        assertEquals("MONTHLY:2026-07", r.instanceKey)
        assertEquals(LocalDate(2026, 7, 1), r.start)
        assertEquals(LocalDate(2026, 7, 31), r.end)
    }

    @Test
    fun `recurring monthly used when no override`() {
        val budgets = listOf(budget("r", "MONTHLY", 40000.0, recurring = true))
        assertEquals(40000.0, BudgetResolver.monthlyFor(budgets, 2026, 8)!!.amount, 0.0)
    }

    @Test
    fun `weekly resolution precedence per-month override then pattern then flat`() {
        val budgets = listOf(
            budget("flat", "WEEKLY", 10000.0, recurring = true),                 // flat cap
            budget("pat2", "WEEKLY", 12000.0, recurring = true, weekBlock = 2),   // recurring pattern block 2
            budget("ovr2", "WEEKLY", 15000.0, recurring = false, yearMonth = "2026-07", weekBlock = 2),
        )
        val blocks = BudgetResolver.weeklyBlocksFor(budgets, 2026, 7)
        // block 1 -> flat, block 2 -> per-month override, block 3 -> flat
        assertEquals(10000.0, blocks.first { it.instanceKey == "WEEKLY:2026-07:1" }.amount, 0.0)
        assertEquals(15000.0, blocks.first { it.instanceKey == "WEEKLY:2026-07:2" }.amount, 0.0)
        assertEquals(10000.0, blocks.first { it.instanceKey == "WEEKLY:2026-07:3" }.amount, 0.0)
    }

    @Test
    fun `coveringDate returns monthly plus containing block plus containing customs`() {
        val budgets = listOf(
            budget("m", "MONTHLY", 40000.0, recurring = true),
            budget("w", "WEEKLY", 10000.0, recurring = true),
            budget("c", "CUSTOM", 8000.0, recurring = false, yearMonth = "2026-07",
                start = LocalDate(2026, 7, 5), end = LocalDate(2026, 7, 12), label = "Trip"),
        )
        val covering = BudgetResolver.coveringDate(budgets, LocalDate(2026, 7, 10))
        val keys = covering.map { it.instanceKey }.toSet()
        assertEquals(setOf("MONTHLY:2026-07", "WEEKLY:2026-07:2", "CUSTOM:c"), keys)
    }

    @Test
    fun `no monthly budget yields null`() {
        assertNull(BudgetResolver.monthlyFor(emptyList(), 2026, 7))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew :app:testDebugUnitTest --tests "com.daysync.app.feature.expenses.budget.BudgetResolverTest"`
Expected: FAIL — unresolved references.

- [ ] **Step 3: Write the model + resolver**

`ResolvedBudget.kt`:
```kotlin
package com.daysync.app.feature.expenses.budget.model

import kotlinx.datetime.LocalDate

enum class BudgetKind { MONTHLY, WEEKLY, CUSTOM }

data class ResolvedBudget(
    val instanceKey: String, // stable dedup key: "MONTHLY:2026-07", "WEEKLY:2026-07:2", "CUSTOM:<id>"
    val kind: BudgetKind,
    val label: String,
    val start: LocalDate,
    val end: LocalDate,
    val amount: Double,
)
```

`BudgetResolver.kt`:
```kotlin
package com.daysync.app.feature.expenses.budget

import com.daysync.app.core.database.entity.BudgetEntity
import com.daysync.app.feature.expenses.budget.model.BudgetKind
import com.daysync.app.feature.expenses.budget.model.MonthWeeks
import com.daysync.app.feature.expenses.budget.model.ResolvedBudget
import kotlinx.datetime.LocalDate

object BudgetResolver {

    private fun ym(year: Int, month: Int): String = "%04d-%02d".format(year, month)

    private fun monthLabel(month: Int): String = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )[month - 1]

    fun monthlyFor(budgets: List<BudgetEntity>, year: Int, month: Int): ResolvedBudget? {
        val key = ym(year, month)
        val monthlies = budgets.filter { it.type == "MONTHLY" }
        val chosen = monthlies.firstOrNull { !it.recurring && it.yearMonth == key }
            ?: monthlies.firstOrNull { it.recurring }
            ?: return null
        val n = MonthWeeks.daysInMonth(year, month)
        return ResolvedBudget(
            instanceKey = "MONTHLY:$key",
            kind = BudgetKind.MONTHLY,
            label = "${monthLabel(month)} $year",
            start = LocalDate(year, month, 1),
            end = LocalDate(year, month, n),
            amount = chosen.amount,
        )
    }

    fun weeklyBlocksFor(budgets: List<BudgetEntity>, year: Int, month: Int): List<ResolvedBudget> {
        val key = ym(year, month)
        val weeklies = budgets.filter { it.type == "WEEKLY" }
        val perMonth = weeklies.filter { !it.recurring && it.yearMonth == key && it.weekBlock != null }
            .associateBy { it.weekBlock!! }
        val pattern = weeklies.filter { it.recurring && it.weekBlock != null }
            .associateBy { it.weekBlock!! }
        val flat = weeklies.firstOrNull { it.recurring && it.weekBlock == null }

        return MonthWeeks.blocksFor(year, month).mapNotNull { block ->
            val amount = (perMonth[block.index] ?: pattern[block.index] ?: flat)?.amount ?: return@mapNotNull null
            ResolvedBudget(
                instanceKey = "WEEKLY:$key:${block.index}",
                kind = BudgetKind.WEEKLY,
                label = "${monthLabel(month).take(3)} ${block.start.dayOfMonth}–${block.end.dayOfMonth}",
                start = block.start,
                end = block.end,
                amount = amount,
            )
        }
    }

    fun customFor(budgets: List<BudgetEntity>, year: Int, month: Int): List<ResolvedBudget> {
        val key = ym(year, month)
        return budgets.filter { it.type == "CUSTOM" && it.yearMonth == key && it.startDate != null && it.endDate != null }
            .map {
                ResolvedBudget(
                    instanceKey = "CUSTOM:${it.id}",
                    kind = BudgetKind.CUSTOM,
                    label = it.label ?: "${it.startDate} – ${it.endDate}",
                    start = it.startDate!!,
                    end = it.endDate!!,
                    amount = it.amount,
                )
            }
    }

    fun coveringDate(budgets: List<BudgetEntity>, date: LocalDate): List<ResolvedBudget> {
        val year = date.year
        val month = date.monthNumber
        val result = mutableListOf<ResolvedBudget>()
        monthlyFor(budgets, year, month)?.let { result += it }
        weeklyBlocksFor(budgets, year, month).firstOrNull { date >= it.start && date <= it.end }?.let { result += it }
        result += customFor(budgets, year, month).filter { date >= it.start && date <= it.end }
        return result
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: same as Step 2. Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/daysync/app/feature/expenses/budget/model/ResolvedBudget.kt \
  app/src/main/kotlin/com/daysync/app/feature/expenses/budget/BudgetResolver.kt \
  app/src/test/kotlin/com/daysync/app/feature/expenses/budget/BudgetResolverTest.kt
git commit -m "Add budget resolver with monthly/weekly/custom precedence"
```

---

### Task 6: Budget sync (DTO, mapper, engine step, restore)

**Files:**
- Create: `app/src/main/kotlin/com/daysync/app/core/sync/dto/BudgetDto.kt`
- Modify: `app/src/main/kotlin/com/daysync/app/core/sync/mapper/SyncMappers.kt` (add `BudgetEntity.toDto()`)
- Modify: `app/src/main/kotlin/com/daysync/app/core/sync/DaySyncEngine.kt` (inject `budgetDao`, add `syncBudgets`, register step, bump `TOTAL_TABLES` 16 → 17)
- Modify: `app/src/main/kotlin/com/daysync/app/core/sync/SyncRestoreEngine.kt` (inject `budgetDao`, add `restoreBudgets` + `BudgetRow`, register step)
- Create: `supabase/migrations/004_add_budgets.sql`

**Interfaces:**
- Consumes: `BudgetEntity` (Task 3), `BudgetDao` (Task 4).
- Produces: `BudgetDto`; `BudgetEntity.toDto()`; Supabase `budgets` table.

- [ ] **Step 1: Create `BudgetDto`**

```kotlin
package com.daysync.app.core.sync.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BudgetDto(
    val id: String,
    val type: String,
    val category: String? = null,
    val amount: Double,
    val recurring: Boolean,
    @SerialName("year_month") val yearMonth: String? = null,
    @SerialName("week_block") val weekBlock: Int? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    val label: String? = null,
    @SerialName("last_modified") val lastModified: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
)
```

- [ ] **Step 2: Add mapper in `SyncMappers.kt`**

Add after the `ExpenseEntity.toDto()` block (~line 171). Add import at top:
```kotlin
import com.daysync.app.core.database.entity.BudgetEntity
import com.daysync.app.core.sync.dto.BudgetDto
```
```kotlin
// Budgets

fun BudgetEntity.toDto() = BudgetDto(
    id = id,
    type = type,
    category = category,
    amount = amount,
    recurring = recurring,
    yearMonth = yearMonth,
    weekBlock = weekBlock,
    startDate = startDate?.toString(),
    endDate = endDate?.toString(),
    label = label,
    lastModified = lastModified.toEpochMilliseconds(),
    isDeleted = isDeleted,
)
```

- [ ] **Step 3: Add sync step in `DaySyncEngine.kt`**

Add constructor param (after `expenseDao`, line 66):
```kotlin
    private val budgetDao: com.daysync.app.core.database.dao.BudgetDao,
```
Add step to `syncSteps` after the `"expenses"` line (line 100):
```kotlin
            "budgets" to ::syncBudgets,
```
Add the function next to `syncExpenses` (after line 238):
```kotlin
    private suspend fun syncBudgets(): Result<Unit> = runCatching {
        val pending = budgetDao.getPendingSync()
        if (pending.isEmpty()) { logSync("budgets", 0, "SUCCESS"); return@runCatching }
        val dtos = pending.map { it.toDto() }
        upsertChunked("budgets", dtos)
        markSyncedChunked(pending.map { it.id }) { budgetDao.markAsSynced(it) }
        logSync("budgets", pending.size, "SUCCESS")
    }.onFailure { logSync("budgets", 0, "FAILED"); Log.e(TAG, "syncBudgets failed", it) }
```
Bump `TOTAL_TABLES` (line 373): `private const val TOTAL_TABLES = 17`.

- [ ] **Step 4: Add restore in `SyncRestoreEngine.kt`**

Inject `budgetDao` into the constructor (match existing DAO injection style). Register the step after `"expenses"` (line 71):
```kotlin
            "budgets" to ::restoreBudgets,
```
Add the restore function (near `restoreExpenses`, ~line 141):
```kotlin
    private suspend fun restoreBudgets(): Int {
        val rows = fetchAll<BudgetRow>("budgets")
        val entities = rows.map { it.toEntity() }
        if (entities.isNotEmpty()) budgetDao.upsertAll(entities)
        return entities.size
    }
```
Add the row type next to `ExpenseRow` (~line 227):
```kotlin
    @Serializable
    data class BudgetRow(
        val id: String,
        val type: String,
        val category: String? = null,
        val amount: Double,
        val recurring: Boolean,
        @SerialName("year_month") val yearMonth: String? = null,
        @SerialName("week_block") val weekBlock: Int? = null,
        @SerialName("start_date") val startDate: String? = null,
        @SerialName("end_date") val endDate: String? = null,
        val label: String? = null,
        @SerialName("last_modified") val lastModified: Long,
    ) {
        fun toEntity() = com.daysync.app.core.database.entity.BudgetEntity(
            id = id, type = type, category = category, amount = amount, recurring = recurring,
            yearMonth = yearMonth, weekBlock = weekBlock,
            startDate = startDate?.let { LocalDate.parse(it) },
            endDate = endDate?.let { LocalDate.parse(it) },
            label = label,
            syncStatus = SyncStatus.SYNCED,
            lastModified = Instant.fromEpochMilliseconds(lastModified),
        )
    }
```

- [ ] **Step 5: Create Supabase migration `supabase/migrations/004_add_budgets.sql`**

```sql
-- 004_add_budgets.sql — budgets table for Expenses budgeting feature
create table if not exists public.budgets (
    id text primary key,
    type text not null,
    category text,
    amount double precision not null,
    recurring boolean not null,
    year_month text,
    week_block integer,
    start_date text,
    end_date text,
    label text,
    last_modified bigint not null,
    is_deleted boolean not null default false
);

create index if not exists idx_budgets_type on public.budgets (type);
create index if not exists idx_budgets_year_month on public.budgets (year_month);
```

- [ ] **Step 6: Compile**

Run: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/kotlin/com/daysync/app/core/sync/dto/BudgetDto.kt \
  app/src/main/kotlin/com/daysync/app/core/sync/mapper/SyncMappers.kt \
  app/src/main/kotlin/com/daysync/app/core/sync/DaySyncEngine.kt \
  app/src/main/kotlin/com/daysync/app/core/sync/SyncRestoreEngine.kt \
  supabase/migrations/004_add_budgets.sql
git commit -m "Sync and restore budgets to Supabase"
```

> **Deployment note (not a code step):** Apply `004_add_budgets.sql` in the Supabase SQL Editor **before** installing a build that syncs budgets, or the upload step will fail (per project policy).

---

### Task 7: `BudgetAlertStore` (device-local dedup)

**Files:**
- Create: `app/src/main/kotlin/com/daysync/app/feature/expenses/budget/BudgetAlertStore.kt`
- Test: `app/src/test/kotlin/com/daysync/app/feature/expenses/budget/BudgetAlertStoreTest.kt`

**Interfaces:**
- Produces: `class BudgetAlertStore @Inject constructor(@ApplicationContext context)` with `fun getLevel(key: String): Int`, `fun setLevel(key: String, level: Int)`, `fun keys(): Set<String>`, `fun prune(validKeys: Set<String>)`.

- [ ] **Step 1: Write the failing test** (uses Robolectric-free approach: inject a fake via an interface)

To keep this JVM-unit-testable without Android, define a tiny interface and test the pruning/level logic against an in-memory implementation.

```kotlin
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew :app:testDebugUnitTest --tests "com.daysync.app.feature.expenses.budget.BudgetAlertStoreTest"`
Expected: FAIL — `BudgetAlertLevels` unresolved.

- [ ] **Step 3: Write implementation**

```kotlin
package com.daysync.app.feature.expenses.budget

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface BudgetAlertLevels {
    fun getLevel(key: String): Int
    fun setLevel(key: String, level: Int)
    fun keys(): Set<String>
    fun prune(validKeys: Set<String>)
}

/**
 * Device-local record of the highest alert threshold already fired per budget instance.
 * Never synced — alerts are per-device. Keys are ResolvedBudget.instanceKey.
 */
@Singleton
class BudgetAlertStore @Inject constructor(
    @ApplicationContext context: Context,
) : BudgetAlertLevels {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("daysync_budget_alerts", Context.MODE_PRIVATE)

    override fun getLevel(key: String): Int = prefs.getInt(key, 0)

    override fun setLevel(key: String, level: Int) {
        prefs.edit().putInt(key, level).apply()
    }

    override fun keys(): Set<String> = prefs.all.keys.toSet()

    override fun prune(validKeys: Set<String>) {
        val editor = prefs.edit()
        for (k in prefs.all.keys) if (k !in validKeys) editor.remove(k)
        editor.apply()
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: same as Step 2. Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/daysync/app/feature/expenses/budget/BudgetAlertStore.kt \
  app/src/test/kotlin/com/daysync/app/feature/expenses/budget/BudgetAlertStoreTest.kt
git commit -m "Add device-local budget alert dedup store"
```

---

### Task 8: `BudgetAlertEvaluator` + notification channel

**Files:**
- Create: `app/src/main/kotlin/com/daysync/app/feature/expenses/budget/BudgetNotificationChannel.kt`
- Create: `app/src/main/kotlin/com/daysync/app/feature/expenses/budget/BudgetAlertEvaluator.kt`
- Test: `app/src/test/kotlin/com/daysync/app/feature/expenses/budget/BudgetAlertEvaluatorTest.kt`

**Interfaces:**
- Consumes: `BudgetDao` (Task 4), `ExpenseDao.getTotalInRangeOnce` (Task 4), `BudgetResolver` (Task 5), `BudgetThresholds` (Task 2), `BudgetAlertLevels`/`BudgetAlertStore` (Task 7), `UserPreferences`.
- Produces: `class BudgetAlertEvaluator` with `suspend fun onExpenseChanged(dates: Set<LocalDate>)` and an internal `suspend fun evaluate(date: LocalDate, post: (ResolvedBudget, Int) -> Unit)`. `post` is separated so tests can assert without Android notifications.

- [ ] **Step 1: Write the failing test** (verifies evaluation + dedup, no Android)

```kotlin
package com.daysync.app.feature.expenses.budget

import com.daysync.app.core.database.dao.BudgetDao
import com.daysync.app.core.database.dao.ExpenseDao
import com.daysync.app.core.database.entity.BudgetEntity
import com.daysync.app.core.sync.SyncStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetAlertEvaluatorTest {
    private fun monthly(amount: Double) = BudgetEntity(
        id = "m", type = "MONTHLY", amount = amount, recurring = true,
        syncStatus = SyncStatus.SYNCED, lastModified = Instant.fromEpochMilliseconds(0L),
    )

    @Test
    fun `fires 75 alert once then re-arms after a delete drops spend`() = runTest {
        val budgetDao = mockk<BudgetDao>()
        val expenseDao = mockk<ExpenseDao>()
        val store = InMemoryLevels()
        coEvery { budgetDao.getAllActiveList() } returns listOf(monthly(40000.0))

        val evaluator = BudgetAlertEvaluator(budgetDao, expenseDao, store) { }

        // 80% -> notify 75
        coEvery { expenseDao.getTotalInRangeOnce(any(), any()) } returns 32000.0
        val fired1 = mutableListOf<Pair<String, Int>>()
        evaluator.evaluate(LocalDate(2026, 7, 10)) { rb, lvl -> fired1 += rb.instanceKey to lvl }
        assertEquals(listOf("MONTHLY:2026-07" to 75), fired1)

        // still 80% -> no notify
        val fired2 = mutableListOf<Pair<String, Int>>()
        evaluator.evaluate(LocalDate(2026, 7, 10)) { rb, lvl -> fired2 += rb.instanceKey to lvl }
        assertEquals(emptyList<Pair<String, Int>>(), fired2)

        // delete drops to 40% -> re-arm to 50, no notify
        coEvery { expenseDao.getTotalInRangeOnce(any(), any()) } returns 16000.0
        evaluator.evaluate(LocalDate(2026, 7, 10)) { _, _ -> }
        assertEquals(50, store.getLevel("MONTHLY:2026-07"))
    }

    private class InMemoryLevels : BudgetAlertLevels {
        val m = mutableMapOf<String, Int>()
        override fun getLevel(key: String) = m[key] ?: 0
        override fun setLevel(key: String, level: Int) { m[key] = level }
        override fun keys() = m.keys.toSet()
        override fun prune(validKeys: Set<String>) { m.keys.retainAll(validKeys) }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew :app:testDebugUnitTest --tests "com.daysync.app.feature.expenses.budget.BudgetAlertEvaluatorTest"`
Expected: FAIL — unresolved reference.

- [ ] **Step 3: Write the channel**

```kotlin
package com.daysync.app.feature.expenses.budget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object BudgetNotificationChannel {
    const val CHANNEL_ID = "budget_alerts"
    private const val CHANNEL_NAME = "Budget Alerts"
    private const val CHANNEL_DESCRIPTION = "Alerts when you reach 50/75/100% of a budget"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = CHANNEL_DESCRIPTION }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
```

- [ ] **Step 4: Write the evaluator**

Note the constructor's last parameter `post` defaults to real notification posting; tests pass a no-op or a capturing lambda. The `evaluate(date, onFire)` method is pure of Android (takes an `onFire` callback), which the test drives directly.

```kotlin
package com.daysync.app.feature.expenses.budget

import com.daysync.app.core.database.dao.BudgetDao
import com.daysync.app.core.database.dao.ExpenseDao
import com.daysync.app.feature.expenses.budget.model.ResolvedBudget
import kotlinx.datetime.LocalDate
import javax.inject.Inject

class BudgetAlertEvaluator @Inject constructor(
    private val budgetDao: BudgetDao,
    private val expenseDao: ExpenseDao,
    private val alertLevels: BudgetAlertLevels,
    private val post: (ResolvedBudget, Int) -> Unit,
) {
    /** Re-evaluate every budget instance covering each changed date. */
    suspend fun onExpenseChanged(dates: Set<LocalDate>) {
        for (date in dates) evaluate(date) { rb, level -> post(rb, level) }
    }

    /** Android-free core: resolve covering budgets, decide alerts, update dedup store. */
    suspend fun evaluate(date: LocalDate, onFire: (ResolvedBudget, Int) -> Unit) {
        val budgets = budgetDao.getAllActiveList()
        val covering = BudgetResolver.coveringDate(budgets, date)
        for (rb in covering) {
            val spent = expenseDao.getTotalInRangeOnce(rb.start, rb.end)
            val last = alertLevels.getLevel(rb.instanceKey)
            val decision = BudgetThresholds.evaluate(spent, rb.amount, last)
            alertLevels.setLevel(rb.instanceKey, decision.newStoredLevel)
            decision.notifyLevel?.let { onFire(rb, it) }
        }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: same as Step 2. Expected: PASS (1 test).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/com/daysync/app/feature/expenses/budget/BudgetNotificationChannel.kt \
  app/src/main/kotlin/com/daysync/app/feature/expenses/budget/BudgetAlertEvaluator.kt \
  app/src/test/kotlin/com/daysync/app/feature/expenses/budget/BudgetAlertEvaluatorTest.kt
git commit -m "Add budget alert evaluator and notification channel"
```

---

### Task 8b: Wire the evaluator's notification posting + DI

**Files:**
- Create: `app/src/main/kotlin/com/daysync/app/feature/expenses/budget/di/BudgetModule.kt`

**Interfaces:**
- Consumes: `BudgetAlertEvaluator` (Task 8), `BudgetNotificationChannel` (Task 8), `UserPreferences`, `BudgetAlertStore` (Task 7).
- Produces: a Hilt `@Provides` for `BudgetAlertEvaluator` whose `post` lambda builds and shows a notification.

- [ ] **Step 1: Create the module**

```kotlin
package com.daysync.app.feature.expenses.budget.di

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.daysync.app.core.config.UserPreferences
import com.daysync.app.core.database.dao.BudgetDao
import com.daysync.app.core.database.dao.ExpenseDao
import com.daysync.app.feature.expenses.budget.BudgetAlertEvaluator
import com.daysync.app.feature.expenses.budget.BudgetAlertStore
import com.daysync.app.feature.expenses.budget.BudgetNotificationChannel
import com.daysync.app.feature.expenses.budget.model.ResolvedBudget
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BudgetModule {

    @Provides
    @Singleton
    fun provideBudgetAlertEvaluator(
        @ApplicationContext context: Context,
        budgetDao: BudgetDao,
        expenseDao: ExpenseDao,
        alertStore: BudgetAlertStore,
        userPreferences: UserPreferences,
    ): BudgetAlertEvaluator {
        val post: (ResolvedBudget, Int) -> Unit = { rb, level ->
            BudgetNotificationChannel.createChannel(context)
            val title = when (level) {
                100 -> "Budget reached: ${rb.label}"
                else -> "$level% of budget used: ${rb.label}"
            }
            val body = if (level >= 100) {
                "You've hit your ${userPreferences.formatCurrency(rb.amount)} budget for ${rb.label}."
            } else {
                "You've used $level% of your ${userPreferences.formatCurrency(rb.amount)} budget for ${rb.label}."
            }
            val notification = NotificationCompat.Builder(context, BudgetNotificationChannel.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .build()
            val id = ("budget_" + rb.instanceKey + "_" + level).hashCode()
            context.getSystemService(NotificationManager::class.java).notify(id, notification)
        }
        return BudgetAlertEvaluator(budgetDao, expenseDao, alertStore, post)
    }
}
```

> Note: the notification copy uses the budget amount and percent (not the exact remaining rupees) to avoid a second DB read in the `post` lambda; the banner shows exact money-left. This is intentional and sufficient.

- [ ] **Step 2: Compile**

Run: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/com/daysync/app/feature/expenses/budget/di/BudgetModule.kt
git commit -m "Wire budget alert notification posting via Hilt"
```

---

### Task 9: Trigger evaluation from expense mutations

**Files:**
- Modify: `app/src/main/kotlin/com/daysync/app/feature/expenses/data/ExpenseRepositoryImpl.kt`
- Modify: `app/src/main/kotlin/com/daysync/app/feature/expenses/di/ExpenseModule.kt` (pass evaluator into repo)

**Interfaces:**
- Consumes: `BudgetAlertEvaluator` (Task 8/8b).
- Produces: every expense write path calls `budgetAlertEvaluator.onExpenseChanged(...)`.

- [ ] **Step 1: Inject evaluator into the repo impl**

Change the constructor:
```kotlin
class ExpenseRepositoryImpl(
    private val expenseDao: ExpenseDao,
    private val payeeRuleDao: PayeeRuleDao,
    private val deduplicator: TransactionDeduplicator,
    private val userPreferences: com.daysync.app.core.config.UserPreferences,
    private val budgetAlertEvaluator: com.daysync.app.feature.expenses.budget.BudgetAlertEvaluator,
) : ExpenseRepository {
```

- [ ] **Step 2: Call the evaluator after each write**

`addExpense`:
```kotlin
    override suspend fun addExpense(expense: Expense) {
        expenseDao.insert(expense.toEntity())
        budgetAlertEvaluator.onExpenseChanged(setOf(expense.date))
    }
```
`updateExpense` (evaluate both old and new dates, since an edit can move the date):
```kotlin
    override suspend fun updateExpense(expense: Expense) {
        val oldDate = expenseDao.getById(expense.id)?.date
        expenseDao.update(expense.toEntity())
        budgetAlertEvaluator.onExpenseChanged(setOfNotNull(oldDate, expense.date))
    }
```
`deleteExpense`:
```kotlin
    override suspend fun deleteExpense(id: String) {
        val entity = expenseDao.getById(id) ?: return
        expenseDao.update(
            entity.copy(
                isDeleted = true,
                syncStatus = SyncStatus.PENDING,
                lastModified = Clock.System.now(),
            )
        )
        budgetAlertEvaluator.onExpenseChanged(setOf(entity.date))
    }
```
`processNotification` — after `expenseDao.insert(expense)` (both branches share the insert at line 112):
```kotlin
        expenseDao.insert(expense)
        budgetAlertEvaluator.onExpenseChanged(setOf(expense.date))
```
`importFromCsv` — after the loop, evaluate the affected dates in one batch. Collect dates during the loop into a `val touched = mutableSetOf<LocalDate>()`, add `touched += entry.date` right after a successful `expenseDao.insert(entity)`, then after the loop:
```kotlin
        if (touched.isNotEmpty()) budgetAlertEvaluator.onExpenseChanged(touched)
```
`saveFromReceipt` — after `expenseDao.insert(entity)` (line 191):
```kotlin
        expenseDao.insert(entity)
        budgetAlertEvaluator.onExpenseChanged(setOf(date))
```

- [ ] **Step 3: Update DI provider in `ExpenseModule.kt`**

```kotlin
    @Provides
    @Singleton
    fun provideExpenseRepository(
        expenseDao: ExpenseDao,
        payeeRuleDao: PayeeRuleDao,
        deduplicator: TransactionDeduplicator,
        userPreferences: com.daysync.app.core.config.UserPreferences,
        budgetAlertEvaluator: com.daysync.app.feature.expenses.budget.BudgetAlertEvaluator,
    ): ExpenseRepository {
        return ExpenseRepositoryImpl(expenseDao, payeeRuleDao, deduplicator, userPreferences, budgetAlertEvaluator)
    }
```

- [ ] **Step 4: Compile**

Run: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/daysync/app/feature/expenses/data/ExpenseRepositoryImpl.kt \
  app/src/main/kotlin/com/daysync/app/feature/expenses/di/ExpenseModule.kt
git commit -m "Recompute budget alerts on every expense mutation"
```

---

### Task 10: `BudgetCheckWorker` daily safety net

**Files:**
- Create: `app/src/main/kotlin/com/daysync/app/feature/expenses/budget/BudgetCheckWorker.kt`
- Modify: `app/src/main/kotlin/com/daysync/app/DaySyncApplication.kt` (create channel + schedule worker)

**Interfaces:**
- Consumes: `BudgetAlertEvaluator` (Task 8), `UserPreferences`.
- Produces: a periodic worker re-evaluating today's budgets and pruning stale dedup keys.

- [ ] **Step 1: Create the worker**

```kotlin
package com.daysync.app.feature.expenses.budget

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.daysync.app.core.config.UserPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.time.Clock
import kotlinx.datetime.todayIn

@HiltWorker
class BudgetCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val evaluator: BudgetAlertEvaluator,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = UserPreferences(applicationContext)
        val today = Clock.System.todayIn(prefs.kotlinTimeZone)
        evaluator.onExpenseChanged(setOf(today))
        return Result.success()
    }
}
```

- [ ] **Step 2: Schedule it + create channel in `DaySyncApplication.kt`**

Add imports:
```kotlin
import com.daysync.app.feature.expenses.budget.BudgetCheckWorker
import com.daysync.app.feature.expenses.budget.BudgetNotificationChannel
```
In `onCreate()` after `ExpenseNotificationChannel.createChannel(this)`:
```kotlin
        BudgetNotificationChannel.createChannel(this)
        scheduleBudgetCheck()
```
Add the scheduling method (mirrors `scheduleDailySync`, runs every 24h with no network constraint):
```kotlin
    private fun scheduleBudgetCheck() {
        val request = PeriodicWorkRequestBuilder<BudgetCheckWorker>(24, TimeUnit.HOURS)
            .addTag("budget_check")
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daysync_budget_check",
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
```

- [ ] **Step 3: Compile**

Run: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/daysync/app/feature/expenses/budget/BudgetCheckWorker.kt \
  app/src/main/kotlin/com/daysync/app/DaySyncApplication.kt
git commit -m "Add daily budget-check safety-net worker"
```

---

### Task 11: `BudgetRepository` (CRUD + summary flow)

**Files:**
- Create: `app/src/main/kotlin/com/daysync/app/feature/expenses/budget/data/BudgetRepository.kt`
- Create: `app/src/main/kotlin/com/daysync/app/feature/expenses/budget/data/BudgetRepositoryImpl.kt`
- Create: `app/src/main/kotlin/com/daysync/app/feature/expenses/budget/model/BudgetSummary.kt`
- Modify: `app/src/main/kotlin/com/daysync/app/feature/expenses/budget/di/BudgetModule.kt` (provide repository)
- Test: `app/src/test/kotlin/com/daysync/app/feature/expenses/budget/BudgetSummaryBuilderTest.kt`

**Interfaces:**
- Consumes: `BudgetDao` (Task 4), `ExpenseDao` (`getMonthlyTotal` Flow), `BudgetResolver` (Task 5).
- Produces:
  - `data class BudgetProgressItem(val instanceKey, kind, label, spent, amount, start, end)` with `val remaining: Double get() = amount - spent`.
  - `data class BudgetSummary(val primary: BudgetProgressItem?, val monthly: BudgetProgressItem?, val all: List<BudgetProgressItem>)`
  - `object BudgetSummaryBuilder { fun build(covering: List<ResolvedBudget>, spentByKey: Map<String, Double>): BudgetSummary }` (pure; unit-tested)
  - `interface BudgetRepository` (methods listed below) + `BudgetRepositoryImpl`.

- [ ] **Step 1: Write the failing test for the pure summary builder**

```kotlin
package com.daysync.app.feature.expenses.budget

import com.daysync.app.feature.expenses.budget.data.BudgetSummaryBuilder
import com.daysync.app.feature.expenses.budget.model.BudgetKind
import com.daysync.app.feature.expenses.budget.model.ResolvedBudget
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetSummaryBuilderTest {
    private fun rb(key: String, kind: BudgetKind, amount: Double, start: LocalDate, end: LocalDate) =
        ResolvedBudget(key, kind, key, start, end, amount)

    @Test
    fun `primary is the smallest range and monthly is separated`() {
        val monthly = rb("MONTHLY:2026-07", BudgetKind.MONTHLY, 40000.0, LocalDate(2026, 7, 1), LocalDate(2026, 7, 31))
        val week = rb("WEEKLY:2026-07:2", BudgetKind.WEEKLY, 10000.0, LocalDate(2026, 7, 8), LocalDate(2026, 7, 14))
        val custom = rb("CUSTOM:c", BudgetKind.CUSTOM, 8000.0, LocalDate(2026, 7, 9), LocalDate(2026, 7, 11))
        val summary = BudgetSummaryBuilder.build(
            covering = listOf(monthly, week, custom),
            spentByKey = mapOf("MONTHLY:2026-07" to 22300.0, "WEEKLY:2026-07:2" to 6800.0, "CUSTOM:c" to 4800.0),
        )
        // custom is the smallest (3 days) -> primary
        assertEquals("CUSTOM:c", summary.primary!!.instanceKey)
        assertEquals(3200.0, summary.primary!!.remaining, 0.0)
        assertEquals("MONTHLY:2026-07", summary.monthly!!.instanceKey)
        assertEquals(3, summary.all.size)
    }

    @Test
    fun `no budgets yields empty summary`() {
        val summary = BudgetSummaryBuilder.build(emptyList(), emptyMap())
        assertEquals(null, summary.primary)
        assertEquals(null, summary.monthly)
        assertEquals(0, summary.all.size)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew :app:testDebugUnitTest --tests "com.daysync.app.feature.expenses.budget.BudgetSummaryBuilderTest"`
Expected: FAIL — unresolved reference.

- [ ] **Step 3: Write the model + pure builder**

`BudgetSummary.kt`:
```kotlin
package com.daysync.app.feature.expenses.budget.model

import kotlinx.datetime.LocalDate

data class BudgetProgressItem(
    val instanceKey: String,
    val kind: BudgetKind,
    val label: String,
    val spent: Double,
    val amount: Double,
    val start: LocalDate,
    val end: LocalDate,
) {
    val remaining: Double get() = amount - spent
    val fraction: Float get() = if (amount <= 0.0) 0f else (spent / amount).toFloat()
}

data class BudgetSummary(
    val primary: BudgetProgressItem?,
    val monthly: BudgetProgressItem?,
    val all: List<BudgetProgressItem>,
)
```

`BudgetSummaryBuilder` (in `data/BudgetRepositoryImpl.kt` or its own file — put in `data/BudgetSummaryBuilder.kt`):
```kotlin
package com.daysync.app.feature.expenses.budget.data

import com.daysync.app.feature.expenses.budget.model.BudgetKind
import com.daysync.app.feature.expenses.budget.model.BudgetProgressItem
import com.daysync.app.feature.expenses.budget.model.BudgetSummary
import com.daysync.app.feature.expenses.budget.model.ResolvedBudget

object BudgetSummaryBuilder {
    private fun ResolvedBudget.days(): Int = start.toEpochDays().let { s -> end.toEpochDays() - s + 1 }

    fun build(covering: List<ResolvedBudget>, spentByKey: Map<String, Double>): BudgetSummary {
        val items = covering.map { rb ->
            BudgetProgressItem(
                instanceKey = rb.instanceKey,
                kind = rb.kind,
                label = rb.label,
                spent = spentByKey[rb.instanceKey] ?: 0.0,
                amount = rb.amount,
                start = rb.start,
                end = rb.end,
            )
        }
        val monthly = items.firstOrNull { it.kind == BudgetKind.MONTHLY }
        // primary = smallest range that is NOT the monthly; fall back to monthly
        val primary = covering
            .filter { it.kind != BudgetKind.MONTHLY }
            .minWithOrNull(compareBy({ it.days() }, { it.end }, { it.instanceKey }))
            ?.let { chosen -> items.first { it.instanceKey == chosen.instanceKey } }
            ?: monthly
        return BudgetSummary(primary = primary, monthly = monthly, all = items)
    }
}
```

Note: `LocalDate.toEpochDays()` is available on kotlinx-datetime `LocalDate`.

- [ ] **Step 4: Run test to verify it passes**

Run: same as Step 2. Expected: PASS (2 tests).

- [ ] **Step 5: Write the repository interface + impl**

`BudgetRepository.kt`:
```kotlin
package com.daysync.app.feature.expenses.budget.data

import com.daysync.app.core.database.entity.BudgetEntity
import com.daysync.app.feature.expenses.budget.model.BudgetSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface BudgetRepository {
    /** All active budget rows (for the setup screen). */
    fun observeActiveBudgets(): Flow<List<BudgetEntity>>

    /** Today-anchored money-left summary for the banner. */
    fun observeSummaryForDate(date: LocalDate): Flow<BudgetSummary>

    suspend fun setRecurringMonthly(amount: Double)
    suspend fun clearRecurringMonthly()
    suspend fun setRecurringFlatWeekly(amount: Double)
    suspend fun clearRecurringFlatWeekly()

    /** Replace the weekly split for a month. If repeatEveryMonth, also install as recurring pattern. */
    suspend fun setVaryingWeekly(year: Int, month: Int, amounts: Map<Int, Double>, repeatEveryMonth: Boolean)

    suspend fun addCustomBudget(year: Int, month: Int, start: LocalDate, end: LocalDate, amount: Double, label: String?)
    suspend fun updateCustomBudget(id: String, start: LocalDate, end: LocalDate, amount: Double, label: String?)
    suspend fun deleteBudget(id: String)
}
```

`BudgetRepositoryImpl.kt`:
```kotlin
package com.daysync.app.feature.expenses.budget.data

import com.daysync.app.core.database.dao.BudgetDao
import com.daysync.app.core.database.dao.ExpenseDao
import com.daysync.app.core.database.entity.BudgetEntity
import com.daysync.app.core.sync.SyncStatus
import com.daysync.app.feature.expenses.budget.BudgetResolver
import com.daysync.app.feature.expenses.budget.model.BudgetSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val expenseDao: ExpenseDao,
) : BudgetRepository {

    private fun ym(year: Int, month: Int) = "%04d-%02d".format(year, month)

    override fun observeActiveBudgets(): Flow<List<BudgetEntity>> = budgetDao.getAllActive()

    override fun observeSummaryForDate(date: LocalDate): Flow<BudgetSummary> =
        budgetDao.getAllActive().flatMapLatest { budgets ->
            val covering = BudgetResolver.coveringDate(budgets, date)
            if (covering.isEmpty()) {
                flowOf(BudgetSummaryBuilder.build(emptyList(), emptyMap()))
            } else {
                val spentFlows = covering.map { rb -> expenseDao.getMonthlyTotal(rb.start, rb.end) }
                combine(spentFlows) { spentArray ->
                    val map = covering.mapIndexed { i, rb -> rb.instanceKey to spentArray[i] }.toMap()
                    BudgetSummaryBuilder.build(covering, map)
                }
            }
        }

    override suspend fun setRecurringMonthly(amount: Double) {
        val existing = budgetDao.getAllActiveList().firstOrNull { it.type == "MONTHLY" && it.recurring }
        val row = (existing ?: newRow("MONTHLY", recurring = true)).copy(
            amount = amount, syncStatus = SyncStatus.PENDING, lastModified = Clock.System.now(),
        )
        budgetDao.upsert(row)
    }

    override suspend fun clearRecurringMonthly() {
        budgetDao.getAllActiveList().filter { it.type == "MONTHLY" && it.recurring }
            .forEach { budgetDao.softDelete(it.id, Clock.System.now().toEpochMilliseconds()) }
    }

    override suspend fun setRecurringFlatWeekly(amount: Double) {
        val existing = budgetDao.getAllActiveList()
            .firstOrNull { it.type == "WEEKLY" && it.recurring && it.weekBlock == null }
        val row = (existing ?: newRow("WEEKLY", recurring = true)).copy(
            amount = amount, weekBlock = null, syncStatus = SyncStatus.PENDING, lastModified = Clock.System.now(),
        )
        budgetDao.upsert(row)
    }

    override suspend fun clearRecurringFlatWeekly() {
        budgetDao.getAllActiveList().filter { it.type == "WEEKLY" && it.recurring && it.weekBlock == null }
            .forEach { budgetDao.softDelete(it.id, Clock.System.now().toEpochMilliseconds()) }
    }

    override suspend fun setVaryingWeekly(year: Int, month: Int, amounts: Map<Int, Double>, repeatEveryMonth: Boolean) {
        val key = ym(year, month)
        // Replace this month's per-block overrides wholesale.
        budgetDao.deletePerMonthWeeklyBlocks(key)
        val perMonthRows = amounts.map { (block, amt) ->
            newRow("WEEKLY", recurring = false).copy(yearMonth = key, weekBlock = block, amount = amt)
        }
        if (repeatEveryMonth) {
            budgetDao.deleteRecurringWeeklyBlocks()
            val patternRows = amounts.map { (block, amt) ->
                newRow("WEEKLY", recurring = true).copy(weekBlock = block, amount = amt)
            }
            budgetDao.upsertAll(patternRows)
        }
        budgetDao.upsertAll(perMonthRows)
    }

    override suspend fun addCustomBudget(year: Int, month: Int, start: LocalDate, end: LocalDate, amount: Double, label: String?) {
        budgetDao.upsert(
            newRow("CUSTOM", recurring = false).copy(
                yearMonth = ym(year, month), startDate = start, endDate = end, amount = amount, label = label,
            )
        )
    }

    override suspend fun updateCustomBudget(id: String, start: LocalDate, end: LocalDate, amount: Double, label: String?) {
        val existing = budgetDao.getById(id) ?: return
        budgetDao.upsert(
            existing.copy(
                startDate = start, endDate = end, amount = amount, label = label,
                syncStatus = SyncStatus.PENDING, lastModified = Clock.System.now(),
            )
        )
    }

    override suspend fun deleteBudget(id: String) {
        budgetDao.softDelete(id, Clock.System.now().toEpochMilliseconds())
    }

    private fun newRow(type: String, recurring: Boolean) = BudgetEntity(
        id = UUID.randomUUID().toString(),
        type = type,
        amount = 0.0,
        recurring = recurring,
        syncStatus = SyncStatus.PENDING,
        lastModified = Clock.System.now(),
    )
}
```

- [ ] **Step 6: Provide the repository in `BudgetModule.kt`**

Add:
```kotlin
    @Provides
    @Singleton
    fun provideBudgetRepository(
        budgetDao: com.daysync.app.core.database.dao.BudgetDao,
        expenseDao: com.daysync.app.core.database.dao.ExpenseDao,
    ): com.daysync.app.feature.expenses.budget.data.BudgetRepository {
        return com.daysync.app.feature.expenses.budget.data.BudgetRepositoryImpl(budgetDao, expenseDao)
    }
```

- [ ] **Step 7: Compile + run the new test**

Run: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew :app:testDebugUnitTest --tests "com.daysync.app.feature.expenses.budget.BudgetSummaryBuilderTest"`
Then: `./gradlew :app:compileDebugKotlin`
Expected: PASS + BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/kotlin/com/daysync/app/feature/expenses/budget/data/ \
  app/src/main/kotlin/com/daysync/app/feature/expenses/budget/model/BudgetSummary.kt \
  app/src/main/kotlin/com/daysync/app/feature/expenses/budget/di/BudgetModule.kt \
  app/src/test/kotlin/com/daysync/app/feature/expenses/budget/BudgetSummaryBuilderTest.kt
git commit -m "Add budget repository and money-left summary builder"
```

---

### Task 12: Weekly browse period (ViewModel)

**Files:**
- Modify: `app/src/main/kotlin/com/daysync/app/feature/expenses/ui/ExpensesViewModel.kt`
- Test: `app/src/test/kotlin/com/daysync/app/feature/expenses/ui/ExpensePeriodWeeklyTest.kt`

**Interfaces:**
- Consumes: `MonthWeeks` (Task 1).
- Produces: `ExpensePeriod.Weekly(year, month, blockIndex)`; `ExpensesViewModel.showWeekly()`, `showMonthly()`, `previousWeek()`, `nextWeek()`; `dateRange` maps `Weekly` to block bounds.

- [ ] **Step 1: Write the failing test** (pure period-stepping via a small helper)

Extract week-stepping into a pure, testable helper so it can be unit-tested without the ViewModel/Hilt.

```kotlin
package com.daysync.app.feature.expenses.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ExpensePeriodWeeklyTest {
    @Test
    fun `nextWeek advances block and rolls into next month`() {
        // July 2026 has 5 blocks; from block 5 -> August block 1
        assertEquals(ExpensePeriod.Weekly(2026, 8, 1), WeeklyNav.next(ExpensePeriod.Weekly(2026, 7, 5)))
        assertEquals(ExpensePeriod.Weekly(2026, 7, 3), WeeklyNav.next(ExpensePeriod.Weekly(2026, 7, 2)))
    }

    @Test
    fun `previousWeek rolls into previous month's last block`() {
        // From August block 1 -> July block 5 (July has 5 blocks)
        assertEquals(ExpensePeriod.Weekly(2026, 7, 5), WeeklyNav.previous(ExpensePeriod.Weekly(2026, 8, 1)))
    }

    @Test
    fun `previousWeek into February lands on its last block (4)`() {
        // March block 1 -> February block 4 (2026 Feb has 4 blocks)
        assertEquals(ExpensePeriod.Weekly(2026, 2, 4), WeeklyNav.previous(ExpensePeriod.Weekly(2026, 3, 1)))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew :app:testDebugUnitTest --tests "com.daysync.app.feature.expenses.ui.ExpensePeriodWeeklyTest"`
Expected: FAIL — unresolved references.

- [ ] **Step 3: Add the `Weekly` case + `WeeklyNav` + ViewModel wiring**

In `ExpensesViewModel.kt`, extend the sealed interface:
```kotlin
sealed interface ExpensePeriod {
    data class Monthly(val year: Int, val month: Int) : ExpensePeriod
    data class Weekly(val year: Int, val month: Int, val blockIndex: Int) : ExpensePeriod
    data class Custom(val start: LocalDate, val end: LocalDate) : ExpensePeriod
}

object WeeklyNav {
    fun next(w: ExpensePeriod.Weekly): ExpensePeriod.Weekly {
        val blocks = com.daysync.app.feature.expenses.budget.model.MonthWeeks.blocksFor(w.year, w.month).size
        return if (w.blockIndex < blocks) w.copy(blockIndex = w.blockIndex + 1)
        else {
            val (y, m) = if (w.month == 12) w.year + 1 to 1 else w.year to w.month + 1
            ExpensePeriod.Weekly(y, m, 1)
        }
    }

    fun previous(w: ExpensePeriod.Weekly): ExpensePeriod.Weekly {
        return if (w.blockIndex > 1) w.copy(blockIndex = w.blockIndex - 1)
        else {
            val (y, m) = if (w.month == 1) w.year - 1 to 12 else w.year to w.month - 1
            val lastBlock = com.daysync.app.feature.expenses.budget.model.MonthWeeks.blocksFor(y, m).size
            ExpensePeriod.Weekly(y, m, lastBlock)
        }
    }
}
```
Extend `dateRange` `when` to handle `Weekly`:
```kotlin
            is ExpensePeriod.Weekly -> {
                val block = com.daysync.app.feature.expenses.budget.model.MonthWeeks
                    .blocksFor(period.year, period.month)
                    .first { it.index == period.blockIndex }
                block.start to block.end
            }
```
Add ViewModel actions:
```kotlin
    fun showWeekly() {
        val block = com.daysync.app.feature.expenses.budget.model.MonthWeeks.blockContaining(today)
        _period.value = ExpensePeriod.Weekly(today.year, today.monthNumber, block.index)
    }

    fun showMonthly() {
        _period.value = ExpensePeriod.Monthly(today.year, today.monthNumber)
    }

    fun previousWeek() {
        (_period.value as? ExpensePeriod.Weekly)?.let { _period.value = WeeklyNav.previous(it) }
    }

    fun nextWeek() {
        (_period.value as? ExpensePeriod.Weekly)?.let { _period.value = WeeklyNav.next(it) }
    }
```
Also update the `rangeLabel`/`isCustomRange` block in `uiState` to add a Weekly label (so the header can show "Jul 8–14"):
```kotlin
                rangeLabel = when (val p = _period.value) {
                    is ExpensePeriod.Custom -> com.daysync.app.core.ui.formatRangeLabel(p.start, p.end)
                    is ExpensePeriod.Weekly -> {
                        val b = com.daysync.app.feature.expenses.budget.model.MonthWeeks
                            .blocksFor(p.year, p.month).first { it.index == p.blockIndex }
                        val mon = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")[p.month - 1]
                        "$mon ${b.start.dayOfMonth}–${b.end.dayOfMonth}"
                    }
                    is ExpensePeriod.Monthly -> null
                },
```

- [ ] **Step 4: Run test to verify it passes**

Run: same as Step 2. Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/daysync/app/feature/expenses/ui/ExpensesViewModel.kt \
  app/src/test/kotlin/com/daysync/app/feature/expenses/ui/ExpensePeriodWeeklyTest.kt
git commit -m "Add weekly browse period with block navigation"
```

---

### Task 13: Week|Month toggle + BudgetSummaryCard in ExpensesScreen

**Files:**
- Create: `app/src/main/kotlin/com/daysync/app/feature/expenses/ui/BudgetSummaryCard.kt`
- Create: `app/src/main/kotlin/com/daysync/app/feature/expenses/ui/BudgetSummaryViewModel.kt`
- Modify: `app/src/main/kotlin/com/daysync/app/feature/expenses/ui/ExpensesScreen.kt`

**Interfaces:**
- Consumes: `BudgetRepository.observeSummaryForDate` (Task 11), `ExpensesViewModel` week actions (Task 12), `UserPreferences.formatCurrency`.
- Produces: a `BudgetSummaryCard` composable and its `BudgetSummaryViewModel` exposing `StateFlow<BudgetSummary>`; a Week|Month segmented control wired to the ViewModel.

- [ ] **Step 1: Create `BudgetSummaryViewModel`**

```kotlin
package com.daysync.app.feature.expenses.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daysync.app.core.config.UserPreferences
import com.daysync.app.feature.expenses.budget.data.BudgetRepository
import com.daysync.app.feature.expenses.budget.model.BudgetSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Clock
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

@HiltViewModel
class BudgetSummaryViewModel @Inject constructor(
    budgetRepository: BudgetRepository,
    userPreferences: UserPreferences,
) : ViewModel() {
    private val today = Clock.System.now().toLocalDateTime(userPreferences.kotlinTimeZone).date

    val summary: StateFlow<BudgetSummary?> =
        budgetRepository.observeSummaryForDate(today)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
```

- [ ] **Step 2: Create `BudgetSummaryCard`**

```kotlin
package com.daysync.app.feature.expenses.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daysync.app.core.config.UserPreferences
import com.daysync.app.feature.expenses.budget.model.BudgetProgressItem

@Composable
fun BudgetSummaryCard(
    onSetBudget: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BudgetSummaryViewModel = hiltViewModel(),
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }
    var expanded by remember { mutableStateOf(false) }

    val current = summary
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable {
                if (current?.primary == null && current?.monthly == null) onSetBudget()
                else expanded = !expanded
            },
    ) {
        Column(Modifier.padding(16.dp)) {
            if (current?.primary == null && current?.monthly == null) {
                Text("Set a budget", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Tap to add a weekly, monthly, or custom budget",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            current.primary?.let { ProgressRow(it, prefs, prominent = true) }
            current.monthly?.takeIf { it.instanceKey != current.primary?.instanceKey }?.let {
                ProgressRow(it, prefs, prominent = false)
            }
            if (expanded) {
                current.all
                    .filter { it.instanceKey != current.primary?.instanceKey && it.instanceKey != current.monthly?.instanceKey }
                    .forEach { ProgressRow(it, prefs, prominent = false) }
            } else if (current.all.size > 2) {
                Text(
                    "tap for all (${current.all.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ProgressRow(item: BudgetProgressItem, prefs: UserPreferences, prominent: Boolean) {
    val over = item.remaining < 0
    val leftText = if (over) "Over by ${prefs.formatCurrency(-item.remaining)}"
    else "${prefs.formatCurrency(item.remaining)} left of ${prefs.formatCurrency(item.amount)}"
    val header = if (prominent) "Money left till ${item.end}" else item.label
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(
            header,
            style = if (prominent) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (prominent) FontWeight.Bold else FontWeight.Normal,
        )
        Text(leftText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LinearProgressIndicator(
            progress = { item.fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            color = when {
                over || item.fraction >= 1f -> MaterialTheme.colorScheme.error
                item.fraction >= 0.75f -> Color(0xFFF59E0B)
                else -> MaterialTheme.colorScheme.primary
            },
        )
    }
}
```

- [ ] **Step 3: Wire into `ExpensesScreen.kt`**

(a) Render the card just inside the `Column` (after the notification banner, before the `when (val state = uiState)` at line 153):
```kotlin
            BudgetSummaryCard(onSetBudget = { navController.navigate(ExpenseBudgets) })
```
(`ExpenseBudgets` route is added in Task 14; if implementing this task first, temporarily use `{}` and swap in Task 14.)

(b) Replace the `MonthSelector(...)` call region (lines 182–206, the non-custom branch) so it includes a Week|Month toggle and switches between month and week headers. Replace the `else` branch body with:
```kotlin
                    } else {
                        // Week | Month toggle
                        val isWeekly = viewModel.period.collectAsStateWithLifecycle().value is ExpensePeriod.Weekly
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = isWeekly,
                                onClick = { viewModel.showWeekly() },
                                label = { Text("Week") },
                            )
                            FilterChip(
                                selected = !isWeekly,
                                onClick = { viewModel.showMonthly() },
                                label = { Text("Month") },
                            )
                        }
                        if (isWeekly) {
                            PeriodStepper(
                                label = state.rangeLabel ?: "",
                                total = state.monthlyTotal,
                                onPrevious = viewModel::previousWeek,
                                onNext = viewModel::nextWeek,
                            )
                        } else {
                            MonthSelector(
                                year = state.selectedYear,
                                month = state.selectedMonth,
                                monthlyTotal = state.monthlyTotal,
                                onPrevious = viewModel::previousMonth,
                                onNext = viewModel::nextMonth,
                            )
                        }
                        var showCustomPicker by remember { mutableStateOf(false) }
                        TextButton(
                            onClick = { showCustomPicker = true },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) { Text("Select Custom Date Range") }
                        if (showCustomPicker) {
                            CustomDateRangeDialog(
                                onConfirm = { start, end ->
                                    viewModel.setCustomRange(start, end)
                                    showCustomPicker = false
                                },
                                onDismiss = { showCustomPicker = false },
                            )
                        }
                    }
```

(c) Add a `PeriodStepper` composable near `MonthSelector` (a generic chevron row reusing the same layout):
```kotlin
@Composable
private fun PeriodStepper(
    label: String,
    total: Double,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) { Icon(Icons.Default.ChevronLeft, contentDescription = "Previous") }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                com.daysync.app.core.config.UserPreferences(context).formatCurrency(total),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, contentDescription = "Next") }
    }
}
```
Add imports as needed: `androidx.compose.material3.FilterChip`, `androidx.lifecycle.compose.collectAsStateWithLifecycle`, and the `ExpensePeriod` import from the same package (already in package scope). The `ChevronLeft`/`ChevronRight` icons are already used by `MonthSelector`.

- [ ] **Step 4: Compile**

Run: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. (If `ExpenseBudgets` is unresolved, implement Task 14 first or use a `{}` placeholder for `onSetBudget` and the nav call, then revisit.)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/daysync/app/feature/expenses/ui/BudgetSummaryCard.kt \
  app/src/main/kotlin/com/daysync/app/feature/expenses/ui/BudgetSummaryViewModel.kt \
  app/src/main/kotlin/com/daysync/app/feature/expenses/ui/ExpensesScreen.kt
git commit -m "Add Week|Month toggle and money-left banner to expenses screen"
```

---

### Task 14: Budget setup screen + navigation

**Files:**
- Create: `app/src/main/kotlin/com/daysync/app/feature/expenses/ui/BudgetSetupViewModel.kt`
- Create: `app/src/main/kotlin/com/daysync/app/feature/expenses/ui/BudgetSetupScreen.kt`
- Modify: `app/src/main/kotlin/com/daysync/app/ui/navigation/Routes.kt` (add `ExpenseBudgets`)
- Modify: `app/src/main/kotlin/com/daysync/app/ui/navigation/DaySyncNavHost.kt` (register route + import)
- Modify: `app/src/main/kotlin/com/daysync/app/feature/expenses/ui/ExpensesScreen.kt` (overflow menu item "Budgets")

**Interfaces:**
- Consumes: `BudgetRepository` (Task 11), `BudgetResolver.weeklyBlocksFor` for labels, `MonthWeeks`.
- Produces: `BudgetSetupScreen(onNavigateBack)`; route `ExpenseBudgets`.

- [ ] **Step 1: Add the route**

In `Routes.kt` after `ExpenseReceiptScan`:
```kotlin
@Serializable data object ExpenseBudgets
```

- [ ] **Step 2: Create `BudgetSetupViewModel`**

```kotlin
package com.daysync.app.feature.expenses.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daysync.app.core.config.UserPreferences
import com.daysync.app.core.database.entity.BudgetEntity
import com.daysync.app.feature.expenses.budget.data.BudgetRepository
import com.daysync.app.feature.expenses.budget.model.MonthWeeks
import com.daysync.app.feature.expenses.budget.model.WeekBlock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

@HiltViewModel
class BudgetSetupViewModel @Inject constructor(
    private val repository: BudgetRepository,
    userPreferences: UserPreferences,
) : ViewModel() {
    val today: LocalDate = Clock.System.now().toLocalDateTime(userPreferences.kotlinTimeZone).date

    val budgets: StateFlow<List<BudgetEntity>> =
        repository.observeActiveBudgets()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun blocksFor(year: Int, month: Int): List<WeekBlock> = MonthWeeks.blocksFor(year, month)

    fun setMonthly(amount: Double?) = viewModelScope.launch {
        if (amount == null || amount <= 0.0) repository.clearRecurringMonthly() else repository.setRecurringMonthly(amount)
    }

    fun setFlatWeekly(amount: Double?) = viewModelScope.launch {
        if (amount == null || amount <= 0.0) repository.clearRecurringFlatWeekly() else repository.setRecurringFlatWeekly(amount)
    }

    fun setVaryingWeekly(year: Int, month: Int, amounts: Map<Int, Double>, repeat: Boolean) = viewModelScope.launch {
        repository.setVaryingWeekly(year, month, amounts, repeat)
    }

    fun addCustom(year: Int, month: Int, start: LocalDate, end: LocalDate, amount: Double, label: String?) = viewModelScope.launch {
        repository.addCustomBudget(year, month, start, end, amount, label)
    }

    fun updateCustom(id: String, start: LocalDate, end: LocalDate, amount: Double, label: String?) = viewModelScope.launch {
        repository.updateCustomBudget(id, start, end, amount, label)
    }

    fun deleteBudget(id: String) = viewModelScope.launch { repository.deleteBudget(id) }
}
```

- [ ] **Step 3: Create `BudgetSetupScreen`**

A Compose screen with: a Monthly amount field + Save; a Flat Weekly amount field + Save; an expandable "Vary by week for this month" section listing `blocksFor(today.year, today.monthNumber)` as labelled amount fields, a live "Unallocated" line vs the monthly amount, and a "Repeat this weekly pattern every month" switch + Save; and a Custom budgets list with an "Add custom budget" button opening `CustomDateRangeDialog` (reuse the existing dialog in the expenses package) plus an amount + label field, and delete buttons per custom row.

```kotlin
package com.daysync.app.feature.expenses.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daysync.app.core.database.entity.BudgetEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSetupScreen(
    onNavigateBack: () -> Unit,
    viewModel: BudgetSetupViewModel = hiltViewModel(),
) {
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val today = viewModel.today

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budgets") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Monthly
            val monthly = budgets.firstOrNull { it.type == "MONTHLY" && it.recurring }
            AmountSection(
                title = "Monthly budget",
                initial = monthly?.amount,
                onSave = { viewModel.setMonthly(it) },
            )
            HorizontalDivider()

            // Flat weekly
            val flatWeekly = budgets.firstOrNull { it.type == "WEEKLY" && it.recurring && it.weekBlock == null }
            AmountSection(
                title = "Weekly budget (every week)",
                initial = flatWeekly?.amount,
                onSave = { viewModel.setFlatWeekly(it) },
            )
            HorizontalDivider()

            // Vary by week for this month
            VaryByWeekSection(
                blocks = viewModel.blocksFor(today.year, today.monthNumber),
                monthlyAmount = monthly?.amount ?: 0.0,
                onSave = { amounts, repeat -> viewModel.setVaryingWeekly(today.year, today.monthNumber, amounts, repeat) },
            )
            HorizontalDivider()

            // Custom budgets
            CustomBudgetsSection(
                customs = budgets.filter { it.type == "CUSTOM" },
                onAdd = { start, end, amount, label -> viewModel.addCustom(today.year, today.monthNumber, start, end, amount, label) },
                onDelete = { viewModel.deleteBudget(it) },
            )
        }
    }
}

@Composable
private fun AmountSection(title: String, initial: Double?, onSave: (Double?) -> Unit) {
    var text by remember(initial) { mutableStateOf(initial?.let { it.toInt().toString() } ?: "") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.filter { c -> c.isDigit() } },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { onSave(text.toDoubleOrNull()) }, modifier = Modifier.align(Alignment.End)) {
            Text("Save")
        }
    }
}

@Composable
private fun VaryByWeekSection(
    blocks: List<com.daysync.app.feature.expenses.budget.model.WeekBlock>,
    monthlyAmount: Double,
    onSave: (Map<Int, Double>, Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val amounts = remember { mutableStateMapOf<Int, String>() }
    var repeat by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Vary by week for this month", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Switch(checked = expanded, onCheckedChange = { expanded = it })
        }
        if (expanded) {
            blocks.forEach { block ->
                OutlinedTextField(
                    value = amounts[block.index] ?: "",
                    onValueChange = { amounts[block.index] = it.filter { c -> c.isDigit() } },
                    label = { Text("${block.start.dayOfMonth}–${block.end.dayOfMonth}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            val allocated = amounts.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
            Text(
                "Unallocated: ${(monthlyAmount - allocated).toInt()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = repeat, onCheckedChange = { repeat = it })
                Text("Repeat this weekly pattern every month", style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = {
                    val map = amounts.mapNotNull { (k, v) -> v.toDoubleOrNull()?.let { k to it } }.toMap()
                    onSave(map, repeat)
                },
                modifier = Modifier.align(Alignment.End),
            ) { Text("Save weekly split") }
        }
    }
}

@Composable
private fun CustomBudgetsSection(
    customs: List<BudgetEntity>,
    onAdd: (kotlinx.datetime.LocalDate, kotlinx.datetime.LocalDate, Double, String?) -> Unit,
    onDelete: (String) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    var pendingRange by remember { mutableStateOf<Pair<kotlinx.datetime.LocalDate, kotlinx.datetime.LocalDate>?>(null) }
    var amountText by remember { mutableStateOf("") }
    var labelText by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Custom budgets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        customs.forEach { c ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(c.label ?: "${c.startDate} – ${c.endDate}", style = MaterialTheme.typography.bodyMedium)
                    Text("${c.startDate} – ${c.endDate} · ${c.amount.toInt()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onDelete(c.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
        Button(onClick = { showPicker = true }) { Text("Add custom budget") }

        if (showPicker) {
            CustomDateRangeDialog(
                onConfirm = { start, end -> pendingRange = start to end; showPicker = false },
                onDismiss = { showPicker = false },
            )
        }
        pendingRange?.let { (start, end) ->
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                label = { Text("Amount for ${start} – ${end}") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = labelText,
                onValueChange = { labelText = it },
                label = { Text("Label (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        onAdd(start, end, amt, labelText.ifBlank { null })
                        pendingRange = null; amountText = ""; labelText = ""
                    }
                },
                modifier = Modifier.align(Alignment.End),
            ) { Text("Add") }
        }
    }
}
```

- [ ] **Step 4: Register the route in `DaySyncNavHost.kt`**

Add import:
```kotlin
import com.daysync.app.feature.expenses.ui.BudgetSetupScreen
import com.daysync.app.ui.navigation.ExpenseBudgets
```
Add composable next to the other expense sub-screens:
```kotlin
        composable<ExpenseBudgets> {
            BudgetSetupScreen(onNavigateBack = { navController.popBackStack() })
        }
```

- [ ] **Step 5: Add "Budgets" overflow item in `ExpensesScreen.kt`**

In the `DropdownMenu`, after the "Payee Rules" item:
```kotlin
                        DropdownMenuItem(
                            text = { Text("Budgets") },
                            onClick = {
                                menuExpanded = false
                                navController.navigate(ExpenseBudgets)
                            },
                        )
```
Add import: `import com.daysync.app.ui.navigation.ExpenseBudgets`. If Task 13's `onSetBudget` used a placeholder, change it to `navController.navigate(ExpenseBudgets)` now.

- [ ] **Step 6: Compile**

Run: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/kotlin/com/daysync/app/feature/expenses/ui/BudgetSetupScreen.kt \
  app/src/main/kotlin/com/daysync/app/feature/expenses/ui/BudgetSetupViewModel.kt \
  app/src/main/kotlin/com/daysync/app/ui/navigation/Routes.kt \
  app/src/main/kotlin/com/daysync/app/ui/navigation/DaySyncNavHost.kt \
  app/src/main/kotlin/com/daysync/app/feature/expenses/ui/ExpensesScreen.kt
git commit -m "Add budget setup screen and navigation"
```

---

### Task 15: Full build, test suite, and manual verification

**Files:** none (verification only).

- [ ] **Step 1: Run the full unit test suite**

Run: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL. All new budget tests pass. (Note: `DataContextBuilderTest` has a pre-existing unrelated failure from the timezone refactor — if it still fails, it is out of scope for this feature; confirm no *budget* test fails.)

- [ ] **Step 2: Assemble the debug APK**

Run: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Apply the Supabase migration**

Manually run `supabase/migrations/004_add_budgets.sql` in the Supabase SQL Editor before installing the build.

- [ ] **Step 4: Manual smoke test (on device/emulator)**

Verify each acceptance criterion:
1. Expenses screen shows a **Week | Month** toggle; Week mode steps blocks with a "Jul 8–14"-style label.
2. Overflow → **Budgets** opens the setup screen. Set a monthly budget (e.g. 40000) and a flat weekly budget (e.g. 10000).
3. The **money-left banner** appears above the list showing the smallest range containing today plus a "This month" line.
4. Add expenses until 50/75/100% of a budget — confirm a **notification** fires at each threshold once.
5. **Edit** an expense down / **delete** it — confirm the banner's money-left increases and a later re-crossing notifies again.
6. Add a **custom** range budget (e.g. covering today); confirm it becomes the banner's primary (smallest range) and tapping the banner expands all budgets.
7. Toggle **vary by week** with "repeat every month" on — confirm next month inherits the pattern (change device date or set an override to spot-check).

- [ ] **Step 5: Commit any final fixes**

```bash
git add -A
git commit -m "Finalize expenses budgeting feature"
```

> **Release note (not a code step):** Bump `versionName`/`versionCode` in `app/build.gradle.kts` at release time per the project's versioning convention (this is a new feature → likely a minor bump; confirm with the user).

---

## Self-Review

**Spec coverage:**
- Req #1 weekly browse toggle → Tasks 12, 13. ✓
- Req #2 per-week & per-month budgets + nested + 50/75/100 notifications → Tasks 3–5, 8–11, 14. ✓
- Req #3 edit/delete recompute → Task 9 + derived spend (Tasks 4, 11). ✓
- Req #4 money-left display → Tasks 11, 13. ✓
- Req #5 custom ranges → Tasks 3, 5, 11, 14. ✓
- Spec §4 data model → Tasks 3, 4. §4.5 sync → Task 6. §4.6 migration → Task 3. ✓
- §8 banner (smallest-range + monthly + tie-break) → Task 11 (`BudgetSummaryBuilder`) + Task 13. ✓
- §10 notifications (event-driven + safety net + re-arm) → Tasks 8, 9, 10. ✓
- §11 device-local dedup → Task 7. ✓
- D6 repeat-weekly-pattern → Task 11 `setVaryingWeekly(repeatEveryMonth)`, Task 14 UI. ✓

**Placeholder scan:** No "TBD"/"handle edge cases" placeholders; the only cross-task forward reference (`ExpenseBudgets` route used in Task 13 before defined in Task 14) is called out explicitly with a fallback.

**Type consistency:** `ResolvedBudget.instanceKey` keys are identical across `BudgetResolver`, `BudgetThresholds` evaluation, `BudgetAlertStore`, and `BudgetSummaryBuilder` ("MONTHLY:YYYY-MM", "WEEKLY:YYYY-MM:idx", "CUSTOM:<id>"). `BudgetEntity` field names match across entity/DAO/DTO/mapper/migration. `getTotalInRangeOnce` (suspend) used by evaluator; `getMonthlyTotal` (Flow) used by repository summary — both defined in Task 4/existing.
