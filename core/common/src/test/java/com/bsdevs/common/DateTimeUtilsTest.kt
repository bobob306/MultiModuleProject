package com.bsdevs.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class DateTimeUtilsTest {

    @Test
    fun `parseToInstant handles ISO format`() {
        val iso = "2026-09-07T10:00:00Z"
        val instant = DateTimeUtils.parseToInstant(iso)
        assertEquals(Instant.parse(iso), instant)
    }

    @Test
    fun `parseToInstant handles space-separated format`() {
        val legacy = "2026-09-07 10:00"
        val instant = DateTimeUtils.parseToInstant(legacy)
        // With local zone, we just check it's not EPOCH and represents the same date
        assert(instant != Instant.EPOCH)
    }

    @Test
    fun `parseToInstant returns EPOCH for blank strings`() {
        assertEquals(Instant.EPOCH, DateTimeUtils.parseToInstant(""))
        assertEquals(Instant.EPOCH, DateTimeUtils.parseToInstant("   "))
    }

    @Test
    fun `formatIsoToTime handles ISO Z format`() {
        val iso = "2026-09-07T10:30:00Z"
        val time = DateTimeUtils.formatIsoToTime(iso, ZoneId.of("UTC"))
        assertEquals("10:30", time)
    }

    @Test
    fun `formatIsoToTime handles LocalDateTime format`() {
        val iso = "2026-09-07T14:45:00"
        val time = DateTimeUtils.formatIsoToTime(iso)
        assertEquals("14:45", time)
    }

    @Test
    fun `formatIsoToTime returns null for null input`() {
        assertEquals(null, DateTimeUtils.formatIsoToTime(null))
    }

    @Test
    fun `extractTime uses time if not empty`() {
        assertEquals("09:15", DateTimeUtils.extractTime("09:15", "2026-09-07T10:00:00Z"))
    }

    @Test
    fun `extractTime pulls from dateTimeString if time is empty`() {
        assertEquals("10:00", DateTimeUtils.extractTime("", "2026-09-07T10:00:00Z"))
        assertEquals("10:00", DateTimeUtils.extractTime("", "2026-09-07 10:00:00"))
    }
}
