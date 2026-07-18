package com.creamaker.changli_planet_app.overview.data.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import java.util.Calendar

object OverviewLocalCache {
    private const val CACHE_ID = "overview_local_cache"
    private const val KEY_ELECTRICITY_HISTORY = "electricity_history"
    private const val KEY_LAST_ELECTRICITY = "last_electricity"
    private const val KEY_PREV_ELECTRICITY = "prev_electricity"
    private const val KEY_LAST_ELECTRICITY_TIME = "last_electricity_time"
    private const val KEY_PREV_ELECTRICITY_TIME = "prev_electricity_time"
    private const val MAX_ELECTRICITY_HISTORY_SIZE = 1000
    private const val HISTORY_RETENTION_DAYS = 100
    private val mmkv by lazy { MMKV.mmkvWithID(CACHE_ID) }
    private val gson by lazy { Gson() }

    fun saveElectricitySnapshot(value: Float) {
        appendElectricityHistory(value)
        val oldValue = mmkv.decodeFloat(KEY_LAST_ELECTRICITY, Float.NaN)
        val oldTime = mmkv.decodeLong(KEY_LAST_ELECTRICITY_TIME, 0L)
        if (!oldValue.isNaN() && oldTime > 0L) {
            mmkv.encode(KEY_PREV_ELECTRICITY, oldValue)
            mmkv.encode(KEY_PREV_ELECTRICITY_TIME, oldTime)
        }
        mmkv.encode(KEY_LAST_ELECTRICITY, value)
        mmkv.encode(KEY_LAST_ELECTRICITY_TIME, System.currentTimeMillis())
    }

    fun getElectricitySnapshot(): ElectricitySnapshot? {
        val lastValue = mmkv.decodeFloat(KEY_LAST_ELECTRICITY, Float.NaN)
        val lastTime = mmkv.decodeLong(KEY_LAST_ELECTRICITY_TIME, 0L)
        if (lastValue.isNaN() || lastTime <= 0L) return null

        val prevValue = mmkv.decodeFloat(KEY_PREV_ELECTRICITY, Float.NaN)
        val prevTime = mmkv.decodeLong(KEY_PREV_ELECTRICITY_TIME, 0L)
        return ElectricitySnapshot(
            lastValue = lastValue,
            lastTime = lastTime,
            previousValue = prevValue.takeUnless { it.isNaN() },
            previousTime = prevTime.takeIf { it > 0L },
            history = getElectricityHistory()
        )
    }

    fun getElectricityHistory(): List<ElectricityHistoryEntry> {
        val json = mmkv.decodeString(KEY_ELECTRICITY_HISTORY) ?: return emptyList()
        return runCatching {
            gson.fromJson<List<ElectricityHistoryEntry>>(
                json,
                object : TypeToken<List<ElectricityHistoryEntry>>() {}.type
            ).orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun appendElectricityHistory(value: Float) {
        val now = System.currentTimeMillis()
        val history = getElectricityHistory().toMutableList()
        val last = history.lastOrNull()
        // Keep every real query as a trend sample, even when the balance is unchanged.
        // Only coalesce an accidental duplicate fired within the same second.
        if (last != null && kotlin.math.abs(last.value - value) < 0.01f && now - last.timestamp < 1_000L) {
            history[history.lastIndex] = last.copy(timestamp = now)
        } else {
            history += ElectricityHistoryEntry(value = value, timestamp = now)
        }
        val oldestAllowed = now - HISTORY_RETENTION_DAYS * DAY_MILLIS
        val trimmed = history
            .filter { it.timestamp >= oldestAllowed }
            .takeLast(MAX_ELECTRICITY_HISTORY_SIZE)
        mmkv.encode(KEY_ELECTRICITY_HISTORY, gson.toJson(trimmed))
    }

    /**
     * Calculates daily consumption from consecutive real remaining-balance readings.
     * Positive balance jumps are recharges and are intentionally excluded.
     */
    fun getDailyUsage(days: Int, now: Long = System.currentTimeMillis()): List<ElectricityUsageEntry> {
        if (days <= 0) return emptyList()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -(days - 1))
        }
        val starts = LongArray(days + 1)
        for (index in 0..days) {
            starts[index] = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val usage = FloatArray(days)
        val covered = LongArray(days)
        getElectricityHistory()
            .sortedBy { it.timestamp }
            .zipWithNext()
            .forEach { (previous, current) ->
                val elapsed = current.timestamp - previous.timestamp
                val consumed = previous.value - current.value
                if (elapsed <= 0L || consumed < 0f) return@forEach

                for (index in 0 until days) {
                    val overlapStart = maxOf(previous.timestamp, starts[index])
                    val overlapEnd = minOf(current.timestamp, starts[index + 1], now)
                    val overlap = overlapEnd - overlapStart
                    if (overlap > 0L) {
                        usage[index] += consumed * overlap.toFloat() / elapsed.toFloat()
                        covered[index] += overlap
                    }
                }
            }

        return (0 until days).map { index ->
            ElectricityUsageEntry(
                timestamp = starts[index],
                usage = usage[index].takeIf { covered[index] > 0L },
                coveredMillis = covered[index]
            )
        }
    }

    /** Real remaining-balance samples from the latest 24 hours, capped for chart readability. */
    fun getRealtimeElectricityHistory(
        now: Long = System.currentTimeMillis(),
        maxSamples: Int = 30
    ): List<ElectricityHistoryEntry> {
        val start = now - DAY_MILLIS
        return getElectricityHistory()
            .asSequence()
            .filter { it.timestamp in start..now }
            .sortedBy { it.timestamp }
            .toList()
            .takeLast(maxSamples.coerceAtLeast(2))
    }

    fun clearElectricityHistory() {
        mmkv.removeValuesForKeys(
            arrayOf(
                KEY_ELECTRICITY_HISTORY,
                KEY_LAST_ELECTRICITY,
                KEY_PREV_ELECTRICITY,
                KEY_LAST_ELECTRICITY_TIME,
                KEY_PREV_ELECTRICITY_TIME
            )
        )
    }

    data class ElectricitySnapshot(
        val lastValue: Float,
        val lastTime: Long,
        val previousValue: Float?,
        val previousTime: Long?,
        val history: List<ElectricityHistoryEntry> = emptyList()
    )

    data class ElectricityHistoryEntry(
        val value: Float,
        val timestamp: Long
    )

    data class ElectricityUsageEntry(
        val timestamp: Long,
        val usage: Float?,
        val coveredMillis: Long
    )

    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
}
