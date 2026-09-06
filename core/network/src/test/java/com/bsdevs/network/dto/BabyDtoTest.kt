package com.bsdevs.network.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class BabyDtoTest {

    @Test
    fun `serialization includes new feeding prediction fields`() {
        val baby = BabyDto(
            id = "baby123",
            firstName = "Test",
            activeModel = "balanced",
            predictionsByModel = mapOf("balanced" to "2026-09-06T18:00:00Z"),
            modelPerformance = mapOf(
                "balanced" to ModelPerformance(totalError = 10.5, count = 5)
            ),
            lastPredictionSync = "2026-09-06T16:00:00Z"
        )

        val json = Json.encodeToString(baby)
        val deserialized = Json.decodeFromString<BabyDto>(json)

        assertEquals(baby.activeModel, deserialized.effectiveActiveModel)
        assertEquals(baby.predictionsByModel, deserialized.effectivePredictionsByModel)
        assertEquals(baby.modelPerformance?.get("balanced")?.totalError, deserialized.effectiveModelPerformance?.get("balanced")?.totalError)
        assertEquals(baby.modelPerformance?.get("balanced")?.count, deserialized.effectiveModelPerformance?.get("balanced")?.count)
        assertEquals(baby.lastPredictionSync, deserialized.effectiveLastPredictionSync)
    }

    @Test
    fun `deserialization handles missing optional fields`() {
        val json = """{"id":"baby123","firstName":"Test"}"""
        val deserialized = Json.decodeFromString<BabyDto>(json)

        assertEquals("baby123", deserialized.id)
        assertEquals("Test", deserialized.firstName)
        assertEquals(null, deserialized.effectiveActiveModel)
        assertEquals(null, deserialized.effectivePredictionsByModel)
        assertEquals(null, deserialized.effectiveModelPerformance)
    }

    @Test
    fun `effective properties prioritize camelCase over snake_case`() {
        val baby = BabyDto(
            nextFeedingTime = "12:00"
        ).apply {
            next_feeding_time = "13:00"
        }
        assertEquals("12:00", baby.effectiveNextFeedingTime)
    }

    @Test
    fun `effective properties fall back to snake_case when camelCase is null`() {
        val baby = BabyDto().apply {
            next_feeding_time = "13:00"
        }
        assertEquals("13:00", baby.effectiveNextFeedingTime)
    }
}
