package com.bsdevs.forms.impl

import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.network.UnifiedEventDto
import com.bsdevs.common.result.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FormDeleterImplTest {

    private val babyCareRepository = mockk<BabyCareRepository>(relaxed = true)
    private lateinit var deleter: FormDeleterImpl

    @Before
    fun setUp() {
        deleter = FormDeleterImpl(babyCareRepository)
    }

    @Test
    fun `nappyLog deletes with date extracted from dateTimeString`() = runTest {
        coEvery { babyCareRepository.getNappyEventById("u", "n1") } returns UnifiedEventDto(
            id = "n1", type = "NAPPY", time = "10:30",
            dateTimeString = "2026-08-31 10:30",
        )

        val result = deleter.delete("u", "nappyLog", "n1")

        assertTrue(result is Result.Success)
        coVerify { babyCareRepository.deleteActivityEvent("u", "2026-08-31", "n1") }
    }

    @Test
    fun `feedingLog deletes with date extracted from dateTimeString`() = runTest {
        coEvery { babyCareRepository.getFeedingEventById("u", "f1") } returns UnifiedEventDto(
            id = "f1", type = "FEEDING", time = "08:00",
            dateTimeString = "2026-09-01 08:00",
        )

        val result = deleter.delete("u", "feedingLog", "f1")

        assertTrue(result is Result.Success)
        coVerify { babyCareRepository.deleteActivityEvent("u", "2026-09-01", "f1") }
    }

    @Test
    fun `nappyLog returns Error when entity not found`() = runTest {
        coEvery { babyCareRepository.getNappyEventById("u", "missing") } returns null
        val result = deleter.delete("u", "nappyLog", "missing")
        assertTrue(result is Result.Error)
    }

    @Test
    fun `unknown target returns Error`() = runTest {
        val result = deleter.delete("u", "coffeeLog", "id")
        assertTrue(result is Result.Error)
    }

    // --- temperatureLog ---

    @Test
    fun `temperatureLog extracts date from space-separated dateTimeString`() = runTest {
        coEvery { babyCareRepository.getFeedingEventById("u", "t1") } returns UnifiedEventDto(
            id = "t1", type = "TEMPERATURE", time = "09:30",
            dateTimeString = "2026-09-01 09:30",
        )

        val result = deleter.delete("u", "temperatureLog", "t1")

        assertTrue(result is Result.Success)
        coVerify { babyCareRepository.deleteActivityEvent("u", "2026-09-01", "t1") }
    }

    @Test
    fun `temperatureLog returns Error when entity not found`() = runTest {
        coEvery { babyCareRepository.getFeedingEventById("u", "missing") } returns null
        assertTrue(deleter.delete("u", "temperatureLog", "missing") is Result.Error)
    }

    // --- measurementLog ---

    @Test
    fun `measurementLog extracts date from space-separated dateTimeString`() = runTest {
        coEvery { babyCareRepository.getMeasurementEventById("u", "m1") } returns UnifiedEventDto(
            id = "m1", type = "MEASUREMENT", time = "10:00",
            dateTimeString = "2026-09-02 10:00",
        )

        val result = deleter.delete("u", "measurementLog", "m1")

        assertTrue(result is Result.Success)
        coVerify { babyCareRepository.deleteActivityEvent("u", "2026-09-02", "m1") }
    }

    @Test
    fun `measurementLog returns Error when entity not found`() = runTest {
        coEvery { babyCareRepository.getMeasurementEventById("u", "missing") } returns null
        assertTrue(deleter.delete("u", "measurementLog", "missing") is Result.Error)
    }

    // --- vaccinationLog ---

    @Test
    fun `vaccinationLog extracts date from space-separated dateTimeString`() = runTest {
        coEvery { babyCareRepository.getVaccinationEventById("u", "v1") } returns UnifiedEventDto(
            id = "v1", type = "VACCINATION", time = "10:30",
            dateTimeString = "2026-09-03 10:30",
        )

        val result = deleter.delete("u", "vaccinationLog", "v1")

        assertTrue(result is Result.Success)
        coVerify { babyCareRepository.deleteActivityEvent("u", "2026-09-03", "v1") }
    }
}
