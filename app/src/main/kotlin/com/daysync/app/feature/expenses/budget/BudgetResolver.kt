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
