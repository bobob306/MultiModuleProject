package com.bsdevs.forms.impl

import com.bsdevs.babycare.core.domain.BabyCareRepository
import com.bsdevs.network.dto.BabyEvent
import com.bsdevs.coffeescreen.data.CoffeeRepository
import com.bsdevs.network.dto.CoffeeDto
import com.bsdevs.common.result.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.TimeZone

class FormSubmitterImplTest {

    private val coffeeRepository = mockk<CoffeeRepository>(relaxed = true)
    private val babyCareRepository = mockk<BabyCareRepository>(relaxed = true)
    private lateinit var router: FormSubmitterImpl

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        router = FormSubmitterImpl(coffeeRepository, babyCareRepository)
    }

    // --- coffeeLog ---

    @Test
    fun `coffeeLog routes to CoffeeRepository uploadCoffee`() = runTest {
        val values = mapOf(
            "bean_types" to listOf("Arabica"),
            "origin_countries" to listOf("Ethiopia"),
            "tasting_notes" to listOf("Berry"),
            "preparation_method" to listOf("Washed"),
            "roaster" to "Wogan",
            "is_decaf" to "Caffeinated",
            "roast_date" to "2026-08-31",
        )
        val result = router.submit("user1", "coffeeLog", null, values)
        assertTrue(result is Result.Success)
        coVerify { coffeeRepository.uploadCoffee("user1", any()) }
    }

    @Test
    fun `coffee label is computed from roaster + origins + method + date`() = runTest {
        val coffeeSlot = slot<CoffeeDto>()
        coEvery { coffeeRepository.uploadCoffee(any(), capture(coffeeSlot)) } returns Unit

        router.submit("u", "coffeeLog", null, mapOf(
            "roaster" to "Wogan",
            "origin_countries" to listOf("Ethiopia", "Colombia"),
            "preparation_method" to listOf("Washed"),
            "roast_date" to "2026-08-31",
            "is_decaf" to "Caffeinated",
        ))

        assertTrue(coffeeSlot.captured.label!!.contains("Wogan"))
        assertTrue(coffeeSlot.captured.label!!.contains("Ethiopia"))
        assertTrue(coffeeSlot.captured.label!!.contains("Washed"))
        assertTrue(coffeeSlot.captured.label!!.contains("2026-08-31"))
    }

    @Test
    fun `coffee edit uses entityId as document ID`() = runTest {
        val coffeeSlot = slot<CoffeeDto>()
        coEvery { coffeeRepository.uploadCoffee(any(), capture(coffeeSlot)) } returns Unit
        router.submit("u", "coffeeLog", "existing-id", mapOf("roast_date" to "", "is_decaf" to "Caffeinated"))
        assertEquals("existing-id", coffeeSlot.captured.id)
    }

    @Test
    fun `is_decaf Decaffeinated maps to isDecaf true`() = runTest {
        val slot = slot<CoffeeDto>()
        coEvery { coffeeRepository.uploadCoffee(any(), capture(slot)) } returns Unit
        router.submit("u", "coffeeLog", null, mapOf("is_decaf" to "Decaffeinated", "roast_date" to ""))
        assertTrue(slot.captured.isDecaf == true)
    }

    @Test
    fun `is_decaf Caffeinated maps to isDecaf false`() = runTest {
        val slot = slot<CoffeeDto>()
        coEvery { coffeeRepository.uploadCoffee(any(), capture(slot)) } returns Unit
        router.submit("u", "coffeeLog", null, mapOf("is_decaf" to "Caffeinated", "roast_date" to ""))
        assertFalse(slot.captured.isDecaf == true)
    }

    // --- nappyLog ---

    @Test
    fun `nappyLog routes to BabyCareRepository with NAPPY event type`() = runTest {
        val eventSlot = slot<BabyEvent>()
        coEvery { babyCareRepository.saveActivityEvent(any(), any(), capture(eventSlot)) } returns Unit

        val result = router.submit("user1", "nappyLog", null, mapOf(
            "date" to "2026-08-31",
            "time" to "10:30",
            "nappy_type" to "Wet",
            "comment" to "All good",
        ))

        assertTrue(result is Result.Success)
        val event = eventSlot.captured as BabyEvent.Nappy
        assertEquals("NAPPY", event.type)
        assertEquals("10:30", event.time)
        assertTrue(event.dateTimeString.contains("2026-08-31T10:30"))
        assertTrue(event.dateTimeString.endsWith("Z"))
        assertEquals("Wet", event.nappyType)
        assertEquals("All good", event.comment)
    }

    @Test
    fun `nappyLog edit calls updateActivityEvent not save`() = runTest {
        coEvery { babyCareRepository.updateActivityEvent(any(), any(), any(), any()) } returns Unit

        router.submit("u", "nappyLog", "existing-id", mapOf("date" to "2026-08-31", "time" to "10:00"))

        coVerify { babyCareRepository.updateActivityEvent("u", "2026-08-31", "existing-id", any()) }
        coVerify(exactly = 0) { babyCareRepository.saveActivityEvent(any(), any(), any()) }
    }

    @Test
    fun `nappyLog returns Error when date field is missing`() = runTest {
        assertTrue(router.submit("u", "nappyLog", null, mapOf("time" to "10:00")) is Result.Error)
    }

    // --- feedingLog ---

    @Test
    fun `feedingLog routes to BabyCareRepository with FEEDING event type`() = runTest {
        val eventSlot = slot<BabyEvent>()
        coEvery { babyCareRepository.saveActivityEvent(any(), any(), capture(eventSlot)) } returns Unit

        val result = router.submit("user1", "feedingLog", null, mapOf(
            "date" to "2026-08-31",
            "start_time" to "08:00",
            "feeding_side" to "Left",
            "bottle_amount_ml" to "120",
        ))

        assertTrue(result is Result.Success)
        val event = eventSlot.captured as BabyEvent.Feeding
        assertEquals("FEEDING", event.type)
        assertEquals("08:00", event.time)
        assertTrue(event.dateTimeString.contains("2026-08-31T08:00"))
        assertTrue(event.dateTimeString.endsWith("Z"))
        assertEquals("Left", event.mainFeedingSide)
        assertEquals(120, event.bottleAmountMl)
    }

    @Test
    fun `feedingLog returns Error when date field is missing`() = runTest {
        assertTrue(router.submit("u", "feedingLog", null, mapOf("start_time" to "10:00")) is Result.Error)
    }

    @Test
    fun `unknown target returns Error`() = runTest {
        assertTrue(router.submit("u", "unknownTarget", null, emptyMap()) is Result.Error)
    }

    // --- temperatureLog ---

    @Test
    fun `temperatureLog routes to BabyCareRepository with TEMPERATURE event type`() = runTest {
        val eventSlot = slot<BabyEvent>()
        coEvery { babyCareRepository.saveActivityEvent(any(), any(), capture(eventSlot)) } returns Unit

        val result = router.submit("u", "temperatureLog", null, mapOf(
            "date" to "2026-08-31",
            "time" to "09:00",
            "temperature_value" to 370,
        ))

        assertTrue(result is Result.Success)
        val event = eventSlot.captured as BabyEvent.Temperature
        assertEquals("TEMPERATURE", event.type)
        assertEquals(37.0, event.temperature!!, 0.001)
        assertEquals("09:00", event.time)
    }

    @Test
    fun `temperatureLog edit calls updateActivityEvent`() = runTest {
        coEvery { babyCareRepository.updateActivityEvent(any(), any(), any(), any()) } returns Unit
        router.submit("u", "temperatureLog", "t1", mapOf("date" to "2026-08-31", "temperature_value" to 370))
        coVerify { babyCareRepository.updateActivityEvent("u", "2026-08-31", "t1", any()) }
        coVerify(exactly = 0) { babyCareRepository.saveActivityEvent(any(), any(), any()) }
    }

    @Test
    fun `temperatureLog returns Error when date missing`() = runTest {
        assertTrue(router.submit("u", "temperatureLog", null, mapOf("temperature_value" to 370)) is Result.Error)
    }

    // --- measurementLog ---

    @Test
    fun `measurementLog saves height and weight and head circumference converted from wheel ints`() = runTest {
        val eventSlot = slot<BabyEvent>()
        coEvery { babyCareRepository.saveActivityEvent(any(), any(), capture(eventSlot)) } returns Unit

        router.submit("u", "measurementLog", null, mapOf(
            "date" to "2026-08-31",
            "time" to "10:00",
            "record_height" to true,
            "height_value" to 650,
            "record_weight" to true,
            "weight_value" to 750,
            "record_head_circumference" to true,
            "head_circumference_value" to 425,
            "is_medical" to true,
        ))

        val event = eventSlot.captured as BabyEvent.Measurement
        assertEquals("MEASUREMENT", event.type)
        assertEquals(65.0, event.height!!, 0.001)
        assertEquals(7.5, event.weight!!, 0.001)
        assertEquals(42.5, event.headCircumference!!, 0.001)
        assertTrue(event.isMedical == true)
    }

    @Test
    fun `measurementLog returns Error when neither height nor weight provided`() = runTest {
        assertTrue(router.submit("u", "measurementLog", null, mapOf("date" to "2026-08-31")) is Result.Error)
    }

    // --- vaccinationLog ---

    @Test
    fun `vaccinationLog routes to BabyCareRepository with VACCINATION event type`() = runTest {
        val eventSlot = slot<BabyEvent>()
        coEvery { babyCareRepository.saveActivityEvent(any(), any(), capture(eventSlot)) } returns Unit

        val result = router.submit("user1", "vaccinationLog", null, mapOf(
            "date" to "2026-08-31",
            "time" to "10:30",
            "vaccination_names" to listOf("HepB"),
            "location" to "Clinic A",
            "series_id" to "hep_b_series"
        ))

        assertTrue(result is Result.Success)
        val event = eventSlot.captured as BabyEvent.Vaccination
        assertEquals("VACCINATION", event.type)
        assertEquals("Clinic A", event.location)
        assertEquals("hep_b_series", event.seriesId)
        assertEquals(listOf("HepB"), event.vaccinationNames)
    }

    @Test
    fun `vaccinationLog returns Error when date field is missing`() = runTest {
        assertTrue(router.submit("u", "vaccinationLog", null, mapOf("vaccination_names" to listOf("Polio"))) is Result.Error)
    }
}
