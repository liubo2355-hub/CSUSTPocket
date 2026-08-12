package com.csust.pocket.core.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanetRequestPolicyTest {

    @Test
    fun cacheIsFreshOnlyInsideConfiguredWindow() {
        val now = 100_000L
        assertTrue(PlanetRequestPolicy.isFresh(90_001L, now, 10_000L))
        assertFalse(PlanetRequestPolicy.isFresh(90_000L, now, 10_000L))
        assertFalse(PlanetRequestPolicy.isFresh(0L, now, 10_000L))
        assertFalse(PlanetRequestPolicy.isFresh(now + 1L, now, 10_000L))
    }

    @Test
    fun retryIsBlockedOnlyInsideCooldownWindow() {
        val now = 100_000L
        assertTrue(PlanetRequestPolicy.isCoolingDown(now - 1L, now))
        assertTrue(
            PlanetRequestPolicy.isCoolingDown(
                now - PlanetRequestPolicy.REQUEST_COOLDOWN_MS + 1L,
                now
            )
        )
        assertFalse(
            PlanetRequestPolicy.isCoolingDown(
                now - PlanetRequestPolicy.REQUEST_COOLDOWN_MS,
                now
            )
        )
        assertFalse(PlanetRequestPolicy.isCoolingDown(0L, now))
    }
}
