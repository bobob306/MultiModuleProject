package com.bsdevs.multimoduleproject.functions

import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.network.UnifiedEventDto
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.LocalTime
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
abstract class BaseMMPAppFunctionService : AppFunctionService() {

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

        val date = LocalDate.now().toString()
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

        val normalizedType = type.lowercase()
        val internalNappyType = when {
            normalizedType.contains("both") || (normalizedType.contains("wet") && normalizedType.contains("dirty")) -> "Both"
            normalizedType.contains("dirty") || normalizedType.contains("soiled") || normalizedType.contains("poo") -> "Dirty"
            else -> "Wet"
        }

        val event = UnifiedEventDto(
            id = UUID.randomUUID().toString(),
            time = time,
            dateTimeString = "$date $time",
            type = "NAPPY",
            nappyType = internalNappyType
        )

        return try {
            repository.saveActivityEvent(userId, date, event)
            NappyChangeResult(true, "Logged a $internalNappyType nappy change at $time.")
        } catch (e: Exception) {
            NappyChangeResult(false, "Failed to log nappy change: ${e.message}")
        }
    }
}
