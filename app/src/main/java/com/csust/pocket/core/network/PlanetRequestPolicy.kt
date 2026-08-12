package com.csust.pocket.core.network

internal object PlanetRequestPolicy {
    const val CAMPUS_MAP_CACHE_MAX_AGE_MS = 24L * 60L * 60L * 1000L
    const val REQUEST_COOLDOWN_MS = 30_000L

    fun isFresh(updatedAt: Long, now: Long, maxAgeMs: Long): Boolean =
        updatedAt > 0L && now >= updatedAt && now - updatedAt < maxAgeMs

    fun isCoolingDown(lastAttemptAt: Long, now: Long): Boolean =
        lastAttemptAt > 0L && now >= lastAttemptAt && now - lastAttemptAt < REQUEST_COOLDOWN_MS
}
