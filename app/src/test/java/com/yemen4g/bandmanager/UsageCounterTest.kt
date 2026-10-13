package com.yemen4g.bandmanager

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageCounterTest {
    @Test
    fun convertsBytesToDecimalMegabytes() {
        assertEquals(500.0, bytesToMegabytes(500_000_000L), 0.0001)
    }

    @Test
    fun negativeByteCountIsClampedToZero() {
        assertEquals(0.0, bytesToMegabytes(-1L), 0.0)
    }

    @Test
    fun computesCumulativeCounterDelta() {
        assertEquals(200L, counterDelta(1_000L, 1_200L))
    }

    @Test
    fun counterResetDoesNotCreateNegativeUsage() {
        assertEquals(0L, counterDelta(1_200L, 100L))
    }

    @Test
    fun invalidNegativeCountersAreIgnored() {
        assertEquals(0L, counterDelta(-1L, 100L))
        assertEquals(0L, counterDelta(100L, -1L))
    }

    @Test
    fun cumulativeUsageDeltaIsCorrect() {
        assertEquals(12.5, cumulativeCounterDeltaMb(100.0, 112.5), 0.0001)
    }

    @Test
    fun cumulativeCounterResetIsTreatedAsNewBaseline() {
        assertEquals(0.0, cumulativeCounterDeltaMb(500.0, 10.0), 0.0)
    }

    @Test
    fun invalidCumulativeCountersAreIgnored() {
        assertEquals(0.0, cumulativeCounterDeltaMb(-1.0, 10.0), 0.0)
        assertEquals(0.0, cumulativeCounterDeltaMb(10.0, Double.NaN), 0.0)
    }

    @Test
    fun formatsMegabytesUsingStableDecimalPoint() {
        assertEquals("1.50", formatMegabytes(1_500_000L))
    }
}
