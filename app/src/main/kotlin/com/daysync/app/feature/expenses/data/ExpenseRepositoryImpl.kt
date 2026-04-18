package com.daysync.app.feature.expenses.data

import com.daysync.app.core.database.dao.CategoryTotal
import com.daysync.app.core.database.dao.ExpenseDao
import com.daysync.app.core.database.dao.PayeeRuleDao
import com.daysync.app.core.database.entity.ExpenseEntity
import com.daysync.app.core.database.entity.PayeeRuleEntity
import com.daysync.app.core.sync.SyncStatus
import com.daysync.app.feature.expenses.model.Expense
import com.daysync.app.feature.expenses.model.ExpenseCategory
import com.daysync.app.feature.expenses.model.formatIndianCurrency
import com.daysync.app.feature.expenses.model.generateExpenseId
import com.daysync.app.feature.expenses.model.toDomain
import com.daysync.app.feature.expenses.model.toEntity
import com.daysync.app.feature.expenses.service.ParsedTransaction
import com.daysync.app.feature.expenses.service.ReceiptData
import com.daysync.app.feature.expenses.service.TransactionDeduplicator
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class ExpenseRepositoryImpl(
    private val expenseDao: ExpenseDao,
    private val payeeRuleDao: PayeeRuleDao,
    private val deduplicator: TransactionDeduplicator,
) : ExpenseRepository {

    override fun getExpensesForDate(date: LocalDate): Flow<List<Expense>> {
        return expenseDao.getByDate(date).map { list -> list.map { it.toDomain() } }
    }

    override fun getExpensesInRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<Expense>> {
        return expenseDao.getExpensesInRange(startDate, endDate)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun getDailyTotal(date: LocalDate): Flow<Double> {
        return expenseDao.getDailyTotal(date)
    }

    override fun getMonthlyTotal(startDate: LocalDate, endDate: LocalDate): Flow<Double> {
        return expenseDao.getMonthlyTotal(startDate, endDate)
    }

    override fun getCategoryTotals(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<CategoryTotal>> {
        return expenseDao.getTotalByCategory(startDate, endDate)
    }

    override fun getMerchantNames(): Flow<List<String>> {
        return expenseDao.getAllMerchantNames()
    }

    override suspend fun getExpenseById(id: String): Expense? {
        return expenseDao.getById(id)?.toDomain()
    }

    override suspend fun addExpense(expense: Expense) {
        expenseDao.insert(expense.toEntity())
    }

    override suspend fun updateExpense(expense: Expense) {
        expenseDao.update(expense.toEntity())
    }

    override suspend fun deleteExpense(id: String) {
        val entity = expenseDao.getById(id) ?: return
        expenseDao.update(
            entity.copy(
                isDeleted = true,
                syncStatus = SyncStatus.PENDING,
                lastModified = Clock.System.now(),
            )
        )
    }

    override suspend fun processNotification(parsed: ParsedTransaction): ProcessResult {
        val now = Clock.System.now()
        val today = now.toLocalDateTime(TimeZone.of("Asia/Kolkata")).date

        // 1. Dedup check — strictly by reference ID
        val existing = deduplicator.findDuplicate(parsed.referenceId)
        if (existing != null) {
            if (deduplicator.shouldUpdateExisting(existing, parsed.merchantName)) {
                expenseDao.update(
                    existing.copy(
                        merchantName = parsed.merchantName ?: existing.merchantName,
                        lastModified = now,
                    )
                )
            }
            return ProcessResult.Deduplicated(existing.id)
        }

        // 2. Determine category — payee rule wins, else built-in merchant rules
        val rule = parsed.merchantName?.let { payeeRuleDao.getByPayeeName(it) }
        val category: String? = rule?.category
            ?: ExpenseCategory.suggestFromMerchant(parsed.merchantName, parsed.packageName)
        val title: String? = parsed.merchantName

        // 3. Save (or prompt for classification if we still don't know the category)
        val expense = createExpenseFromParsed(parsed, today, category, title)
        expenseDao.insert(expense)
        return if (category == null) {
            ProcessResult.NeedsClassification(expense.toDomain())
        } else {
            ProcessResult.Saved(expense.toDomain())
        }
    }

    override suspend fun importFromCsv(entries: List<CsvExpenseEntry>): ImportResult {
        var imported = 0
        var skipped = 0
        var errors = 0

        for (entry in entries) {
            try {
                if (entry.debitAmount == null || entry.debitAmount <= 0) continue

                val existing = deduplicator.findDuplicateForCsv(
                    amount = entry.debitAmount,
                    date = entry.date,
                )
                if (existing != null) {
                    skipped++
                    continue
                }

                val category = ExpenseCategory.suggestFromMerchant(entry.narration, null)
                val entity = ExpenseEntity(
                    id = generateExpenseId(),
                    title = entry.narration,
                    date = entry.date,
                    category = category,
                    unitCost = entry.debitAmount,
                    totalAmount = entry.debitAmount,
                    source = "CSV",
                    merchantName = entry.merchantName,
                    syncStatus = SyncStatus.PENDING,
                    lastModified = Clock.System.now(),
                )
                expenseDao.insert(entity)
                imported++
            } catch (_: Exception) {
                errors++
            }
        }

        return ImportResult(imported = imported, skippedDuplicates = skipped, errors = errors)
    }

    override suspend fun saveFromReceipt(
        receiptData: ReceiptData,
        receiptDate: LocalDate?,
    ): Expense {
        val date = receiptDate
            ?: receiptData.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Kolkata")).date

        val category = receiptData.category?.let { cat ->
            if (ExpenseCategory.fromCategoryString(cat) != null) cat else null
        } ?: ExpenseCategory.suggestFromMerchant(receiptData.merchantName, null)

        val lineItemsSummary = receiptData.lineItems?.joinToString(", ") { item ->
            "${item.name} ${formatIndianCurrency(item.totalPrice)}"
        }
        val notes = lineItemsSummary?.let { "Items: $it" }

        val entity = ExpenseEntity(
            id = generateExpenseId(),
            title = receiptData.merchantName,
            date = date,
            category = category,
            unitCost = receiptData.totalAmount,
            totalAmount = receiptData.totalAmount,
            source = "RECEIPT",
            merchantName = receiptData.merchantName,
            notes = notes,
            syncStatus = SyncStatus.PENDING,
            lastModified = Clock.System.now(),
        )
        expenseDao.insert(entity)
        return entity.toDomain()
    }

    override fun getPayeeRules(): Flow<List<PayeeRuleEntity>> {
        return payeeRuleDao.getAll()
    }

    override suspend fun addPayeeRule(
        payeeName: String,
        category: String,
        defaultTitle: String?,
    ) {
        payeeRuleDao.insert(
            PayeeRuleEntity(
                id = generateExpenseId(),
                payeeName = payeeName.lowercase().trim(),
                category = category,
                defaultTitle = defaultTitle,
            )
        )
    }

    override suspend fun getPayeeRule(payeeName: String): PayeeRuleEntity? {
        return payeeRuleDao.getByPayeeName(payeeName)
    }

    override suspend fun deletePayeeRule(id: String) {
        payeeRuleDao.deleteById(id)
    }

    private fun createExpenseFromParsed(
        parsed: ParsedTransaction,
        date: LocalDate,
        category: String?,
        title: String?,
    ): ExpenseEntity {
        val notes = buildString {
            parsed.referenceId?.let { append("Ref: $it") }
            if (parsed.currency != "INR") {
                if (isNotEmpty()) append(" | ")
                append("Currency: ${parsed.currency}")
            }
        }.ifBlank { null }

        return ExpenseEntity(
            id = generateExpenseId(),
            title = title,
            date = date,
            category = category,
            unitCost = parsed.amount,
            totalAmount = parsed.amount,
            notes = notes,
            source = "NOTIFICATION",
            merchantName = parsed.merchantName,
            syncStatus = SyncStatus.PENDING,
            lastModified = Clock.System.now(),
        )
    }
}
