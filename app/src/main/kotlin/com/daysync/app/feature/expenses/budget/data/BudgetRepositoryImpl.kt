package com.daysync.app.feature.expenses.budget.data

import com.daysync.app.core.database.dao.BudgetDao
import com.daysync.app.core.database.dao.ExpenseDao
import com.daysync.app.core.database.entity.BudgetEntity
import com.daysync.app.core.sync.SyncStatus
import com.daysync.app.feature.expenses.budget.BudgetResolver
import com.daysync.app.feature.expenses.budget.model.CalendarWeeks
import com.daysync.app.feature.expenses.budget.model.BudgetSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
