package com.daysync.app.feature.health.model

import com.daysync.app.core.database.entity.ExerciseSessionEntity
import com.daysync.app.core.database.entity.SleepSessionEntity
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HealthModelsTest {

    // ── HeartRateZoneConfig.zoneForBpm ──────────────────

    private val config = HeartRateZoneConfig()

    @Test
    fun `zone 0 warmup below 96`() {
        assertEquals(0, config.zoneForBpm(60))
        assertEquals(0, config.zoneForBpm(95))
    }

    @Test
    fun `zone 1 fat burning 96-123`() {
        assertEquals(1, config.zoneForBpm(96))
        assertEquals(1, config.zoneForBpm(123))
    }

    @Test
    fun `zone 2 endurance 124-144`() {
        assertEquals(2, config.zoneForBpm(124))
        assertEquals(2, config.zoneForBpm(144))
    }

    @Test
    fun `zone 3 anaerobic 145-165`() {
        assertEquals(3, config.zoneForBpm(145))
        assertEquals(3, config.zoneForBpm(165))
    }

    @Test
    fun `zone 4 threshold 166-179`() {
        assertEquals(4, config.zoneForBpm(166))
        assertEquals(4, config.zoneForBpm(179))
    }

    @Test
    fun `zone 5 above threshold 180+`() {
        assertEquals(5, config.zoneForBpm(180))
        assertEquals(5, config.zoneForBpm(200))
    }

    @Test
    fun `zone boundary at exactly zone ceiling`() {
        // zone1Ceiling = 96, so 96 is in zone 1 (not zone 0)
        assertEquals(1, config.zoneForBpm(96))
    }

    @Test
    fun `zone with custom config`() {
        val custom = HeartRateZoneConfig(
            zone1Ceiling = 100,
            zone2Ceiling = 130,
            zone3Ceiling = 150,
            zone4Ceiling = 170,
            zone5Ceiling = 190,
        )
        assertEquals(0, custom.zoneForBpm(99))
        assertEquals(1, custom.zoneForBpm(100))
        assertEquals(2, custom.zoneForBpm(130))
    }

    @Test
    fun `zone names`() {
        assertEquals(5, config.zoneNames.size)
        assertEquals("Warmup", config.zoneNames[0])
        assertEquals("Threshold", config.zoneNames[4])
    }

    // ── SleepSummary ────────────────────────────────────

    private val baseInstant = Instant.fromEpochMilliseconds(1710504000000L)

    private fun makeSleepEntity(
        totalMinutes: Int = 480,
        deepMinutes: Int = 120,
        lightMinutes: Int = 200,
        remMinutes: Int = 100,
        awakeMinutes: Int = 60,
    ) = SleepSessionEntity(
        id = "sleep-1",
        startTime = baseInstant,
        endTime = baseInstant + totalMinutes.minutes,
        totalMinutes = totalMinutes,
        deepMinutes = deepMinutes,
        lightMinutes = lightMinutes,
        remMinutes = remMinutes,
        awakeMinutes = awakeMinutes,
    )

    @Test
    fun `totalHours converts minutes`() {
        val summary = SleepSummary(makeSleepEntity(totalMinutes = 480))
        assertEquals(8.0, summary.totalHours, 0.01)
    }

    @Test
    fun `totalHours fractional`() {
        val summary = SleepSummary(makeSleepEntity(totalMinutes = 450))
        assertEquals(7.5, summary.totalHours, 0.01)
    }

    @Test
    fun `deepPercent calculation`() {
        val summary = SleepSummary(makeSleepEntity(totalMinutes = 480, deepMinutes = 120))
        assertEquals(25, summary.deepPercent) // 120/480 = 25%
    }

    @Test
    fun `lightPercent calculation`() {
        val summary = SleepSummary(makeSleepEntity(totalMinutes = 480, lightMinutes = 200))
        assertEquals(41, summary.lightPercent) // 200/480 = 41.6% -> 41 (int division)
    }

    @Test
    fun `remPercent calculation`() {
        val summary = SleepSummary(makeSleepEntity(totalMinutes = 480, remMinutes = 100))
        assertEquals(20, summary.remPercent) // 100/480 = 20.8% -> 20
    }

    @Test
    fun `awakePercent calculation`() {
        val summary = SleepSummary(makeSleepEntity(totalMinutes = 480, awakeMinutes = 60))
        assertEquals(12, summary.awakePercent) // 60/480 = 12.5% -> 12
    }

    @Test
    fun `zero totalMinutes gives 0 percent`() {
        val summary = SleepSummary(makeSleepEntity(totalMinutes = 0, deepMinutes = 0))
        assertEquals(0, summary.deepPercent)
        assertEquals(0, summary.lightPercent)
        assertEquals(0, summary.remPercent)
        assertEquals(0, summary.awakePercent)
    }

    // ── WorkoutSummary ──────────────────────────────────

    private fun makeExerciseEntity(
        exerciseType: String = "EXERCISE_TYPE_RUNNING",
        durationMinutes: Long = 30,
        distance: Double? = 5000.0,
        activeDurationMs: Long? = null,
    ) = ExerciseSessionEntity(
        id = "ex-1",
        exerciseType = exerciseType,
        startTime = baseInstant,
        endTime = baseInstant + durationMinutes.minutes,
        distance = distance,
        activeDurationMs = activeDurationMs,
    )

    @Test
    fun `durationMinutes from start and end time`() {
        val summary = WorkoutSummary(makeExerciseEntity(durationMinutes = 45))
        assertEquals(45, summary.durationMinutes)
    }

    @Test
    fun `durationMinutes prefers activeDurationMs`() {
        val summary = WorkoutSummary(makeExerciseEntity(
            durationMinutes = 45,
            activeDurationMs = 30 * 60_000L, // 30 min
        ))
        assertEquals(30, summary.durationMinutes)
    }

    @Test
    fun `paceMinPerKm calculates correctly`() {
        // 30 min for 5km = 6 min/km
        val summary = WorkoutSummary(makeExerciseEntity(durationMinutes = 30, distance = 5000.0))
        assertEquals(6.0, summary.paceMinPerKm!!, 0.01)
    }

    @Test
    fun `paceMinPerKm null when distance is null`() {
        val summary = WorkoutSummary(makeExerciseEntity(distance = null))
        assertNull(summary.paceMinPerKm)
    }

    @Test
    fun `paceMinPerKm null when distance is zero`() {
        val summary = WorkoutSummary(makeExerciseEntity(distance = 0.0))
        assertNull(summary.paceMinPerKm)
    }

    @Test
    fun `paceMinPerKm null when distance is negative`() {
        val summary = WorkoutSummary(makeExerciseEntity(distance = -100.0))
        assertNull(summary.paceMinPerKm)
    }

    @Test
    fun `displayType formats exercise type`() {
        val summary = WorkoutSummary(makeExerciseEntity(exerciseType = "EXERCISE_TYPE_RUNNING"))
        assertEquals("Running", summary.displayType)
    }

    @Test
    fun `displayType formats multi-word exercise type`() {
        val summary = WorkoutSummary(makeExerciseEntity(exerciseType = "EXERCISE_TYPE_WEIGHT_TRAINING"))
        assertEquals("Weight training", summary.displayType)
    }

    @Test
    fun `displayType formats type without prefix`() {
        val summary = WorkoutSummary(makeExerciseEntity(exerciseType = "YOGA"))
        assertEquals("Yoga", summary.displayType)
    }

    // ── HealthPeriod ────────────────────────────────────

    @Test
    fun `HealthPeriod values`() {
        assertEquals(1, HealthPeriod.DAILY.days)
        assertEquals(7, HealthPeriod.WEEKLY.days)
        assertEquals(30, HealthPeriod.MONTHLY.days)
    }

    @Test
    fun `HealthPeriod labels`() {
        assertEquals("Today", HealthPeriod.DAILY.label)
        assertEquals("7 Days", HealthPeriod.WEEKLY.label)
        assertEquals("30 Days", HealthPeriod.MONTHLY.label)
    }
}
