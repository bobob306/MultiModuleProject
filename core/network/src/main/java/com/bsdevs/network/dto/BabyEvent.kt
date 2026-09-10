package com.bsdevs.network.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class BabyEvent {
    abstract val id: String
    abstract val time: String
    abstract val dateTimeString: String
    abstract val comment: String?
    abstract val isPendingSync: Boolean

    val type: String
        get() = when (this) {
            is Feeding -> "FEEDING"
            is Nappy -> "NAPPY"
            is Temperature -> "TEMPERATURE"
            is Measurement -> "MEASUREMENT"
            is Vaccination -> "VACCINATION"
            is Unknown -> this.unknownType
            is VitaminD -> "VITAMIN_D"
        }

    fun withPendingSync(pending: Boolean): BabyEvent {
        return when (this) {
            is Feeding -> copy(isPendingSync = pending)
            is Nappy -> copy(isPendingSync = pending)
            is Temperature -> copy(isPendingSync = pending)
            is Measurement -> copy(isPendingSync = pending)
            is Vaccination -> copy(isPendingSync = pending)
            is VitaminD -> copy(isPendingSync = pending)
            is Unknown -> copy(isPendingSync = pending)
        }
    }

    @Serializable
    @SerialName("FEEDING")
    data class Feeding(
        override val id: String = "",
        override val time: String = "",
        override val dateTimeString: String = "",
        override val comment: String? = null,
        override val isPendingSync: Boolean = false,
        val mainFeedingSide: String? = null,
        val leftDuration: Long = 0,
        val rightDuration: Long = 0,
        val totalDuration: Long = 0,
        val bottleAmountMl: Int? = null,
        val hasVitaminD: Boolean? = null,
        val predictionGapMinutes: Long? = null
    ) : BabyEvent()

    @Serializable
    @SerialName("NAPPY")
    data class Nappy(
        override val id: String = "",
        override val time: String = "",
        override val dateTimeString: String = "",
        override val comment: String? = null,
        override val isPendingSync: Boolean = false,
        val nappyType: String? = null
    ) : BabyEvent()

    @Serializable
    @SerialName("TEMPERATURE")
    data class Temperature(
        override val id: String = "",
        override val time: String = "",
        override val dateTimeString: String = "",
        override val comment: String? = null,
        override val isPendingSync: Boolean = false,
        val temperature: Double? = null
    ) : BabyEvent()

    @Serializable
    @SerialName("MEASUREMENT")
    data class Measurement(
        override val id: String = "",
        override val time: String = "",
        override val dateTimeString: String = "",
        override val comment: String? = null,
        override val isPendingSync: Boolean = false,
        val height: Double? = null,
        val weight: Double? = null,
        val headCircumference: Double? = null,
        val isMedical: Boolean? = null
    ) : BabyEvent()

    @Serializable
    @SerialName("VACCINATION")
    data class Vaccination(
        override val id: String = "",
        override val time: String = "",
        override val dateTimeString: String = "",
        override val comment: String? = null,
        override val isPendingSync: Boolean = false,
        val vaccinationNames: List<String>? = null,
        val location: String? = null,
        val seriesId: String? = null
    ) : BabyEvent()

    @Serializable
    @SerialName("VITAMIN_D")
    data class VitaminD(
        override val id: String = "",
        override val time: String = "",
        override val dateTimeString: String = "",
        override val comment: String? = null,
        override val isPendingSync: Boolean = false
    ) : BabyEvent()

    @Serializable
    @SerialName("UNKNOWN")
    data class Unknown(
        override val id: String = "",
        override val time: String = "",
        override val dateTimeString: String = "",
        override val comment: String? = null,
        override val isPendingSync: Boolean = false,
        val unknownType: String = "UNKNOWN"
    ) : BabyEvent()
}
