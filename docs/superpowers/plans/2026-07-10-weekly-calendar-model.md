# Weekly Calendar-Week Model Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Switch the Expenses weekly budget/browse granularity from day-of-month blocks (1–7, 8–14, …) to real Monday–Sunday calendar weeks, keeping Month and Custom untouched.

**Architecture:** A new pure `CalendarWeeks` util (Monday-of-date, weeks-overlapping-month) replaces `MonthWeeks`. `ExpensePeriod.Weekly` is re-keyed by its Monday date; `WeeklyNav` steps ±7 days. Weekly budgets become a recurring flat cap plus per-week overrides keyed by the week's Monday, reusing the existing `startDate`/`endDate` columns — so **no Room/Supabase/DTO schema change**. Resolution, repository, and the setup UI are updated to match; `MonthWeeks` is deleted last once unreferenced.

**Tech Stack:** Kotlin (JDK 17), Room, Hilt, Jetpack Compose + Material 3, kotlinx-datetime. Tests: JUnit 4 + MockK (JVM unit tests; no device in CI).

## Global Constraints

- Build/test: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew <task>`.
- A week is **Monday–Sunday** (ISO), identified by its **Monday `LocalDate`**. "This week" = Mon–Sun week containing today. Week start is fixed at Monday (matches the Journal calendar); not configurable.
- **No schema change**: Room stays v7; `budgets` table, `BudgetDto`, mappers, sync/restore unchanged. Per-week overrides reuse `startDate` (Monday) / `endDate` (Sunday); `weekBlock`/`yearMonth` go unused for weekly rows.
- Weekly instance key = `WEEKLY:<monday-iso>` (e.g. `WEEKLY:2026-07-06`, from `monday.toString()`).
- `LocalDate.dayOfWeek.value` is ISO (Monday=1 … Sunday=7) — verified against `JournalCalendarGrid.kt`.
- Date math via `kotlinx.datetime` `plus`/`minus` with `DateTimeUnit.DAY`.
- Currency via `UserPreferences.formatCurrency`; timezone via `kotlinTimeZone`.
- No `Co-Authored-By` trailer in commits.
- Old `weekBlock`-keyed weekly rows are inert (never matched by the new resolver); no data migration.

---

### Task 1: `CalendarWeeks` util

**Files:**
- Create: `app/src/main/kotlin/com/daysync/app/feature/expenses/budget/model/CalendarWeeks.kt`
- Test: `app/src/test/kotlin/com/daysync/app/feature/expenses/budget/model/CalendarWeeksTest.kt`

**Interfaces:**
- Produces:
  - `data class CalendarWeek(val start: LocalDate, val end: LocalDate)` (start = Monday, end = Sunday)
  - `object CalendarWeeks { fun daysInMonth(year: Int, month: Int): Int; fun weekStart(date: LocalDate): LocalDate; fun weekEnd(monday: LocalDate): LocalDate; fun weeksOverlappingMonth(year: Int, month: Int): List<CalendarWeek> }`

`MonthWeeks` remains in place this task (still referenced elsewhere); it is deleted in Task 6.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.daysync.app.feature.expenses.budget.model

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarWeeksTest {
    @Test
    fun `weekStart returns the Monday of the containing week`() {
        // Jul 2026: Jul 6 is Monday, Jul 12 is Sunday
        assertEquals(LocalDate(2026, 7, 6), CalendarWeeks.weekStart(LocalDate(2026, 7, 6)))  // Monday -> itself
        assertEquals(LocalDate(2026, 7, 6), CalendarWeeks.weekStart(LocalDate(2026, 7, 10))) // Friday
        assertEquals(LocalDate(2026, 7, 6), CalendarWeeks.weekStart(LocalDate(2026, 7, 12))) // Sunday
        assertEquals(LocalDate(2026, 6, 29), CalendarWeeks.weekStart(LocalDate(2026, 7, 1)))  // Wed -> prior Monday
    }

    @Test
    fun `weekEnd is the Sunday six days after the Monday`() {
        assertEquals(LocalDate(2026, 7, 12), CalendarWeeks.weekEnd(LocalDate(2026, 7, 6)))
        // Crosses a month boundary
        assertEquals(LocalDate(2026, 8, 2), CalendarWeeks.weekEnd(LocalDate(2026, 7, 27)))
    }

    @Test
    fun `weeksOverlappingMonth covers all Mon-Sun weeks touching the month`() {
        val weeks = CalendarWeeks.weeksOverlappingMonth(2026, 7)
        assertEquals(
            listOf(
                LocalDate(2026, 6, 29), LocalDate(2026, 7, 6), LocalDate(2026, 7, 13),
                LocalDate(2026, 7, 20), LocalDate(2026, 7, 27),
            ),
            weeks.map { it.start },
        )
        assertEquals(LocalDate(2026, 7, 5), weeks.first().end)   // partial leading week
        assertEquals(LocalDate(2026, 8, 2), weeks.last().end)    // trailing week spills into August
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew :app:testDebugUnitTest --tests "com.daysync.app.feature.expenses.budget.model.CalendarWeeksTest"`
Expected: FAIL — `CalendarWeeks` unresolved.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.daysync.app.feature.expenses.budget.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

data class CalendarWeek(val start: LocalDate, val end: LocalDate)

object CalendarWeeks {
    fun daysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        else -> 30
    }

    /** Monday of the ISO week containing [date]. dayOfWeek.value: Monday=1 … Sunday=7. */
    fun weekStart(date: LocalDate): LocalDate = date.minus(date.dayOfWeek.value - 1, DateTimeUnit.DAY)

    fun weekEnd(monday: LocalDate): LocalDate = monday.plus(6, DateTimeUnit.DAY)

    /** Every Mon–Sun week (by Monday) that overlaps [first-of-month .. last-of-month]. */
    fun weeksOverlappingMonth(year: Int, month: Int): List<CalendarWeek> {
        val first = LocalDate(year, month, 1)
        val last = LocalDate(year, month, daysInMonth(year, month))
        val weeks = mutableListOf<CalendarWeek>()
        var monday = weekStart(first)
        while (monday <= last) {
            weeks += CalendarWeek(monday, weekEnd(monday))
            monday = monday.plus(7, DateTimeUnit.DAY)
        }
        return weeks
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: same as Step 2. Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/daysync/app/feature/expenses/budget/model/CalendarWeeks.kt app/src/test/kotlin/com/daysync/app/feature/expenses/budget/model/CalendarWeeksTest.kt
git commit -m "Add CalendarWeeks Monday-Sunday week util"
```

---

### Task 2: `BudgetResolver` weekly rework

**Files:**
- Modify: `app/src/main/kotlin/com/daysync/app/feature/expenses/budget/BudgetResolver.kt`
- Test: `app/src/test/kotlin/com/daysync/app/feature/expenses/budget/BudgetResolverTest.kt`

**Interfaces:**
- Consumes: `CalendarWeeks` (Task 1), `BudgetEntity`.
- Produces: `BudgetResolver.weeklyForWeek(budgets: List<BudgetEntity>, monday: LocalDate): ResolvedBudget?`; `coveringDate` now resolves the calendar week containing the date. `weeklyBlocksFor` is removed. `monthlyFor`/`customFor` unchanged.

- [ ] **Step 1: Replace the weekly cases in `BudgetResolverTest.kt`**

Replace the existing test method `weekly resolution precedence per-month override then pattern then flat` and the `coveringDate returns monthly plus containing block plus containing customs` method with these (keep the `budget(...)` helper, the monthly tests, and the null test unchanged):

```kotlin
    @Test
    fun `per-week override wins over recurring flat cap`() {
        val budgets = listOf(
            budget("flat", "WEEKLY", 10000.0, recurring = true),
            budget("ovr", "WEEKLY", 15000.0, recurring = false,
                start = LocalDate(2026, 7, 6), end = LocalDate(2026, 7, 12)),
        )
        // week of Mon Jul 6
        assertEquals(15000.0, BudgetResolver.weeklyForWeek(budgets, LocalDate(2026, 7, 6))!!.amount, 0.0)
        assertEquals("WEEKLY:2026-07-06", BudgetResolver.weeklyForWeek(budgets, LocalDate(2026, 7, 6))!!.instanceKey)
        // a different week falls back to the flat cap
        assertEquals(10000.0, BudgetResolver.weeklyForWeek(budgets, LocalDate(2026, 7, 13))!!.amount, 0.0)
    }

    @Test
    fun `weeklyForWeek is null when neither override nor flat cap exists`() {
        assertNull(BudgetResolver.weeklyForWeek(emptyList(), LocalDate(2026, 7, 6)))
    }

    @Test
    fun `coveringDate resolves the calendar week containing the date`() {
        val budgets = listOf(
            budget("m", "MONTHLY", 40000.0, recurring = true),
            budget("w", "WEEKLY", 10000.0, recurring = true),
            budget("c", "CUSTOM", 8000.0, recurring = false, yearMonth = "2026-07",
                start = LocalDate(2026, 7, 5), end = LocalDate(2026, 7, 12), label = "Trip"),
        )
        // Jul 10 is in the Mon Jul 6 week
        val keys = BudgetResolver.coveringDate(budgets, LocalDate(2026, 7, 10)).map { it.instanceKey }.toSet()
        assertEquals(setOf("MONTHLY:2026-07", "WEEKLY:2026-07-06", "CUSTOM:c"), keys)
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew :app:testDebugUnitTest --tests "com.daysync.app.feature.expenses.budget.BudgetResolverTest"`
Expected: FAIL — `weeklyForWeek` unresolved.

- [ ] **Step 3: Rework `BudgetResolver.kt`**

Change the import from `MonthWeeks` to `CalendarWeeks`:
```kotlin
import com.daysync.app.feature.expenses.budget.model.CalendarWeeks
```
(Remove `import com.daysync.app.feature.expenses.budget.model.MonthWeeks`.)

In `monthlyFor`, change `MonthWeeks.daysInMonth(...)` to `CalendarWeeks.daysInMonth(...)`.

Add a short-month helper next to `monthLabel`:
```kotlin
    private fun shortMonth(month: Int): String = monthLabel(month).take(3)
```

Replace the entire `weeklyBlocksFor(...)` function with:
```kotlin
    fun weeklyForWeek(budgets: List<BudgetEntity>, monday: LocalDate): ResolvedBudget? {
        val weeklies = budgets.filter { it.type == "WEEKLY" }
        val override = weeklies.firstOrNull { !it.recurring && it.startDate == monday }
        val flat = weeklies.firstOrNull { it.recurring && it.weekBlock == null }
        val chosen = override ?: flat ?: return null
        val end = CalendarWeeks.weekEnd(monday)
        return ResolvedBudget(
            instanceKey = "WEEKLY:$monday",
            kind = BudgetKind.WEEKLY,
            label = "${shortMonth(monday.monthNumber)} ${monday.dayOfMonth} – ${shortMonth(end.monthNumber)} ${end.dayOfMonth}",
            start = monday,
            end = end,
            amount = chosen.amount,
        )
    }
```

Replace `coveringDate`'s weekly line. The full new `coveringDate`:
```kotlin
    fun coveringDate(budgets: List<BudgetEntity>, date: LocalDate): List<ResolvedBudget> {
        val year = date.year
        val month = date.monthNumber
        val result = mutableListOf<ResolvedBudget>()
        monthlyFor(budgets, year, month)?.let { result += it }
        weeklyForWeek(budgets, CalendarWeeks.weekStart(date))?.let { result += it }
        result += customFor(budgets, year, month).filter { date >= it.start && date <= it.end }
        return result
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: same as Step 2. Expected: PASS (all `BudgetResolverTest` methods).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/daysync/app/feature/expenses/budget/BudgetResolver.kt app/src/test/kotlin/com/daysync/app/feature/expenses/budget/BudgetResolverTest.kt
git commit -m "Resolve weekly budgets by calendar week and Monday-keyed overrides"
```

---

### Task 3: `ExpensePeriod.Weekly` re-keyed + `WeeklyNav` ±7 days

**Files:**
- Modify: `app/src/main/kotlin/com/daysync/app/feature/expenses/ui/ExpensesViewModel.kt`
- Test: `app/src/test/kotlin/com/daysync/app/feature/expenses/ui/ExpensePeriodWeeklyTest.kt`

**Interfaces:**
- Consumes: `CalendarWeeks` (Task 1), `formatRangeLabel` (`com.daysync.app.core.ui.formatRangeLabel`).
- Produces: `ExpensePeriod.Weekly(weekStart: LocalDate)`; `WeeklyNav.next/previous(w): ExpensePeriod.Weekly` stepping ±7 days. `showWeekly()/showMonthly()/previousWeek()/nextWeek()` signatures unchanged. **`ExpensesScreen.kt` needs no change** — the weekly label comes from `state.rangeLabel` and all method signatures are stable.

- [ ] **Step 1: Rewrite `ExpensePeriodWeeklyTest.kt`**

```kotlin
package com.daysync.app.feature.expenses.ui

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpensePeriodWeeklyTest {
    @Test
    fun `nextWeek advances by seven days across a month boundary`() {
        assertEquals(
            ExpensePeriod.Weekly(LocalDate(2026, 8, 3)),
            WeeklyNav.next(ExpensePeriod.Weekly(LocalDate(2026, 7, 27))),
        )
    }

    @Test
    fun `previousWeek goes back seven days across a month boundary`() {
        assertEquals(
            ExpensePeriod.Weekly(LocalDate(2026, 7, 27)),
            WeeklyNav.previous(ExpensePeriod.Weekly(LocalDate(2026, 8, 3))),
        )
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew :app:testDebugUnitTest --tests "com.daysync.app.feature.expenses.ui.ExpensePeriodWeeklyTest"`
Expected: FAIL — `Weekly(LocalDate)` constructor mismatch / unresolved.

- [ ] **Step 3: Update `ExpensesViewModel.kt`**

Replace the `Weekly` case and `WeeklyNav`:
```kotlin
sealed interface ExpensePeriod {
    data class Monthly(val year: Int, val month: Int) : ExpensePeriod
    data class Weekly(val weekStart: LocalDate) : ExpensePeriod
    data class Custom(val start: LocalDate, val end: LocalDate) : ExpensePeriod
}

object WeeklyNav {
    fun next(w: ExpensePeriod.Weekly): ExpensePeriod.Weekly =
        ExpensePeriod.Weekly(w.weekStart.plus(7, kotlinx.datetime.DateTimeUnit.DAY))

    fun previous(w: ExpensePeriod.Weekly): ExpensePeriod.Weekly =
        ExpensePeriod.Weekly(w.weekStart.minus(7, kotlinx.datetime.DateTimeUnit.DAY))
}
```
Add imports at the top (the file already imports `kotlinx.datetime.LocalDate`):
```kotlin
import kotlinx.datetime.plus
import kotlinx.datetime.minus
```

Replace the `Weekly` branch in the `dateRange` `when`:
```kotlin
            is ExpensePeriod.Weekly ->
                period.weekStart to com.daysync.app.feature.expenses.budget.model.CalendarWeeks.weekEnd(period.weekStart)
```

Replace the `Weekly` branch in the `rangeLabel` `when`:
```kotlin
                    is ExpensePeriod.Weekly -> com.daysync.app.core.ui.formatRangeLabel(
                        p.weekStart,
                        com.daysync.app.feature.expenses.budget.model.CalendarWeeks.weekEnd(p.weekStart),
                    )
```

Replace `showWeekly()` (keep `showMonthly`, `previousWeek`, `nextWeek` as they are):
```kotlin
    fun showWeekly() {
        _period.value = ExpensePeriod.Weekly(
            com.daysync.app.feature.expenses.budget.model.CalendarWeeks.weekStart(today)
        )
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: same as Step 2. Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/daysync/app/feature/expenses/ui/ExpensesViewModel.kt app/src/test/kotlin/com/daysync/app/feature/expenses/ui/ExpensePeriodWeeklyTest.kt
git commit -m "Re-key weekly browse period by Monday date"
```

---

### Task 4: `BudgetRepository` per-week overrides

**Files:**
- Modify: `app/src/main/kotlin/com/daysync/app/feature/expenses/budget/data/BudgetRepository.kt`
- Modify: `app/src/main/kotlin/com/daysync/app/feature/expenses/budget/data/BudgetRepositoryImpl.kt`

**Interfaces:**
- Consumes: `CalendarWeeks.weekEnd` (Task 1), `BudgetDao`.
- Produces: `BudgetRepository.setWeekOverride(monday: LocalDate, amount: Double)` and `clearWeekOverride(monday: LocalDate)`; `setVaryingWeekly(...)` removed. `setRecurringFlatWeekly`/`clearRecurringFlatWeekly`/monthly/custom unchanged.

- [ ] **Step 1: Update the interface `BudgetRepository.kt`**

Remove:
```kotlin
    /** Replace the weekly split for a month. If repeatEveryMonth, also install as recurring pattern. */
    suspend fun setVaryingWeekly(year: Int, month: Int, amounts: Map<Int, Double>, repeatEveryMonth: Boolean)
```
Add in its place:
```kotlin
    /** Set (upsert) a per-week override amount for the Mon–Sun week starting [monday]. */
    suspend fun setWeekOverride(monday: LocalDate, amount: Double)

    /** Remove any per-week override for the week starting [monday]. */
    suspend fun clearWeekOverride(monday: LocalDate)
```

- [ ] **Step 2: Update `BudgetRepositoryImpl.kt`**

Add the import:
```kotlin
import com.daysync.app.feature.expenses.budget.model.CalendarWeeks
```
Remove the entire `setVaryingWeekly(...)` override and replace with:
```kotlin
    override suspend fun setWeekOverride(monday: LocalDate, amount: Double) {
        val existing = budgetDao.getAllActiveList()
            .firstOrNull { it.type == "WEEKLY" && !it.recurring && it.startDate == monday }
        val row = (existing ?: newRow("WEEKLY", recurring = false)).copy(
            startDate = monday,
            endDate = CalendarWeeks.weekEnd(monday),
            amount = amount,
            syncStatus = SyncStatus.PENDING,
            lastModified = Clock.System.now(),
        )
        budgetDao.upsert(row)
    }

    override suspend fun clearWeekOverride(monday: LocalDate) {
        budgetDao.getAllActiveList()
            .filter { it.type == "WEEKLY" && !it.recurring && it.startDate == monday }
            .forEach { budgetDao.softDelete(it.id, Clock.System.now().toEpochMilliseconds()) }
    }
```

(The DAO methods `deletePerMonthWeeklyBlocks`/`deleteRecurringWeeklyBlocks` are now unused but left in `BudgetDao` — no schema/DAO change per spec.)

- [ ] **Step 3: Compile**

Run: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew :app:compileDebugKotlin`
Expected: FAIL — `BudgetSetupViewModel` still calls `setVaryingWeekly` (fixed in Task 5). This confirms the only remaining caller. Proceed to Task 5 before re-compiling.

- [ ] **Step 4: Commit (bundled with Task 5)** — do not commit yet; Task 5 restores compilation.

---

### Task 5: Setup UI — per-week overrides

**Files:**
- Modify: `app/src/main/kotlin/com/daysync/app/feature/expenses/ui/BudgetSetupViewModel.kt`
- Modify: `app/src/main/kotlin/com/daysync/app/feature/expenses/ui/BudgetSetupScreen.kt`

**Interfaces:**
- Consumes: `CalendarWeeks`/`CalendarWeek` (Task 1), `BudgetRepository.setWeekOverride/clearWeekOverride` (Task 4).
- Produces: `BudgetSetupViewModel.weeksForCurrentMonth(): List<CalendarWeek>`, `setWeekOverride(monday, amount?)`; the setup screen's vary-by-week section edits per-week overrides.

- [ ] **Step 1: Update `BudgetSetupViewModel.kt`**

Change imports — remove `MonthWeeks`/`WeekBlock`, add `CalendarWeeks`/`CalendarWeek`:
```kotlin
import com.daysync.app.feature.expenses.budget.model.CalendarWeek
import com.daysync.app.feature.expenses.budget.model.CalendarWeeks
```
Replace `blocksFor(...)`:
```kotlin
    fun weeksForCurrentMonth(): List<CalendarWeek> =
        CalendarWeeks.weeksOverlappingMonth(today.year, today.monthNumber)
```
Replace `setVaryingWeekly(...)`:
```kotlin
    fun setWeekOverride(monday: kotlinx.datetime.LocalDate, amount: Double?) = viewModelScope.launch {
        if (amount == null || amount <= 0.0) repository.clearWeekOverride(monday)
        else repository.setWeekOverride(monday, amount)
    }
```

- [ ] **Step 2: Update `BudgetSetupScreen.kt`**

Change the import:
```kotlin
import com.daysync.app.feature.expenses.budget.model.CalendarWeek
```
(remove `import com.daysync.app.feature.expenses.budget.model.WeekBlock`.)

In `BudgetSetupScreen`, replace the `VaryByWeekSection(...)` call with one that passes the overlapping weeks and the current override amounts (derived from the `budgets` list already collected):
```kotlin
            // Vary by week (per-week overrides for weeks touching this month)
            val weekOverrides = budgets
                .filter { it.type == "WEEKLY" && !it.recurring && it.startDate != null }
                .associate { it.startDate!! to it.amount }
            VaryByWeekSection(
                weeks = viewModel.weeksForCurrentMonth(),
                overrideAmounts = weekOverrides,
                onSaveWeek = { monday, amount -> viewModel.setWeekOverride(monday, amount) },
            )
```

Replace the entire `VaryByWeekSection` composable with:
```kotlin
@Composable
private fun VaryByWeekSection(
    weeks: List<CalendarWeek>,
    overrideAmounts: Map<kotlinx.datetime.LocalDate, Double>,
    onSaveWeek: (kotlinx.datetime.LocalDate, Double?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Vary by week (this month)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = expanded, onCheckedChange = { expanded = it })
        }
        if (expanded) {
            Text(
                "Override a specific week; blank falls back to your weekly budget.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            weeks.forEach { week ->
                var text by remember(week.start) {
                    mutableStateOf(overrideAmounts[week.start]?.let { it.toInt().toString() } ?: "")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.filter { c -> c.isDigit() } },
                        label = { Text("${week.start.monthNumber}/${week.start.dayOfMonth} – ${week.end.monthNumber}/${week.end.dayOfMonth}") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = { onSaveWeek(week.start, text.toDoubleOrNull()) },
                        modifier = Modifier.padding(start = 8.dp),
                    ) { Text("Save") }
                }
            }
        }
    }
}
```

(Remove the now-unused `mutableStateMapOf` import if present and no longer referenced; leave it if other sections use it — `CustomBudgetsSection` does not, so remove `import androidx.compose.runtime.mutableStateMapOf`.)

- [ ] **Step 3: Compile**

Run: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit (Tasks 4 + 5 together)**

```bash
git add app/src/main/kotlin/com/daysync/app/feature/expenses/budget/data/BudgetRepository.kt \
  app/src/main/kotlin/com/daysync/app/feature/expenses/budget/data/BudgetRepositoryImpl.kt \
  app/src/main/kotlin/com/daysync/app/feature/expenses/ui/BudgetSetupViewModel.kt \
  app/src/main/kotlin/com/daysync/app/feature/expenses/ui/BudgetSetupScreen.kt
git commit -m "Replace weekly split with per-week overrides in budget setup"
```

---

### Task 6: Delete `MonthWeeks`, full build + test suite

**Files:**
- Delete: `app/src/main/kotlin/com/daysync/app/feature/expenses/budget/model/MonthWeeks.kt`
- Delete: `app/src/test/kotlin/com/daysync/app/feature/expenses/budget/model/MonthWeeksTest.kt`

- [ ] **Step 1: Confirm nothing references `MonthWeeks`/`WeekBlock`/`blocksFor`/`blockContaining`/`setVaryingWeekly`**

Run:
```bash
cd /home/arjun/DaySync
grep -rn "MonthWeeks\|WeekBlock\|blocksFor\|blockContaining\|setVaryingWeekly\|weeklyBlocksFor" app/src/main app/src/test
```
Expected: no matches (empty output). If any remain, fix them before deleting.

- [ ] **Step 2: Delete the files**

```bash
git rm app/src/main/kotlin/com/daysync/app/feature/expenses/budget/model/MonthWeeks.kt \
  app/src/test/kotlin/com/daysync/app/feature/expenses/budget/model/MonthWeeksTest.kt
```

- [ ] **Step 3: Run the full unit test suite**

Run: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL for all budget/expenses tests. (The pre-existing `DataContextBuilderTest` — 21 failures from an unrelated `tz` reflection rot — may still fail; confirm no *budget/expenses* test fails and no new failures appear.)

- [ ] **Step 4: Assemble the debug APK**

Run: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/arjun/Android/Sdk ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add -A app/src/main/kotlin/com/daysync/app/feature/expenses/budget/model/ app/src/test/kotlin/com/daysync/app/feature/expenses/budget/model/
git commit -m "Remove day-of-month MonthWeeks util"
```

- [ ] **Step 6: Manual smoke test (on device/emulator)**

1. Expenses → **Week** toggle shows a Mon–Sun range label (e.g. "Jul 6 – Jul 12"); chevrons step by exactly 7 days across month boundaries.
2. Budgets setup → set a weekly cap, then "Vary by week (this month)" lists weeks touching the month (including a leading/trailing partial week); set an override on one week and Save.
3. Add expenses in that week → the money-left banner's weekly line reflects the override amount; alerts fire at 50/75/100% of the overridden week.
4. A week spanning into next month appears in both months' vary-by-week lists and edits the same override.

---

## Self-Review

**Spec coverage:**
- §4 `CalendarWeeks` (weekStart/weekEnd/weeksOverlappingMonth/daysInMonth) → Task 1. ✓
- §5 `ExpensePeriod.Weekly(weekStart)` + `WeeklyNav` ±7 + labels + `showWeekly` → Task 3. ✓ (ExpensesScreen needs no change — noted.)
- §6 `weeklyForWeek` + `coveringDate` + `WEEKLY:<monday>` key → Task 2. ✓ (Spec's optional `weeksOverlappingMonthResolved` helper is intentionally not added; the setup VM lists weeks via `CalendarWeeks.weeksOverlappingMonth` and reads override amounts directly from the budgets list — simpler, avoids a flat-cap-prefill bug where a week with only a flat cap would wrongly pre-fill an override amount.)
- §7 repository `setWeekOverride`/`clearWeekOverride`, remove `setVaryingWeekly` → Task 4. ✓
- §8 setup UI per-week overrides, remove repeat toggle + unallocated line → Task 5. ✓
- §4 W4 no schema change → confirmed: only `startDate`/`endDate` reused; Task list touches no entity/DAO/DTO/migration. ✓
- §7 W7 stale rows inert → new resolver matches only `startDate == monday` (overrides) or `recurring && weekBlock == null` (flat); old `weekBlock`-set rows match neither. ✓
- §10 tests: CalendarWeeksTest, ExpensePeriodWeeklyTest, BudgetResolverTest updated → Tasks 1–3. ✓
- §11 delete MonthWeeks → Task 6. ✓

**Placeholder scan:** none.

**Type consistency:** `ExpensePeriod.Weekly(weekStart: LocalDate)` used identically in Tasks 2 (via `coveringDate`/`weekStart`), 3, and tests. `weeklyForWeek(budgets, monday)` signature matches its test call and `coveringDate` call. `setWeekOverride(monday, amount)`/`clearWeekOverride(monday)` names match across interface (Task 4), impl (Task 4), and VM (Task 5). Instance key `WEEKLY:<monday>` (from `monday.toString()`, ISO `2026-07-06`) matches the resolver test assertions. `CalendarWeek(start, end)` fields used consistently in the setup UI (Task 5) and resolver/util.
