package com.bsdevs.multimoduleproject.functions

import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.core.domain.BabyCareRepository
import com.bsdevs.network.dto.BabyEvent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Result of recording a nappy change.
 */
@AppFunctionSerializable
data class NappyChangeResult(
    /** Whether the recording was successful. */
    val success: Boolean,
    /** A descriptive message about the outcome. */
    val message: String
)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppFunctionsEntryPoint {
    fun accountService(): AccountService
    fun babyCareRepository(): BabyCareRepository
}

/**
 * AppFunctions for recording baby activities.
 */
@RequiresApi(36)
@AppFunctionServiceEntryPoint(
    serviceName = "MMPAppFunctionService",
    appFunctionXmlFileName = "mmp_app_functions"
)
abstract class MMPAppFunctions : AppFunctionService() {

    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(applicationContext, AppFunctionsEntryPoint::class.java)
    }

    private val accountService get() = entryPoint.accountService()
    private val repository get() = entryPoint.babyCareRepository()

    /**
     * Records a new nappy change or diaper change for the baby.
     * Use this when the user says: "record a dirty diaper", "log a wet nappy", etc.
     *
     * @param type The state of the nappy. Should be "wet", "dirty", or "both".
     * @return A result indicating if the change was successfully logged.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun recordNappyChange(type: String): NappyChangeResult {
        Log.d("MMPAppFunctions", "recordNappyChange called with type: $type")
        val userId = accountService.currentUserId
        if (userId.isEmpty()) {
            return NappyChangeResult(false, "Please sign in to the MMP app to record activities.")
        }

        val date = LocalDate.now()
        val time = LocalTime.now()
        val utcDateTimeString = LocalDateTime.of(date, time)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toString()

        val normalizedType = type.lowercase()
        val internalNappyType = when {
            normalizedType.contains("both") || (normalizedType.contains("wet") && normalizedType.contains("dirty")) -> "Both"
            normalizedType.contains("dirty") || normalizedType.contains("soiled") || normalizedType.contains("poo") -> "Dirty"
            else -> "Wet"
        }

        val event = BabyEvent.Nappy(
            id = UUID.randomUUID().toString(),
            time = time.format(DateTimeFormatter.ofPattern("HH:mm")),
            dateTimeString = utcDateTimeString,
            nappyType = internalNappyType,
            isPendingSync = true
        )

        return try {
            repository.saveActivityEvent(userId, date.toString(), event)
            NappyChangeResult(true, "Logged a $internalNappyType nappy change at ${event.time}.")
        } catch (e: Exception) {
            NappyChangeResult(false, "Failed to log nappy change: ${e.message}")
        }
    }
}
