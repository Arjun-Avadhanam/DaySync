# Expenses Budgeting — Design Spec

**Date:** 2026-07-10
**Status:** Approved design, pending implementation plan
**Feature area:** `feature/expenses`

## 1. Summary

Add budgeting to the Expenses section. Users can set spending caps over date ranges
(a whole month, day-of-month "week" blocks, or arbitrary custom ranges), see how much
money is left for the period they are viewing, and get notified at 50% / 75% / 100% of
each active cap. Budget progress is always derived live from the current expenses, so
editing or deleting an expense recomputes every budget with no stored counter to get stuck.

Also adds a **Week | Month** browse toggle to the expenses period selector.

Scope for v1: a single **overall** budget dimension (all expenses count toward it).
Per-category budgets are explicitly out of scope but the schema leaves room for them.

## 2. Requirements (from the request)

1. A weekly browse period in addition to monthly — a **Week | Month** toggle.
2. Per-week and per-month budgets, with **50/75/100%** notifications. When setting a
   monthly budget the user can also set per-week budgets within that month.
3. Edit/delete of an expense must correctly recompute budget progress (never stuck).
4. A **"Money left in Budget: Rs xxx"** display above the expenses list.
5. (Added mid-discussion) **Custom date-range budgets within a month** — any number of
   calendar-picked ranges; the money-left display generalizes to "money left till <date>".

## 3. Key decisions (resolved during brainstorming)

| # | Decision |
|---|----------|
| D1 | **One overall budget** dimension for v1. Schema carries a nullable `category` (null = overall) so per-category can be added later without migration churn. |
| D2 | **Everything is a "range budget"**: `{ amount, startDate, endDate }`. Monthly, weekly-block, and custom are all instances of this one primitive. |
| D3 | Weekly = **day-of-month blocks**: 1–7, 8–14, 15–21, 22–28, 29–end. Displayed with real dates ("Jul 8–14"). Blocks tile the month exactly. A 28-day month has 4 blocks (block 5 empty). |
| D4 | **Monthly and weekly caps are active simultaneously**, each tracked and alerting independently. |
| D5 | **Recurrence:** monthly cap and *flat* weekly cap recur automatically (set once, apply every period) with per-month overrides. Custom ranges are **one-off**, tied to exact dates. |
| D6 | A **varying** weekly split (Week1=X, Week2=Y…) defaults to **that month only**; an opt-in **"repeat this weekly pattern every month"** toggle promotes it to a recurring block-indexed template, applied by block number (missing blocks skipped). |
| D7 | Custom ranges **may overlap** each other and the weekly/monthly ranges. Each budget is independent; an expense can count toward several. |
| D8 | **Money-left banner** leads with the **smallest range containing today**, with **This month** as a secondary line; tap expands to all applicable budgets. Over-budget shows "Over by Rs xxx". |
| D9 | **Spent is always derived** (`SUM(totalAmount)` over the range) — no stored counter. This is the mechanism that satisfies requirement #3. |
| D10 | Notifications are **event-driven**: recomputed on every expense add/edit/delete (covers manual, CSV, receipt, and auto-captured notification expenses, which all pass through the repository), with a daily safety-net worker. Dedup markers **re-arm downward** when spend drops below a threshold. |
| D11 | Budgets are **synced** to Supabase and restored on reinstall (user-entered config). Notification dedup state stays **device-local**. |
| D12 | Week-start day is a **non-issue** — blocks are date-based, not weekday-based. |

## 4. Data model

### 4.1 Room entity — `BudgetEntity` (new)

New table `budgets`. One row per budget definition (template or instance).

```
BudgetEntity(
    id: String              // PK, UUID
    type: String            // "MONTHLY" | "WEEKLY" | "CUSTOM"
    category: String? = null // null = overall (reserved for future per-category)
    amount: Double
    recurring: Boolean       // true = template applying every period; false = specific instance
    yearMonth: String? = null // "YYYY-MM": set for month-specific instances/overrides and all CUSTOM; null for recurring templates
    weekBlock: Int? = null    // 1..5 for WEEKLY rows; null for MONTHLY/CUSTOM
    startDate: LocalDate? = null // set for CUSTOM (explicit range); null for MONTHLY/WEEKLY (derived)
    endDate: LocalDate? = null   // set for CUSTOM; null for MONTHLY/WEEKLY (derived)
    label: String? = null     // optional user label for CUSTOM (e.g. "Goa trip")
    // SyncableEntity:
    syncStatus, lastModified, isDeleted
)
```

Indices: `type`, `yearMonth`, `(type, recurring)`.

**Row shapes:**

| Concept | type | recurring | yearMonth | weekBlock | start/end |
|---|---|---|---|---|---|
| Recurring monthly cap | MONTHLY | true | null | null | null |
| Specific-month monthly override | MONTHLY | false | "2026-07" | null | null |
| Flat recurring weekly cap | WEEKLY | true | null | null | null |
| Recurring varying pattern (per block) | WEEKLY | true | null | 1..5 | null |
| Per-month weekly-block override | WEEKLY | false | "2026-07" | 1..5 | null |
| Custom range | CUSTOM | false | "2026-07" | null | set |

### 4.2 Resolution rules (which cap applies)

- **Monthly cap for month M** = `MONTHLY, recurring=false, yearMonth=M`
  ?: `MONTHLY, recurring=true`.
- **Weekly cap for block (M, b)** =
  `WEEKLY, recurring=false, yearMonth=M, weekBlock=b`  (per-month override)
  ?: `WEEKLY, recurring=true, weekBlock=b`              (promoted varying pattern)
  ?: `WEEKLY, recurring=true, weekBlock=null`           (flat recurring cap).
- **Custom budgets for month M** = all `CUSTOM, yearMonth=M, isDeleted=0`.

Enabling "vary by week" for month M writes five `WEEKLY, recurring=false, yearMonth=M, weekBlock=1..5` rows. Toggling "repeat every month" instead writes `WEEKLY, recurring=true, weekBlock=1..5` rows (and clears the per-month rows).

### 4.3 Day-of-month blocks

Block boundaries for a month with `n` days:
- Block 1: 1–7, Block 2: 8–14, Block 3: 15–21, Block 4: 22–28, Block 5: 29–`n` (omitted if `n <= 28`).

A small pure helper `MonthWeeks.blocksFor(year, month): List<Block(index, startDate, endDate)>`
using the existing `daysInMonth` logic (currently private in `ExpensesViewModel`; extract to a
shared util so both the ViewModel and budget code use it).

### 4.4 Spent computation

Reuse `ExpenseDao.getMonthlyTotal(start: LocalDate, end: LocalDate): Flow<Double>` (a plain
range `SUM(totalAmount) WHERE isDeleted=0`). No new query needed — every budget's spent is
this sum over its resolved `[start, end]`. This is derived, so edit/delete/add recompute
automatically (D9).

### 4.5 Sync

Follow the existing expense pattern:
- `core/sync/dto/BudgetDto.kt` (`@Serializable`, snake_case `@SerialName`, `date` fields as String).
- `SyncMappers.kt`: `BudgetEntity.toDto()` / `BudgetRow.toEntity()`.
- `DaySyncEngine`: add `syncBudgets()` step + register `"budgets"` in `syncSteps`.
- `SyncRestoreEngine`: add `"budgets" to ::restoreBudgets` and a `BudgetRow`.
- Supabase migration: new numbered SQL file under `supabase/migrations/` creating `budgets`.
  New DTO fields default to null per project policy.

### 4.6 Room migration

Bump `AppDatabase` version **6 → 7**. Add an explicit `Migration(6, 7)` in `DatabaseModule.kt`
that `CREATE TABLE budgets (...)` (real migration, not destructive fallback, per project policy).
Register `BudgetEntity` in `@Database.entities` and add `abstract fun budgetDao(): BudgetDao`.

## 5. DAO — `BudgetDao` (new)

```
@Upsert suspend fun upsert(entity: BudgetEntity)
@Query("... isDeleted=0") fun getAllActive(): Flow<List<BudgetEntity>>
suspend fun getById(id): BudgetEntity?
suspend fun softDelete(id, now)                 // sets isDeleted=1, syncStatus=PENDING
// resolution helpers (or resolve in repository from getAllActive)
suspend fun getPendingSync(): List<BudgetEntity>
suspend fun markAsSynced(ids: List<String>)
```

Resolution (§4.2) can be done in Kotlin over `getAllActive()` to keep SQL simple; the active
budget set is tiny.

## 6. Repository — `BudgetRepository` + impl (new)

Interface responsibilities:
- CRUD: `setMonthlyBudget(amount, recurring, yearMonth?)`, `setFlatWeeklyBudget(...)`,
  `setVaryingWeekly(yearMonth, Map<blockIndex, amount>, repeatEveryMonth: Boolean)`,
  `addCustomBudget(yearMonth, start, end, amount, label?)`, `updateCustomBudget(...)`,
  `deleteBudget(id)`.
- Read model: `observeBudgetsForRange(start, end): Flow<List<ResolvedBudget>>` where a
  `ResolvedBudget` carries `{ id, kind, label, start, end, amount, spentFlow }`. The repo
  resolves applicable budgets (§4.2) and pairs each with its `getMonthlyTotal(start,end)` flow.
- `moneyLeftFor(range)` and `budgetsCovering(date)` helpers for the banner.

Bind in `ExpenseModule` (Hilt `@Provides @Singleton`), following `ExpenseRepository`.

## 7. Period browsing (requirement #1)

- Extend the `ExpensePeriod` sealed interface (`ExpensesViewModel.kt:22`) with
  `data class Weekly(val year: Int, val month: Int, val blockIndex: Int) : ExpensePeriod`.
- `dateRange` maps `Weekly` to the block's `[start, end]` via `MonthWeeks.blocksFor`.
- Add `previousWeek()` / `nextWeek()` that step block-to-block, rolling across month
  boundaries (block 5 → next month block 1; block 1 → previous month's last block).
- UI: a **Week | Month** segmented control in the period-selector header
  (`ExpensesScreen.kt` `MonthSelector`, ~:282). In Week mode the chevrons step weeks and the
  centered label reads e.g. "Jul 8–14"; in Month mode current behavior. The existing
  "Custom Date Range" browse stays.

## 8. Money-left banner (requirements #4, #5)

New composable `BudgetSummaryCard`, rendered directly above the `LazyColumn` in
`ExpensesScreen.kt` (between the period selector and the list).

- **Anchored to today**, not the browsed period — it always answers "how much can I still
  spend right now". (The period selector header still shows the browsed period's spent total;
  the banner is independent.)
- Leads with the **smallest-range budget containing today**:
  "Money left till <endDate> — Rs <left> of Rs <amount>" + a progress bar coloured by
  threshold (normal / 75%+ / over). Tie-break when two ranges of equal length contain today:
  earlier `endDate`, then `id`.
- Secondary line: **This month — Rs <left> left of <amount>**.
- Tapping expands a list of **all** budgets covering the date (custom, weekly, monthly),
  each with its own bar.
- Over budget: "Over by Rs <x>" in the error colour; bar clamped/!full.
- No budget covers the view: a subtle "Set a budget" affordance opening the setup screen.
- Currency via `UserPreferences.formatCurrency`; "today" via `kotlinTimeZone`.

## 9. Budget setup UI (requirement #2)

A **Budgets** entry in the Expenses overflow menu (alongside Import CSV / Payee Rules),
opening a `BudgetSetupScreen`:

- **Monthly budget**: amount field + enable switch (recurring). "Override just this month" option.
- **Weekly budget**: amount field + enable switch (flat, recurring, applies to every block).
- **Vary by week for a month**: expander → month picker + rows for each day-of-month block
  labelled with real dates, each with an amount field; a live "unallocated: Rs x" remainder
  vs the monthly cap; a **"repeat this weekly pattern every month"** toggle (D6).
- **Custom budgets**: a list + "Add custom budget" → M3 `DateRangePicker` (same component the
  expenses custom-range browse already uses) + amount + optional label. Any number; edit/delete each.

## 10. Notifications (requirement #2)

- New channel `"budget_alerts"` ("Budget Alerts"). Create it in a small
  `BudgetNotificationChannel` object (mirrors `ExpenseNotificationChannel`).
- `BudgetAlertEvaluator` (injectable): given an affected `LocalDate`, resolve every budget
  instance covering that date **and** the containing month, compute `pct = spent/amount`, and
  for each determine the highest crossed threshold in {50, 75, 100}.
- **Dedup + re-arm:** device-local store (SharedPreferences JSON map) keyed by a stable
  budget-instance key (e.g. `MONTHLY:2026-07`, `WEEKLY:2026-07:2`, `CUSTOM:<id>`) → highest
  threshold notified. Fire a notification only when the crossed threshold exceeds the stored
  value; **lower** the stored value when `pct` falls below a threshold band (so a later
  re-crossing notifies again). Never notify on a downward move.
- **Trigger points:** call `evaluator.onExpenseChanged(date)` from `ExpenseRepositoryImpl`
  after `addExpense` / `updateExpense` / `deleteExpense` succeed (covers all capture sources).
  For edit that moves an expense's date, evaluate both old and new dates.
- **Safety net:** a periodic `@HiltWorker BudgetCheckWorker` (daily) re-evaluates current
  month + current block, so a genuinely missed event still surfaces. Prunes stale dedup keys.
- Posting mirrors `DailyReminderWorker.postReminder` (no shared helper exists; optionally
  extract a tiny `NotificationHelper`, but not required).

## 11. Edge cases

- **Edit/delete recompute (req #3):** guaranteed by derived spend (§4.4) — no counter to reset.
- **Expense moved across a budget boundary by an edit:** evaluate both dates; both budgets recompute.
- **Soft-deleted expenses** already excluded (`isDeleted=0`) from all sums — deletes drop spend.
- **Overlapping custom ranges:** allowed; each budget independent (D7). An expense in the
  overlap counts toward each — intended.
- **28-day month:** block 5 absent; varying/recurring patterns skip missing block indices (D3/D6).
- **Over budget:** money-left goes negative → displayed as "Over by Rs x"; 100% alert fires once
  until spend drops back below 100% then over again.
- **No budget set:** banner shows a "Set a budget" prompt; no alerts.
- **Amount edited downward below current spend:** immediately reflects as over/threshold; dedup
  re-arms so the next real crossing notifies.
- **Currency/timezone:** always via `UserPreferences` (formatCurrency / kotlinTimeZone).

## 12. Out of scope (v1)

- Per-category budgets (schema-ready via nullable `category`, not built).
- Rollover of unspent budget into the next period.
- Budget analytics/charts.
- Configurable alert thresholds (fixed at 50/75/100).

## 13. Affected / new files

**New:** `BudgetEntity`, `BudgetDao`, `BudgetRepository`(+Impl), `BudgetDto`,
`BudgetAlertEvaluator`, `BudgetNotificationChannel`, `BudgetCheckWorker`, `MonthWeeks` util,
`BudgetSetupScreen`, `BudgetSummaryCard`, Supabase `budgets` migration SQL.

**Modified:** `AppDatabase` (v7, entity+dao), `DatabaseModule` (Migration 6→7),
`ExpensesViewModel`/`ExpensePeriod` (Weekly case, week nav), `ExpensesScreen`
(Week|Month toggle + banner + Budgets menu entry), `ExpenseRepositoryImpl` (evaluator hooks),
`ExpenseModule` (DI), `SyncMappers`/`DaySyncEngine`/`SyncRestoreEngine` (budget sync+restore),
version bump in `app/build.gradle.kts`.
