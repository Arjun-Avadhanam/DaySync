package com.daysync.app.feature.ai.data

import com.daysync.app.core.database.dao.DailyHealthOverrideDao
import com.daysync.app.core.database.dao.DailyMealEntryDao
import com.daysync.app.core.database.dao.DailyNutritionSummaryDao
import com.daysync.app.core.database.dao.ExerciseSessionDao
import com.daysync.app.core.database.dao.ExpenseDao
import com.daysync.app.core.database.dao.FoodItemDao
import com.daysync.app.core.database.dao.HealthMetricDao
import com.daysync.app.core.database.dao.JournalEntryDao
import com.daysync.app.core.database.dao.MediaItemDao
import com.daysync.app.core.database.dao.SleepSessionDao
import com.daysync.app.core.database.dao.SportEventDao
import com.daysync.app.core.database.entity.ExerciseSessionEntity
import com.daysync.app.core.database.entity.HealthMetricEntity
import com.daysync.app.core.database.entity.SleepSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

class DataContextBuilder(
    private val healthMetricDao: HealthMetricDao,
    private val sleepSessionDao: SleepSessionDao,
    private val exerciseSessionDao: ExerciseSessionDao,
    private val nutritionSummaryDao: DailyNutritionSummaryDao,
    private val dailyMealEntryDao: DailyMealEntryDao,
    private val foodItemDao: FoodItemDao,
    private val expenseDao: ExpenseDao,
    private val journalEntryDao: JournalEntryDao,
    private val mediaItemDao: MediaItemDao,
    private val sportEventDao: SportEventDao,
    private val dailyHealthOverrideDao: DailyHealthOverrideDao,
) {
    private val tz = TimeZone.of("Asia/Kolkata")

    suspend fun buildContextForQuestion(question: String): String {
        val (startDate, endDate) = inferDateRange(question)
        return buildContext(startDate, endDate)
    }

    suspend fun buildContext(startDate: LocalDate, endDate: LocalDate): String {
        val startMillis = startDate.atStartOfDayIn(tz).toEpochMilliseconds()
        val nextDay = endDate.plus(DatePeriod(days = 1))
        val endMillis = nextDay.atStartOfDayIn(tz).toEpochMilliseconds() - 1
        val startStr = startDate.toString()
        val endStr = endDate.toString()

        val healthMetrics = healthMetricDao.getByDateRange(startMillis, endMillis)
        val sleepSessions = sleepSessionDao.getByDateRange(startMillis, endMillis)
        val exercises = exerciseSessionDao.getByDateRange(startMillis, endMillis)
        val nutritionSummaries = nutritionSummaryDao.getByDateRangeList(startStr, endStr)
        val expenses = expenseDao.getByDateRange(startStr, endStr)
        val categoryTotals = expenseDao.getTotalByCategoryList(startStr, endStr)
        val journalEntries = journalEntryDao.getByDateRangeList(startStr, endStr)
        val sportEvents = sportEventDao.getEventsByDateRangeList(startMillis, endMillis)
        val mediaItems = mediaItemDao.getActiveAndCompleted()

        val dates = generateDateSequence(startDate, endDate)
        val sb = StringBuilder()
        sb.appendLine("Data from $startDate to $endDate:")
        sb.appendLine()

        for (date in dates) {
            val dayMillisStart = date.atStartOfDayIn(tz).toEpochMilliseconds()
            val dayNext = date.plus(DatePeriod(days = 1))
            val dayMillisEnd = dayNext.atStartOfDayIn(tz).toEpochMilliseconds() - 1
            val dateStr = date.toString()

            sb.appendLine("=== $date ===")

            // Health metrics
            val dayMetrics = healthMetrics.filter {
                it.timestamp.toEpochMilliseconds() in dayMillisStart..dayMillisEnd
            }
            sb.appendLine("Health: ${formatHealthMetrics(dayMetrics)}")

            // Sleep (group by endTime — Wed night→Thu morning counts as Thursday)
            val daySleep = sleepSessions.filter {
                it.endTime.toEpochMilliseconds() in dayMillisStart..dayMillisEnd
            }
            sb.appendLine("Sleep: ${formatSleep(daySleep)}")

            // Workouts
            val dayExercises = exercises.filter {
                it.startTime.toEpochMilliseconds() in dayMillisStart..dayMillisEnd
            }
            sb.appendLine("Workouts: ${formatExercises(dayExercises)}")

            // Weight & calories burned (from daily health overrides)
            val override = dailyHealthOverrideDao.get(date)
            val weightParts = listOfNotNull(
                override?.weightMorning?.let { "Morning ${it}kg" },
                override?.weightEvening?.let { "Evening ${it}kg" },
                override?.weightNight?.let { "Night ${it}kg" },
            )
            if (weightParts.isNotEmpty()) {
                sb.appendLine("Weight: ${weightParts.joinToString(", ")}")
            }

            // Nutrition
            val dayNutrition = nutritionSummaries.find { it.date.toString() == dateStr }
            if (dayNutrition != null) {
                sb.appendLine("Nutrition consumed: ${dayNutrition.totalCalories.toInt()} cal, " +
                    "${dayNutrition.totalProtein.toInt()}g protein, " +
                    "${dayNutrition.totalCarbs.toInt()}g carbs, " +
                    "${dayNutrition.totalFat.toInt()}g fat, " +
                    "${dayNutrition.waterLiters}L water")
            } else {
                sb.appendLine("Nutrition consumed: No data")
            }

            // Calorie deficit
            val burned = override?.totalCalories
            val consumed = dayNutrition?.totalCalories
            if (burned != null && consumed != null) {
                val deficit = burned - consumed
                val label = if (deficit >= 0) "deficit" else "surplus"
                sb.appendLine("Calories burned: ${burned.toInt()} kcal, $label: ${deficit.toInt()} kcal")
            } else if (burned != null) {
                sb.appendLine("Calories burned: ${burned.toInt()} kcal")
            }

            // Individual food items per meal
            val dayMealEntries = dailyMealEntryDao.getByDate(date).first()
            if (dayMealEntries.isNotEmpty()) {
                // Pre-fetch food names to avoid suspend calls inside lambdas
                val foodIds = dayMealEntries.map { it.foodId }.distinct()
                val foodNames = mutableMapOf<String, String>()
                for (id in foodIds) {
                    foodItemDao.getById(id)?.let { foodNames[id] = it.name }
                }
                val mealGroups = dayMealEntries.groupBy { it.mealTime }
                val mealStr = mealGroups.entries.joinToString("; ") { (time, entries) ->
                    val items = entries.mapNotNull { entry ->
                        foodNames[entry.foodId]?.let { "$it x${entry.amount}" }
                    }
                    "$time: ${items.joinToString(", ")}"
                }
                sb.appendLine("Meals: $mealStr")
            }

            // Expenses
            val dayExpenses = expenses.filter { it.date.toString() == dateStr }
            if (dayExpenses.isNotEmpty()) {
                val total = dayExpenses.sumOf { it.totalAmount }
                val categories = dayExpenses
                    .groupBy { it.category ?: "Uncategorized" }
                    .entries
                    .joinToString(", ") { (cat, items) ->
                        "$cat \u20B9${items.sumOf { it.totalAmount }.toInt()}"
                    }
                sb.appendLine("Expenses: \u20B9${total.toInt()} total [$categories]")
            } else {
                sb.appendLine("Expenses: No data")
            }

            // Journal
            val dayJournal = journalEntries.find { it.date.toString() == dateStr }
            if (dayJournal != null) {
                val excerpt = dayJournal.content?.take(100)?.let { "\"$it\"" } ?: ""
                val tags = if (dayJournal.tags.isNotEmpty()) "tags ${dayJournal.tags}" else ""
                val mood = dayJournal.mood?.let { "mood $it/10" } ?: ""
                sb.appendLine("Journal: ${listOf(mood, tags, excerpt).filter { it.isNotEmpty() }.joinToString(", ")}")
            } else {
                sb.appendLine("Journal: No data")
            }

            // Sports
            val dayEvents = sportEvents.filter {
                it.scheduledAt.toLocalDateTime(tz).date == date
            }
            if (dayEvents.isNotEmpty()) {
                val eventsStr = dayEvents.joinToString("; ") { event ->
                    val name = event.eventName ?: "Event"
                    val score = if (event.homeScore != null && event.awayScore != null) {
                        "${event.homeScore}-${event.awayScore}"
                    } else {
                        event.status
                    }
                    "$name ($score)"
                }
                sb.appendLine("Sports: $eventsStr")
            } else {
                sb.appendLine("Sports: No data")
            }

            sb.appendLine()
        }

        // Media summary (not date-specific)
        if (mediaItems.isNotEmpty()) {
            sb.appendLine("=== Active Media ===")
            val inProgress = mediaItems.filter { it.status == "IN_PROGRESS" }
            val recentDone = mediaItems.filter { it.status == "DONE" }.take(5)
            if (inProgress.isNotEmpty()) {
                sb.appendLine("Currently consuming: ${inProgress.joinToString(", ") { "${it.title} (${it.mediaType})" }}")
            }
            if (recentDone.isNotEmpty()) {
                sb.appendLine("Recently completed: ${recentDone.joinToString(", ") { "${it.title} (${it.mediaType}, score ${it.score ?: "unrated"})" }}")
            }
            sb.appendLine()
        }

        // Expense category totals for the period
        if (categoryTotals.isNotEmpty()) {
            sb.appendLine("=== Expense Summary ($startDate to $endDate) ===")
            val totalSpent = categoryTotals.sumOf { it.total }
            sb.appendLine("Total spent: \u20B9${totalSpent.toInt()}")
            categoryTotals.forEach { ct ->
                sb.appendLine("  ${ct.category}: \u20B9${ct.total.toInt()} (${ct.count} transactions)")
            }
        }

        return sb.toString()
    }

    private fun inferDateRange(question: String): Pair<LocalDate, LocalDate> {
        val now = kotlin.time.Clock.System.now()
        val today = now.toLocalDateTime(tz).date

        val lower = question.lowercase()

        // Check for custom numeric ranges: "last N days", "past N days", "last N weeks"
        val daysPattern = Regex("""(?:last|past)\s+(\d+)\s+days?""")
        val weeksPattern = Regex("""(?:last|past)\s+(\d+)\s+weeks?""")
        val monthsPattern = Regex("""(?:last|past)\s+(\d+)\s+months?""")

        daysPattern.find(lower)?.let { match ->
            val n = match.groupValues[1].toIntOrNull() ?: 7
            return today.minus(DatePeriod(days = n - 1)) to today
        }

        weeksPattern.find(lower)?.let { match ->
            val n = match.groupValues[1].toIntOrNull() ?: 1
            return today.minus(DatePeriod(days = n * 7 - 1)) to today
        }

        monthsPattern.find(lower)?.let { match ->
            val n = match.groupValues[1].toIntOrNull() ?: 1
            return today.minus(DatePeriod(months = n)) to today
        }

        return when {
            lower.contains("today") -> today to today
            lower.contains("yesterday") -> {
                val yesterday = today.minus(DatePeriod(days = 1))
                yesterday to yesterday
            }
            lower.contains("this week") -> today.minus(DatePeriod(days = 6)) to today
            lower.contains("last week") -> {
                val weekAgo = today.minus(DatePeriod(days = 7))
                today.minus(DatePeriod(days = 13)) to weekAgo
            }
            lower.contains("this month") -> LocalDate(today.year, today.month, 1) to today
            lower.contains("last month") -> {
                val firstOfThisMonth = LocalDate(today.year, today.month, 1)
                val lastMonthEnd = firstOfThisMonth.minus(DatePeriod(days = 1))
                LocalDate(lastMonthEnd.year, lastMonthEnd.month, 1) to lastMonthEnd
            }
            else -> today.minus(DatePeriod(days = 6)) to today // default: last 7 days
        }
    }

    private fun formatHealthMetrics(metrics: List<HealthMetricEntity>): String {
        if (metrics.isEmpty()) return "No data"
        val parts = mutableListOf<String>()
        metrics.groupBy { it.type }.forEach { (type, values) ->
            when (type) {
                "STEPS" -> parts.add("${values.sumOf { it.value.toInt() }} steps")
                "HR" -> parts.add("avg HR ${values.map { it.value }.average().toInt()} bpm")
                "RESTING_HR" -> parts.add("resting HR ${values.last().value.toInt()}")
                "SPO2" -> parts.add("SpO2 ${values.last().value.toInt()}%")
                "HRV" -> parts.add("HRV ${values.last().value.toInt()}ms")
                "FLOORS" -> parts.add("${values.sumOf { it.value.toInt() }} floors")
                "VO2MAX" -> parts.add("VO2max ${values.last().value}")
                "WEIGHT" -> parts.add("weight ${values.last().value}kg")
                else -> parts.add("$type ${values.last().value}${values.last().unit}")
            }
        }
        return parts.joinToString(", ")
    }

    private fun formatSleep(sessions: List<SleepSessionEntity>): String {
        if (sessions.isEmpty()) return "No data"
        val session = sessions.first()
        val hours = session.totalMinutes / 60.0
        val scoreStr = session.score?.let { ", score $it/100" } ?: ""
        return "%.1fh total (deep %dmin, REM %dmin, light %dmin, awake %dmin%s)".format(
            hours, session.deepMinutes, session.remMinutes, session.lightMinutes, session.awakeMinutes, scoreStr
        )
    }

    private fun formatExercises(exercises: List<ExerciseSessionEntity>): String {
        if (exercises.isEmpty()) return "No data"
        return exercises.joinToString("; ") { ex ->
            val duration = (ex.endTime - ex.startTime).inWholeMinutes
            val parts = mutableListOf("${ex.exerciseType} ${duration}min")
            ex.calories?.let { parts.add("${it.toInt()}cal") }
            ex.avgHeartRate?.let { parts.add("avgHR $it") }
            ex.distance?.let { parts.add("${String.format("%.1f", it / 1000.0)}km") }
            parts.joinToString(" ")
        }
    }

    private fun generateDateSequence(start: LocalDate, end: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var current = start
        while (current <= end) {
            dates.add(current)
            current = current.plus(DatePeriod(days = 1))
        }
        return dates
    }
}
