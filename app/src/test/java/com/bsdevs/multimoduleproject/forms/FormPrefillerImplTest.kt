package com.bsdevs.multimoduleproject.forms

import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.network.UnifiedEventDto
import com.bsdevs.coffeescreen.data.CoffeeRepository
import com.bsdevs.coffeescreen.network.CoffeeDto
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FormPrefillerImplTest {

    private val coffeeRepository = mockk<CoffeeRepository>()
    private val babyCareRepository = mockk<BabyCareRepository>()
    private lateinit var prefiller: FormPrefillerImpl

    @Before
    fun setUp() {
        every { coffeeRepository.allCoffee } returns MutableStateFlow(emptyList<CoffeeDto>()).asStateFlow()
        prefiller = FormPrefillerImpl(coffeeRepository, babyCareRepository)
    }

    @Test
    fun `coffeeLog maps all fields from CoffeeDto`() = runTest {
        coEvery { coffeeRepository.getCoffeeById("u", "id1") } returns CoffeeDto(
            beanTypes = listOf("Arabica"),
            originCountries = listOf("Ethiopia"),
            tastingNotes = listOf("Berry"),
            beanPreparationMethod = listOf("Washed"),
            roaster = "Wogan",
            roastDate = "2026-08-31",
            isDecaf = false,
        )
        val result = prefiller.loadExistingValues("u", "coffeeLog", "id1")!!
        assertEquals(listOf("Arabica"), result["bean_types"])
        assertEquals(listOf("Ethiopia"), result["origin_countries"])
        assertEquals(listOf("Berry"), result["tasting_notes"])
        assertEquals("Wogan", result["roaster"])
        assertEquals("2026-08-31", result["roast_date"])
        assertEquals("Caffeinated", result["is_decaf"])
    }

    @Test
    fun `coffeeLog maps isDecaf true to Decaffeinated`() = runTest {
        coEvery { coffeeRepository.getCoffeeById("u", "id1") } returns CoffeeDto(isDecaf = true)
        val result = prefiller.loadExistingValues("u", "coffeeLog", "id1")!!
        assertEquals("Decaffeinated", result["is_decaf"])
    }

    @Test
    fun `coffeeLog returns null when entity not found`() = runTest {
        coEvery { coffeeRepository.getCoffeeById("u", "missing") } returns null
        assertNull(prefiller.loadExistingValues("u", "coffeeLog", "missing"))
    }

    @Test
    fun `nappyLog maps time, nappyType, comment and date`() = runTest {
        coEvery { babyCareRepository.getNappyEventById("u", "n1") } returns UnifiedEventDto(
            type = "NAPPY",
            time = "10:30",
            dateTimeString = "2026-08-31T10:30",
            nappyType = "Wet",
            comment = "Normal",
        )
        val result = prefiller.loadExistingValues("u", "nappyLog", "n1")!!
        assertEquals("10:30", result["time"])
        assertEquals("Wet", result["nappy_type"])
        assertEquals("Normal", result["comment"])
        assertEquals("2026-08-31", result["date"])
    }

    @Test
    fun `nappyLog returns null when entity not found`() = runTest {
        coEvery { babyCareRepository.getNappyEventById("u", "missing") } returns null
        assertNull(prefiller.loadExistingValues("u", "nappyLog", "missing"))
    }

    @Test
    fun `unknown target returns null`() = runTest {
        assertNull(prefiller.loadExistingValues("u", "unknownTarget", "id"))
    }

    // --- temperatureLog ---

    @Test
    fun `temperatureLog maps time, temperature and date from space-separated dateTimeString`() = runTest {
        coEvery { babyCareRepository.getFeedingEventById("u", "t1") } returns UnifiedEventDto(
            type = "TEMPERATURE",
            time = "09:30",
            dateTimeString = "2026-08-31 09:30",
            temperature = 37.2,
            comment = "Feeling warm",
        )
        val result = prefiller.loadExistingValues("u", "temperatureLog", "t1")!!
        assertEquals("2026-08-31", result["date"])
        assertEquals("09:30", result["time"])
        assertEquals(372, result["temperature_value"])
        assertEquals("Feeling warm", result["comment"])
    }

    @Test
    fun `temperatureLog returns null when event type is not TEMPERATURE`() = runTest {
        coEvery { babyCareRepository.getFeedingEventById("u", "t1") } returns UnifiedEventDto(type = "NAPPY")
        assertNull(prefiller.loadExistingValues("u", "temperatureLog", "t1"))
    }

    @Test
    fun `temperatureLog returns null when entity not found`() = runTest {
        coEvery { babyCareRepository.getFeedingEventById("u", "missing") } returns null
        assertNull(prefiller.loadExistingValues("u", "temperatureLog", "missing"))
    }

    // --- measurementLog ---

    @Test
    fun `measurementLog maps height and weight from wheel int conversion`() = runTest {
        coEvery { babyCareRepository.getMeasurementEventById("u", "m1") } returns UnifiedEventDto(
            type = "MEASUREMENT",
            time = "10:00",
            dateTimeString = "2026-08-31 10:00",
            height = 65.0,
            weight = 7.5,
            isMedical = true,
            comment = "GP visit",
        )
        val result = prefiller.loadExistingValues("u", "measurementLog", "m1")!!
        assertEquals("2026-08-31", result["date"])
        assertEquals(true, result["record_height"])
        assertEquals(650, result["height_value"])
        assertEquals(true, result["record_weight"])
        assertEquals(750, result["weight_value"])
        assertEquals(true, result["is_medical"])
        assertEquals("GP visit", result["comment"])
    }

    @Test
    fun `measurementLog omits height fields when height is null`() = runTest {
        coEvery { babyCareRepository.getMeasurementEventById("u", "m1") } returns UnifiedEventDto(
            type = "MEASUREMENT", time = "10:00", dateTimeString = "2026-08-31 10:00",
            height = null, weight = 8.0,
        )
        val result = prefiller.loadExistingValues("u", "measurementLog", "m1")!!
        assertNull(result["record_height"])
        assertNull(result["height_value"])
        assertEquals(true, result["record_weight"])
    }

    @Test
    fun `measurementLog returns null when entity not found`() = runTest {
        coEvery { babyCareRepository.getMeasurementEventById("u", "missing") } returns null
        assertNull(prefiller.loadExistingValues("u", "measurementLog", "missing"))
    }
}
