package com.bsdevs.babycare.domain

import com.bsdevs.babycare.network.UnifiedEventDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
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
        assertEquals("2026-09-06 11:00", prediction?.predictedTime?.format(formatter))
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
        // Age Adjustment (no birthdate): 0
        // Growth Spurt (no measurements): 0
        // Prediction: 07:30 + (160 + 30) = 07:30 + 190 mins = 10:40
        
        assertEquals("2026-09-06 10:40", prediction?.predictedTime?.format(formatter))
    }

    @Test
    fun `predictNextFeeding handles noisy growing trend`() {
        val events = listOf(
            createFeeding("2026-09-06 00:00"),
            createFeeding("2026-09-06 02:00"), // 120 min gap
            createFeeding("2026-09-06 04:30"), // 150 min gap
            createFeeding("2026-09-06 06:50"), // 140 min gap (noise!)
            createFeeding("2026-09-06 09:40")  // 170 min gap
        )
        val prediction = engine.predictNextFeeding(events)
        assertNotNull(prediction)
        
        // Gaps: 120, 150, 140, 170
        // Differences: +30, -10, +30
        // Avg Velocity (Trend): (30 - 10 + 30) / 3 = 16.66 mins
        // Weighted Avg Gap: (120*1 + 150*2 + 140*3 + 170*4) / 10 = (120 + 300 + 420 + 680) / 10 = 1520 / 10 = 152 mins
        // Prediction: 09:40 + (152 + 16.66) = 09:40 + 168.66 mins = 12:28 (approx)
        
        assertEquals("2026-09-06 12:28", prediction?.predictedTime?.format(formatter))
    }

    @Test
    fun `predictNextFeeding factors in age adjustment`() {
        val events = listOf(
            createFeeding("2026-09-06 02:00"),
            createFeeding("2026-09-06 05:00"),
            createFeeding("2026-09-06 08:00")
        )
        // Set birthdate to 4 months ago
        val birthDate = java.time.LocalDate.now().minusMonths(4).toString()
        val context = BabyContext(birthDate = birthDate)
        
        val prediction = engine.predictNextFeeding(events, context)
        assertNotNull(prediction)
        
        // Base gap: 180 mins
        // Age adjustment: 4 months * 5 mins = 20 mins
        // Prediction: 08:00 + 180 + 20 = 11:20
        assertEquals("2026-09-06 11:20", prediction?.predictedTime?.format(formatter))
    }

    @Test
    fun `predictNextFeeding factors in growth spurt adjustment`() {
        val events = listOf(
            createFeeding("2026-09-06 02:00"),
            createFeeding("2026-09-06 05:00"),
            createFeeding("2026-09-06 08:00")
        )
        // Add weight measurements showing 50g/day gain
        val measurements = listOf(
            UnifiedEventDto(type = "MEASUREMENT", weight = 4.0, dateTimeString = "2026-09-01 08:00"),
            UnifiedEventDto(type = "MEASUREMENT", weight = 4.25, dateTimeString = "2026-09-06 08:00")
        )
        val context = BabyContext(measurements = measurements)
        
        val prediction = engine.predictNextFeeding(events, context)
        assertNotNull(prediction)
        
        // Base gap: 180 mins
        // Weight gain: 250g over 5 days = 50g/day
        // Growth spurt adjustment: -20 mins
        // Prediction: 08:00 + 180 - 20 = 10:40
        assertEquals("2026-09-06 10:40", prediction?.predictedTime?.format(formatter))
    }

    @Test
    fun `predictNextFeeding factors in WHO centile adjustment`() {
        val events = listOf(
            createFeeding("2026-09-06 02:00"),
            createFeeding("2026-09-06 05:00"),
            createFeeding("2026-09-06 08:00")
        )
        // Set baby to be at 98th percentile for age
        val birthDate = java.time.LocalDate.now().toString() // 0 months old
        val measurements = listOf(
            UnifiedEventDto(type = "MEASUREMENT", weight = 4.6, dateTimeString = "2026-09-06 08:00")
        )
        val context = BabyContext(birthDate = birthDate, measurements = measurements, gender = "male")
        
        val prediction = engine.predictNextFeeding(events, context)
        assertNotNull(prediction)
        
        // Base gap: 180 mins
        // WHO Adjustment (98th centile > p91): -15 mins
        // Prediction: 08:00 + 180 - 15 = 10:45
        assertEquals("2026-09-06 10:45", prediction?.predictedTime?.format(formatter))
    }

    @Test
    fun `predictNextFeeding factors in metabolic nappy adjustment`() {
        val events = listOf(
            createFeeding("2026-09-06 02:00"),
            createFeeding("2026-09-06 05:00"),
            createFeeding("2026-09-06 08:00")
        )
        // Add high nappy output in last 24h
        val now = java.time.LocalDateTime.now()
        val nappyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val nappyEvents = listOf(
            UnifiedEventDto(type = "NAPPY", dateTimeString = now.minusHours(2).format(nappyFormatter)),
            UnifiedEventDto(type = "NAPPY", dateTimeString = now.minusHours(4).format(nappyFormatter)),
            UnifiedEventDto(type = "NAPPY", dateTimeString = now.minusHours(6).format(nappyFormatter)),
            UnifiedEventDto(type = "NAPPY", dateTimeString = now.minusHours(8).format(nappyFormatter)),
            UnifiedEventDto(type = "NAPPY", dateTimeString = now.minusHours(10).format(nappyFormatter))
        )
        val context = BabyContext(nappyEvents = nappyEvents)
        
        val prediction = engine.predictNextFeeding(events, context)
        assertNotNull(prediction)
        
        // Base gap: 180 mins
        // Metabolic Adjustment (high output): -10 mins
        // Prediction: 08:00 + 180 - 10 = 10:50
        assertEquals("2026-09-06 10:50", prediction?.predictedTime?.format(formatter))
    }

    @Test
    fun `predictNextFeeding provides confidence range for high variance`() {
        val events = listOf(
            createFeeding("2026-09-06 00:00"),
            createFeeding("2026-09-06 02:00"), // 120 min gap
            createFeeding("2026-09-06 05:00"), // 180 min gap
            createFeeding("2026-09-06 07:00")  // 120 min gap
        )
        val prediction = engine.predictNextFeeding(events)
        assertNotNull(prediction)
        
        // Gaps: 120, 180, 120. Mean: 140. 
        // SD: sqrt(((120-140)^2 + (180-140)^2 + (120-140)^2)/3) = sqrt((400 + 1600 + 400)/3) = sqrt(800) = 28.28
        // Range: 28.28 * 1.5 = 42.42 -> 42
        assertEquals(42, prediction?.confidenceRangeMinutes)
    }

    private fun createFeeding(dateTime: String) = UnifiedEventDto(
        id = "id",
        type = "FEEDING",
        dateTimeString = dateTime
    )
}
