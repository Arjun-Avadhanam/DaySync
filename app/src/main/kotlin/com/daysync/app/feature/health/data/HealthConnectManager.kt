package com.daysync.app.feature.health.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.daysync.app.core.database.entity.ExerciseSessionEntity
import com.daysync.app.core.database.entity.HealthMetricEntity
import com.daysync.app.core.database.entity.SleepSessionEntity
import com.daysync.app.feature.health.model.HeartRateZoneConfig
import androidx.health.connect.client.records.ExerciseRouteResult
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class HealthConnectManager(private val context: Context) {

    private val healthConnectClient: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ElevationGainedRecord::class),
        HealthPermission.getReadPermission(FloorsClimbedRecord::class),
        HealthPermission.getReadPermission(SpeedRecord::class),
        HealthPermission.getReadPermission(Vo2MaxRecord::class),
        HealthPermission.getReadPermission(SkinTemperatureRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
    )

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    suspend fun hasAllPermissions(): Boolean {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return permissions.all { it in granted }
    }

    suspend fun getGrantedPermissions(): Set<String> =
        healthConnectClient.permissionController.getGrantedPermissions()

    // ── Daily aggregate metrics ──────────────────────────────────────────

    suspend fun readDailyMetrics(
        start: Instant,
        end: Instant,
    ): List<HealthMetricEntity> {
        val timeRange = TimeRangeFilter.between(start, end)
        val metrics = mutableListOf<HealthMetricEntity>()
        val ts = kotlinInstant(start)

        // Steps
        tryAggregate(timeRange, setOf(StepsRecord.COUNT_TOTAL))?.let { result ->
            result[StepsRecord.COUNT_TOTAL]?.let { steps ->
                metrics += metric("STEPS", steps.toDouble(), "count", ts)
            }
        }

        // Total calories
        tryAggregate(timeRange, setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL))?.let { result ->
            result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.let { energy ->
                metrics += metric("TOTAL_CALORIES", energy.inKilocalories, "kcal", ts)
            }
        }

        // Active calories
        tryAggregate(timeRange, setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL))?.let { result ->
            result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.let { energy ->
                metrics += metric("ACTIVE_CALORIES", energy.inKilocalories, "kcal", ts)
            }
        }

        // Heart rate aggregates
        tryAggregate(
            timeRange,
            setOf(HeartRateRecord.BPM_AVG, HeartRateRecord.BPM_MAX, HeartRateRecord.BPM_MIN),
        )?.let { result ->
            result[HeartRateRecord.BPM_AVG]?.let { metrics += metric("HR_AVG", it.toDouble(), "bpm", ts) }
            result[HeartRateRecord.BPM_MAX]?.let { metrics += metric("HR_MAX", it.toDouble(), "bpm", ts) }
            result[HeartRateRecord.BPM_MIN]?.let { metrics += metric("HR_MIN", it.toDouble(), "bpm", ts) }
        }

        // Resting heart rate
        readRecords<RestingHeartRateRecord>(timeRange)?.lastOrNull()?.let {
            metrics += metric("RESTING_HR", it.beatsPerMinute.toDouble(), "bpm", ts)
        }

        // HRV
        readRecords<HeartRateVariabilityRmssdRecord>(timeRange)?.lastOrNull()?.let {
            metrics += metric("HRV", it.heartRateVariabilityMillis, "ms", ts)
        }

        // SpO2
        readRecords<OxygenSaturationRecord>(timeRange)?.lastOrNull()?.let {
            metrics += metric("SPO2", it.percentage.value, "%", ts)
        }

        // Floors
        tryAggregate(timeRange, setOf(FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL))?.let { result ->
            result[FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL]?.let {
                metrics += metric("FLOORS", it, "floors", ts)
            }
        }

        // Elevation gained
        tryAggregate(timeRange, setOf(ElevationGainedRecord.ELEVATION_GAINED_TOTAL))?.let { result ->
            result[ElevationGainedRecord.ELEVATION_GAINED_TOTAL]?.let { length ->
                metrics += metric("ELEVATION", length.inMeters, "m", ts)
            }
        }

        // VO2 Max
        readRecords<Vo2MaxRecord>(timeRange)?.lastOrNull()?.let {
            metrics += metric("VO2MAX", it.vo2MillilitersPerMinuteKilogram, "mL/kg/min", ts)
        }

        // Skin temperature
        readRecords<SkinTemperatureRecord>(timeRange)?.firstOrNull()?.let { record ->
            record.baseline?.let { temp ->
                metrics += metric("SKIN_TEMP", temp.inCelsius, "°C", ts)
            }
        }

        // Weight
        readRecords<WeightRecord>(timeRange)?.lastOrNull()?.let {
            metrics += metric("WEIGHT", it.weight.inKilograms, "kg", ts)
        }

        return metrics
    }

    // ── Sleep sessions ───────────────────────────────────────────────────

    suspend fun readSleepSessions(
        start: Instant,
        end: Instant,
    ): List<SleepSessionEntity> {
        val records = readRecords<SleepSessionRecord>(
            TimeRangeFilter.between(start, end),
        ) ?: return emptyList()

        return records.map { session ->
            var deepMin = 0
            var lightMin = 0
            var remMin = 0
            var awakeMin = 0

            session.stages.forEach { stage ->
                val durationMin = java.time.Duration.between(stage.startTime, stage.endTime)
                    .toMinutes().toInt()
                when (stage.stage) {
                    SleepSessionRecord.STAGE_TYPE_DEEP -> deepMin += durationMin
                    SleepSessionRecord.STAGE_TYPE_LIGHT -> lightMin += durationMin
                    SleepSessionRecord.STAGE_TYPE_REM -> remMin += durationMin
                    SleepSessionRecord.STAGE_TYPE_AWAKE -> awakeMin += durationMin
                }
            }

            val totalMin = java.time.Duration.between(session.startTime, session.endTime)
                .toMinutes().toInt()

            SleepSessionEntity(
                id = session.metadata.id,
                startTime = kotlinInstant(session.startTime),
                endTime = kotlinInstant(session.endTime),
                totalMinutes = totalMin,
                deepMinutes = deepMin,
                lightMinutes = lightMin,
                remMinutes = remMin,
                awakeMinutes = awakeMin,
            )
        }
    }

    // ── Exercise sessions (enriched) ─────────────────────────────────────

    suspend fun readExerciseSessions(
        start: Instant,
        end: Instant,
        zoneConfig: HeartRateZoneConfig,
    ): List<ExerciseSessionEntity> {
        val records = readRecords<ExerciseSessionRecord>(
            TimeRangeFilter.between(start, end),
        ) ?: return emptyList()

        return records.map { session ->
            enrichExerciseSession(session, zoneConfig)
        }
    }

    private suspend fun enrichExerciseSession(
        session: ExerciseSessionRecord,
        zoneConfig: HeartRateZoneConfig,
    ): ExerciseSessionEntity {
        val sessionTimeRange = TimeRangeFilter.between(session.startTime, session.endTime)

        // Aggregate common metrics for this session's time window
        val aggregateResult = tryAggregate(
            sessionTimeRange,
            setOf(
                ExerciseSessionRecord.EXERCISE_DURATION_TOTAL,
                HeartRateRecord.BPM_AVG,
                HeartRateRecord.BPM_MAX,
                HeartRateRecord.BPM_MIN,
                TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                StepsRecord.COUNT_TOTAL,
                DistanceRecord.DISTANCE_TOTAL,
                ElevationGainedRecord.ELEVATION_GAINED_TOTAL,
            ),
        )

        // Running-specific aggregates
        val speedResult = tryAggregate(
            sessionTimeRange,
            setOf(SpeedRecord.SPEED_AVG, SpeedRecord.SPEED_MAX),
        )

        val cadenceResult = tryAggregate(
            sessionTimeRange,
            setOf(StepsCadenceRecord.RATE_AVG, StepsCadenceRecord.RATE_MAX),
        )

        // HR zones from raw samples
        val hrZones = computeHrZones(sessionTimeRange, zoneConfig)

        // Laps
        val lapsJson = if (session.laps.isNotEmpty()) {
            val lapData = session.laps.mapIndexed { index, lap ->
                LapData(
                    lapNumber = index + 1,
                    distanceMeters = lap.length?.inMeters,
                    durationSeconds = java.time.Duration.between(lap.startTime, lap.endTime)
                        .seconds.toInt(),
                )
            }
            Json.encodeToString(lapData)
        } else {
            null
        }

        return ExerciseSessionEntity(
            id = session.metadata.id,
            exerciseType = resolveExerciseType(session.exerciseType, session.title),
            startTime = kotlinInstant(session.startTime),
            endTime = kotlinInstant(session.endTime),
            title = session.title,
            calories = aggregateResult?.get(TotalCaloriesBurnedRecord.ENERGY_TOTAL)?.inKilocalories,
            activeCalories = aggregateResult?.get(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)?.inKilocalories,
            avgHeartRate = aggregateResult?.get(HeartRateRecord.BPM_AVG)?.toInt(),
            maxHeartRate = aggregateResult?.get(HeartRateRecord.BPM_MAX)?.toInt(),
            minHeartRate = aggregateResult?.get(HeartRateRecord.BPM_MIN)?.toInt(),
            distance = aggregateResult?.get(DistanceRecord.DISTANCE_TOTAL)?.inMeters,
            elevationGain = aggregateResult?.get(ElevationGainedRecord.ELEVATION_GAINED_TOTAL)?.inMeters,
            steps = aggregateResult?.get(StepsRecord.COUNT_TOTAL),
            activeDurationMs = aggregateResult?.get(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL)?.toMillis(),
            avgSpeedMps = speedResult?.get(SpeedRecord.SPEED_AVG)?.inMetersPerSecond,
            maxSpeedMps = speedResult?.get(SpeedRecord.SPEED_MAX)?.inMetersPerSecond,
            avgCadenceSpm = cadenceResult?.get(StepsCadenceRecord.RATE_AVG),
            maxCadenceSpm = cadenceResult?.get(StepsCadenceRecord.RATE_MAX),
            zone1Seconds = hrZones[0],
            zone2Seconds = hrZones[1],
            zone3Seconds = hrZones[2],
            zone4Seconds = hrZones[3],
            zone5Seconds = hrZones[4],
            laps = lapsJson,
            hasRoute = session.exerciseRouteResult is ExerciseRouteResult.Data,
            notes = session.notes,
        )
    }

    private suspend fun computeHrZones(
        timeRange: TimeRangeFilter,
        config: HeartRateZoneConfig,
    ): IntArray {
        val zones = IntArray(5) // seconds per zone
        val hrRecords = readRecords<HeartRateRecord>(timeRange) ?: return zones

        val allSamples = hrRecords.flatMap { it.samples }.sortedBy { it.time }
        if (allSamples.size < 2) return zones

        for (i in 0 until allSamples.size - 1) {
            val sample = allSamples[i]
            val nextSample = allSamples[i + 1]
            val durationSec = java.time.Duration.between(sample.time, nextSample.time)
                .seconds.toInt().coerceAtMost(60) // cap gap at 60s
            val zone = config.zoneForBpm(sample.beatsPerMinute)
            if (zone in 1..5) {
                zones[zone - 1] += durationSec
            }
        }
        return zones
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private suspend inline fun <reified T : androidx.health.connect.client.records.Record> readRecords(
        timeRange: TimeRangeFilter,
    ): List<T>? = try {
        healthConnectClient.readRecords(
            ReadRecordsRequest(T::class, timeRangeFilter = timeRange),
        ).records
    } catch (e: Exception) {
        android.util.Log.w("HealthConnect", "Failed to read ${T::class.simpleName}: ${e.message}")
        null
    }

    private suspend fun tryAggregate(
        timeRange: TimeRangeFilter,
        metrics: Set<androidx.health.connect.client.aggregate.AggregateMetric<*>>,
    ) = try {
        healthConnectClient.aggregate(
            AggregateRequest(metrics = metrics, timeRangeFilter = timeRange),
        )
    } catch (e: Exception) {
        android.util.Log.w("HealthConnect", "Failed to aggregate ${metrics.map { it.metricKey }}: ${e.message}")
        null
    }

    // Deterministic ID so re-syncing the same day's metric replaces the
    // old value instead of accumulating duplicate rows. The old random UUID
    // pattern left stale aggregates (e.g. 7-day summed steps on Monday)
    // in Room permanently.
    private fun metric(
        type: String,
        value: Double,
        unit: String,
        timestamp: kotlin.time.Instant,
    ) = HealthMetricEntity(
        id = "$type-${timestamp.toEpochMilliseconds()}",
        type = type,
        value = value,
        unit = unit,
        timestamp = timestamp,
    )

    private fun kotlinInstant(javaInstant: Instant): kotlin.time.Instant =
        kotlin.time.Instant.fromEpochMilliseconds(javaInstant.toEpochMilli())

    // OHealth writes many workout types as OTHER_WORKOUT and puts the real
    // name in the title field. Recover known types from the title so the
    // rest of the app (sub-type picker, type charts) works correctly.
    private fun resolveExerciseType(rawType: Int, title: String?): String {
        val mapped = mapExerciseType(rawType)
        if (mapped != "EXERCISE_TYPE_OTHER_WORKOUT") return mapped
        return when (title?.trim()?.lowercase()) {
            "strength training" -> "EXERCISE_TYPE_STRENGTH_TRAINING"
            else -> mapped
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun mapExerciseType(type: Int): String = when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON -> "EXERCISE_TYPE_BADMINTON"
        ExerciseSessionRecord.EXERCISE_TYPE_BASEBALL -> "EXERCISE_TYPE_BASEBALL"
        ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL -> "EXERCISE_TYPE_BASKETBALL"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "EXERCISE_TYPE_BIKING"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> "EXERCISE_TYPE_BIKING_STATIONARY"
        ExerciseSessionRecord.EXERCISE_TYPE_BOOT_CAMP -> "EXERCISE_TYPE_BOOT_CAMP"
        ExerciseSessionRecord.EXERCISE_TYPE_BOXING -> "EXERCISE_TYPE_BOXING"
        ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS -> "EXERCISE_TYPE_CALISTHENICS"
        ExerciseSessionRecord.EXERCISE_TYPE_CRICKET -> "EXERCISE_TYPE_CRICKET"
        ExerciseSessionRecord.EXERCISE_TYPE_DANCING -> "EXERCISE_TYPE_DANCING"
        ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> "EXERCISE_TYPE_ELLIPTICAL"
        ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS -> "EXERCISE_TYPE_EXERCISE_CLASS"
        ExerciseSessionRecord.EXERCISE_TYPE_FENCING -> "EXERCISE_TYPE_FENCING"
        ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN -> "EXERCISE_TYPE_FOOTBALL_AMERICAN"
        ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AUSTRALIAN -> "EXERCISE_TYPE_FOOTBALL_AUSTRALIAN"
        ExerciseSessionRecord.EXERCISE_TYPE_FRISBEE_DISC -> "EXERCISE_TYPE_FRISBEE_DISC"
        ExerciseSessionRecord.EXERCISE_TYPE_GOLF -> "EXERCISE_TYPE_GOLF"
        ExerciseSessionRecord.EXERCISE_TYPE_GUIDED_BREATHING -> "EXERCISE_TYPE_GUIDED_BREATHING"
        ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS -> "EXERCISE_TYPE_GYMNASTICS"
        ExerciseSessionRecord.EXERCISE_TYPE_HANDBALL -> "EXERCISE_TYPE_HANDBALL"
        ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> "EXERCISE_TYPE_HIIT"
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "EXERCISE_TYPE_HIKING"
        ExerciseSessionRecord.EXERCISE_TYPE_ICE_HOCKEY -> "EXERCISE_TYPE_ICE_HOCKEY"
        ExerciseSessionRecord.EXERCISE_TYPE_ICE_SKATING -> "EXERCISE_TYPE_ICE_SKATING"
        ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS -> "EXERCISE_TYPE_MARTIAL_ARTS"
        ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT -> "EXERCISE_TYPE_OTHER_WORKOUT"
        ExerciseSessionRecord.EXERCISE_TYPE_PADDLING -> "EXERCISE_TYPE_PADDLING"
        ExerciseSessionRecord.EXERCISE_TYPE_PARAGLIDING -> "EXERCISE_TYPE_PARAGLIDING"
        ExerciseSessionRecord.EXERCISE_TYPE_PILATES -> "EXERCISE_TYPE_PILATES"
        ExerciseSessionRecord.EXERCISE_TYPE_RACQUETBALL -> "EXERCISE_TYPE_RACQUETBALL"
        ExerciseSessionRecord.EXERCISE_TYPE_ROCK_CLIMBING -> "EXERCISE_TYPE_ROCK_CLIMBING"
        ExerciseSessionRecord.EXERCISE_TYPE_ROLLER_HOCKEY -> "EXERCISE_TYPE_ROLLER_HOCKEY"
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING -> "EXERCISE_TYPE_ROWING"
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> "EXERCISE_TYPE_ROWING_MACHINE"
        ExerciseSessionRecord.EXERCISE_TYPE_RUGBY -> "EXERCISE_TYPE_RUGBY"
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "EXERCISE_TYPE_RUNNING"
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> "EXERCISE_TYPE_RUNNING_TREADMILL"
        ExerciseSessionRecord.EXERCISE_TYPE_SAILING -> "EXERCISE_TYPE_SAILING"
        ExerciseSessionRecord.EXERCISE_TYPE_SCUBA_DIVING -> "EXERCISE_TYPE_SCUBA_DIVING"
        ExerciseSessionRecord.EXERCISE_TYPE_SKATING -> "EXERCISE_TYPE_SKATING"
        ExerciseSessionRecord.EXERCISE_TYPE_SKIING -> "EXERCISE_TYPE_SKIING"
        ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING -> "EXERCISE_TYPE_SNOWBOARDING"
        ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING -> "EXERCISE_TYPE_SNOWSHOEING"
        ExerciseSessionRecord.EXERCISE_TYPE_SOCCER -> "EXERCISE_TYPE_SOCCER"
        ExerciseSessionRecord.EXERCISE_TYPE_SOFTBALL -> "EXERCISE_TYPE_SOFTBALL"
        ExerciseSessionRecord.EXERCISE_TYPE_SQUASH -> "EXERCISE_TYPE_SQUASH"
        ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING -> "EXERCISE_TYPE_STAIR_CLIMBING"
        ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE -> "EXERCISE_TYPE_STAIR_CLIMBING_MACHINE"
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> "EXERCISE_TYPE_STRENGTH_TRAINING"
        ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING -> "EXERCISE_TYPE_STRETCHING"
        ExerciseSessionRecord.EXERCISE_TYPE_SURFING -> "EXERCISE_TYPE_SURFING"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "EXERCISE_TYPE_SWIMMING_OPEN_WATER"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> "EXERCISE_TYPE_SWIMMING_POOL"
        ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS -> "EXERCISE_TYPE_TABLE_TENNIS"
        ExerciseSessionRecord.EXERCISE_TYPE_TENNIS -> "EXERCISE_TYPE_TENNIS"
        ExerciseSessionRecord.EXERCISE_TYPE_VOLLEYBALL -> "EXERCISE_TYPE_VOLLEYBALL"
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "EXERCISE_TYPE_WALKING"
        ExerciseSessionRecord.EXERCISE_TYPE_WATER_POLO -> "EXERCISE_TYPE_WATER_POLO"
        ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> "EXERCISE_TYPE_WEIGHTLIFTING"
        ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR -> "EXERCISE_TYPE_WHEELCHAIR"
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "EXERCISE_TYPE_YOGA"
        else -> "EXERCISE_TYPE_OTHER_WORKOUT"
    }

    @Serializable
    data class LapData(
        val lapNumber: Int,
        val distanceMeters: Double?,
        val durationSeconds: Int,
    )
}
