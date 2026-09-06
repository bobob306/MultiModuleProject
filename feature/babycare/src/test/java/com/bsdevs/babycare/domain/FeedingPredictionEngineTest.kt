package com.bsdevs.babycare.domain

import com.bsdevs.babycare.network.UnifiedEventDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FeedingPredictionEngineTest {

    private val engine = FeedingPredictionEngine()
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    @Test
    fun `predictNextFeeding returns null for empty list`() {
        val prediction = engine.predictNextFeeding(emptyList())
        assertNull(prediction)
    }

    @Test
    fun `predictNextFeeding returns null for single event`() {
        val events = listOf(createFeeding("2026-09-06 08:00"))
        val prediction = engine.predictNextFeeding(events)
        assertNull(prediction)
    }

    @Test
    fun `predictNextFeeding handles consistent 3 hour gaps`() {
        val events = listOf(
            createFeeding("2026-09-06 02:00"),
            createFeeding("2026-09-06 05:00"),
            createFeeding("2026-09-06 08:00")
        )
        val prediction = engine.predictNextFeeding(events)
        assertNotNull(prediction)
        assertEquals("2026-09-06 11:00", prediction?.format(formatter))
    }

    @Test
    fun `predictNextFeeding factors in recency bias (growing gaps)`() {
        val events = listOf(
            createFeeding("2026-09-06 00:00"),
            createFeeding("2026-09-06 02:00"), // 120 min gap
            createFeeding("2026-09-06 04:30"), // 150 min gap
            createFeeding("2026-09-06 07:30")  // 180 min gap
        )
        val prediction = engine.predictNextFeeding(events)
        assertNotNull(prediction)
        
        // Gaps: 120, 150, 180. 
        // Trend: Increasing by 30 mins.
        // Weighted Avg: (120*1 + 150*2 + 180*3) / 6 = (120 + 300 + 540) / 6 = 960 / 6 = 160
        // Trend Adjustment: (180 - 120) / 2 = 30
        // Prediction: 180 (last) + 160 (avg) + 30 (trend) ? No, logic is last + (avg + trend)
        // Wait, my logic is: last + (weightedAverageGapMinutes + trendAdjustment + timeOfDayAdjustment)
        // 07:30 + (160 + 30) = 07:30 + 190 mins = 10:40
        
        assertEquals("2026-09-06 10:40", prediction?.format(formatter))
    }

    private fun createFeeding(dateTime: String) = UnifiedEventDto(
        id = "id",
        type = "FEEDING",
        dateTimeString = dateTime
    )
}
