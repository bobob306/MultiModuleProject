package com.bsdevs.network.dto

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
@Serializable
@Keep
data class BabyDto(
    val id: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val middleName: String? = null,
    @get:PropertyName("birthDate")
    @set:PropertyName("birthDate")
    var birthDate: String? = null,
    @get:PropertyName("birth_date")
    @set:PropertyName("birth_date")
    var birth_date: String? = null,
    @get:PropertyName("dateOfBirth")
    @set:PropertyName("dateOfBirth")
    var dateOfBirth: String? = null,
    val gender: String? = null, // "male" or "female"
    val nextFeedingTime: String? = null,
    @get:PropertyName("next_feeding_time")
    @set:PropertyName("next_feeding_time")
    var next_feeding_time: String? = null,

    val nextFeedingTimeMin: String? = null,
    @get:PropertyName("next_feeding_time_min")
    @set:PropertyName("next_feeding_time_min")
    var next_feeding_time_min: String? = null,

    val nextFeedingTimeMax: String? = null,
    @get:PropertyName("next_feeding_time_max")
    @set:PropertyName("next_feeding_time_max")
    var next_feeding_time_max: String? = null,

    val predictionConfidenceRange: String? = null, // "low", "medium", "high"
    @get:PropertyName("prediction_confidence_range")
    @set:PropertyName("prediction_confidence_range")
    var prediction_confidence_range: String? = null,

    val activeModel: String? = null,
    @get:PropertyName("active_model")
    @set:PropertyName("active_model")
    var active_model: String? = null,

    val predictionsByModel: Map<String, String>? = null,
    @get:PropertyName("predictions_by_model")
    @set:PropertyName("predictions_by_model")
    var predictions_by_model: Map<String, String>? = null,

    val modelPerformance: Map<String, ModelPerformance>? = null,
    @get:PropertyName("model_performance")
    @set:PropertyName("model_performance")
    var model_performance: Map<String, ModelPerformance>? = null,

    val lastPredictionSync: String? = null,
    @get:PropertyName("last_prediction_sync")
    @set:PropertyName("last_prediction_sync")
    var last_prediction_sync: String? = null
) {
    val effectiveBirthDate: String?
        get() = birthDate ?: birth_date ?: dateOfBirth

    val effectiveNextFeedingTime: String?
        get() = nextFeedingTime ?: next_feeding_time

    val effectiveNextFeedingTimeMin: String?
        get() = nextFeedingTimeMin ?: next_feeding_time_min

    val effectiveNextFeedingTimeMax: String?
        get() = nextFeedingTimeMax ?: next_feeding_time_max

    val effectivePredictionConfidenceRange: String?
        get() = predictionConfidenceRange ?: prediction_confidence_range

    val effectiveActiveModel: String?
        get() = activeModel ?: active_model

    val effectivePredictionsByModel: Map<String, String>?
        get() = predictionsByModel ?: predictions_by_model

    val effectiveModelPerformance: Map<String, ModelPerformance>?
        get() = modelPerformance ?: model_performance

    val effectiveLastPredictionSync: String?
        get() = lastPredictionSync ?: last_prediction_sync
}

@Serializable
@Keep
data class ModelPerformance(
    val totalError: Double? = null,
    val count: Int? = null
)
