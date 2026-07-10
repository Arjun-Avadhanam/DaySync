# Weekly Calendar-Week Model — Design Spec

**Date:** 2026-07-10
**Status:** Approved design, pending implementation plan
**Feature area:** `feature/expenses` (budgeting)
**Supersedes:** the "day-of-month blocks" weekly model in `2026-07-10-expenses-budgeting-design.md` §D2/D3/D6. Monthly and Custom budgets are unchanged.

## 1. Summary

Change the weekly granularity of the Expenses budgeting feature from **day-of-month blocks** (1–7, 8–14, 15–21, 22–28, 29–end) to **real Monday–Sunday calendar weeks**. Month and Custom budgets/browsing are untouched (still date-wise). A week is identified by its Monday. Weekly budgeting becomes: one recurring "Rs X per Mon–Sun week" cap plus optional per-week overrides keyed by the week's Monday. No Room, Supabase, or DTO schema change is required — per-week overrides reuse the existing `startDate`/`endDate` columns.

## 2. Motivation

Day-of-month blocks were chosen originally so weekly budgets tiled a month exactly and summed to the monthly budget. The user wants weeks aligned to actual weekdays instead ("a week is Monday–Sunday, based on days of the week"), consistent with the Journal calendar which already starts weeks on Monday. Calendar weeks cross month boundaries, so the exact-tiling / month-sum property is intentionally dropped.

## 3. Key decisions (resolved during brainstorming)

| # | Decision |
|---|----------|
| W1 | A week is **Monday–Sunday** (ISO), 7 days, identified by its **Monday's `LocalDate`**. "This week" = the Mon–Sun week containing today. |
| W2 | Weekly budgeting = **one recurring flat cap** (Rs X per Mon–Sun week) **+ optional per-week overrides**. An override sets a different amount for one specific week, keyed by that week's Monday. |
| W3 | **Removed:** day-of-month blocks, `weekBlock` (1–5) keying, per-month weekly-block overrides, and the "repeat this weekly pattern every month" toggle. A recurring cap already repeats every week. |
| W4 | Per-week override rows **reuse `startDate` (Monday) and `endDate` (Sunday)** — the same columns Custom budgets use. **No schema migration** (Room stays v7; Supabase `budgets` unchanged; `weekBlock`/`yearMonth` go unused for weekly rows). |
| W5 | Weekly instance key (for alert dedup and the banner) becomes `WEEKLY:<monday-iso>`, e.g. `WEEKLY:2026-07-06`. |
| W6 | The setup screen's "vary by week" section lists the Mon–Sun weeks that **overlap the current month**. A week spanning two months (e.g. Jul 27–Aug 2) appears in **both** months' lists and is the same underlying override (keyed by its Monday). |
| W7 | Old `weekBlock`-keyed weekly rows (from v2.7.0) are **inert** — the new resolver never matches them. No data migration; the feature shipped the same day, so realistically none exist. |
| W8 | Month and Custom budgets, the monthly cap, the money-left banner logic (smallest-range-containing-today + monthly sub-line), notifications, and sync are all **unchanged** except that the weekly range is now a Mon–Sun week. |

## 4. Week math

New util `CalendarWeeks` (replaces `MonthWeeks`):

```
object CalendarWeeks {
    // Monday of the ISO week containing `date`.
    fun weekStart(date: LocalDate): LocalDate           // date.minus(dayOfWeek.isoDayNumber - 1 days)
    fun weekEnd(monday: LocalDate): LocalDate            // monday.plus(6 days)
    // All Mon–Sun weeks (by their Monday) that overlap [first..last] of the given month.
    fun weeksOverlappingMonth(year: Int, month: Int): List<CalendarWeek>
}
data class CalendarWeek(val start: LocalDate, val end: LocalDate) // start = Monday, end = Sunday
```

- `weekStart` uses `date.dayOfWeek.isoDayNumber` (Mon = 1 … Sun = 7): `date.minus(isoDayNumber - 1, DateTimeUnit.DAY)`.
- `weeksOverlappingMonth(y, m)`: first = `LocalDate(y, m, 1)`, last = `LocalDate(y, m, daysInMonth)`. Start from `weekStart(first)`, step +7 days while `weekMonday <= last`, emitting `CalendarWeek(weekMonday, weekMonday+6)`. This yields the (possibly partial-at-edges) weeks that touch the month, including a final week whose Sunday is in the next month.
- Keep a `daysInMonth(year, month)` helper (currently in `MonthWeeks`/`ExpensesViewModel`) available to `CalendarWeeks` for month bounds.

## 5. Period model & browsing

- `ExpensePeriod.Weekly(year, month, blockIndex)` → **`ExpensePeriod.Weekly(weekStart: LocalDate)`** (the Monday).
- `dateRange` for `Weekly` = `weekStart .. CalendarWeeks.weekEnd(weekStart)`.
- `WeeklyNav`: `next(w) = Weekly(w.weekStart.plus(7 days))`, `previous(w) = Weekly(w.weekStart.minus(7 days))`. No month logic.
- `showWeekly()` sets `Weekly(CalendarWeeks.weekStart(today))`.
- Weekly range label in `ExpensesViewModel.uiState` and the stepper: format as `"MMM d – MMM d"` across the week (e.g. `"Jul 6 – Jul 12"`; if the week spans months, `"Jul 27 – Aug 2"`). Reuse `com.daysync.app.core.ui.formatRangeLabel(start, end)` for consistency with Custom range labels.

## 6. Resolution & keys

`BudgetResolver`:

- Remove `weeklyBlocksFor(budgets, year, month)`.
- Add `weeklyForWeek(budgets, monday: LocalDate): ResolvedBudget?`:
  - override = `WEEKLY, recurring=false, startDate == monday, isDeleted=0` → use it;
  - else flat cap = `WEEKLY, recurring=true, weekBlock == null` → use it over this week's range;
  - else `null`.
  - Resolved fields: `instanceKey = "WEEKLY:" + monday`, `kind = WEEKLY`, `start = monday`, `end = monday+6`, `label = "<Mon d> – <Sun d>"`, `amount` from the chosen row.
- Add `weeksOverlappingMonthResolved(budgets, year, month): List<ResolvedBudget>` for the setup UI (one resolved budget per overlapping week, using `weeklyForWeek` on each week's Monday; weeks with neither override nor flat cap still appear with amount 0 so the user can set them).
- `coveringDate(budgets, date)`: monthly (unchanged) + `weeklyForWeek(budgets, weekStart(date))` (if non-null) + customs containing date (unchanged).

## 7. Repository

`BudgetRepository`:

- Keep `setRecurringFlatWeekly(amount)` / `clearRecurringFlatWeekly()` (unchanged row shape).
- Remove `setVaryingWeekly(year, month, amounts, repeatEveryMonth)`.
- Add:
  - `suspend fun setWeekOverride(monday: LocalDate, amount: Double)` — upsert a `WEEKLY, recurring=false, startDate=monday, endDate=monday+6, amount` row; if one already exists for that Monday, update it.
  - `suspend fun clearWeekOverride(monday: LocalDate)` — soft-delete the override for that Monday, if any.
- `observeSummaryForDate` / `observeActiveBudgets` unchanged.
- Add DAO support to find an existing weekly override by Monday: either filter `getAllActiveList()` in Kotlin (small set — preferred, no new query) or a `@Query`. Prefer Kotlin filtering to avoid a DAO change.

## 8. Setup UI ("vary by week")

- Replace the day-of-month block editor with a **per-week override editor**:
  - Header "Vary by week (this month)" with an expander.
  - Rows = `CalendarWeeks.weeksOverlappingMonth(today.year, today.monthNumber)`, each labelled with its date span (e.g. "Jun 29 – Jul 5", "Jul 27 – Aug 2"), pre-filled with the current override amount if any (blank otherwise).
  - Saving a non-blank amount calls `setWeekOverride(week.start, amount)`; clearing a previously-set field calls `clearWeekOverride(week.start)`.
  - Remove the "repeat every month" toggle and the "unallocated vs monthly" remainder line (no month-sum relationship anymore). Optionally show each week's fallback (the recurring cap) as placeholder text.
- Monthly, flat-weekly, and Custom sections unchanged.

## 9. Alerts

- Weekly instance key `WEEKLY:<monday>` flows through `BudgetAlertEvaluator` unchanged (it already iterates `coveringDate`). Dedup markers are per real week; the daily `BudgetCheckWorker` prunes stale keys (including old `WEEKLY:YYYY-MM:idx` keys).
- Thresholds 50/75/100 and re-arm behaviour unchanged.

## 10. Tests

- Replace `MonthWeeksTest` → `CalendarWeeksTest`: `weekStart` for various weekdays (Mon returns itself, Sun returns the prior Mon), `weekEnd`, and `weeksOverlappingMonth` for July 2026 (expect weeks with Mondays Jun 29, Jul 6, 13, 20, 27) and a clean-aligned month.
- Replace `ExpensePeriodWeeklyTest`: `WeeklyNav.next/previous` = ±7 days across a month boundary (e.g. `Weekly(2026-07-27).next == Weekly(2026-08-03)`).
- Update `BudgetResolverTest` weekly cases: override-by-Monday wins over flat cap; flat cap applies when no override; `coveringDate` picks the correct week and key `WEEKLY:2026-07-06` for a date in that week.
- `BudgetThresholdsTest`, `BudgetAlertStoreTest`, `BudgetAlertEvaluatorTest`, `BudgetSummaryBuilderTest` need no logic change (the evaluator/summary are key-agnostic); adjust any fixtures that referenced block-based weekly keys.

## 11. Files touched

**Rename/rework:** `feature/expenses/budget/model/MonthWeeks.kt` → `CalendarWeeks.kt` (+ `WeekBlock` → `CalendarWeek`).
**Modify:** `BudgetResolver.kt`, `feature/expenses/budget/data/BudgetRepository.kt` + `BudgetRepositoryImpl.kt`, `ExpensesViewModel.kt` (`ExpensePeriod.Weekly` + `WeeklyNav` + weekly label), `ExpensesScreen.kt` (weekly stepper label only — toggle unchanged), `BudgetSetupScreen.kt` + `BudgetSetupViewModel.kt` (vary-by-week → per-week overrides).
**Tests:** `MonthWeeksTest`→`CalendarWeeksTest`, `ExpensePeriodWeeklyTest`, `BudgetResolverTest`.
**Unchanged:** `BudgetEntity`, `BudgetDao`, `BudgetDto`, sync/restore/mappers, Supabase, Room version, `BudgetThresholds`, `BudgetAlertStore`, `BudgetAlertEvaluator`, `BudgetCheckWorker`, `BudgetSummary`/`BudgetSummaryBuilder`, money-left banner, monthly & custom budgets.

## 12. Out of scope

- Configurable week-start day (fixed at Monday, matching Journal).
- Any month/custom behaviour change.
- Migrating or cleaning up pre-existing day-of-month weekly rows (they're inert).
- Weekly-vs-monthly sum reconciliation (impossible with calendar weeks, intentionally dropped).
