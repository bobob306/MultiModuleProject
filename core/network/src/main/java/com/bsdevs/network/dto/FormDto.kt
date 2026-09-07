package com.bsdevs.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class FormFieldConditionDto(
    val fieldKey: String = "",
    val equals: JsonElement? = null,
)

@Serializable
data class FormFieldDto(
    val fieldKey: String = "",
    val type: String = "",
    val label: String = "",
    val required: Boolean = false,
    val index: Int = 0,
    val placeholder: String? = null,
    val defaultValue: JsonElement? = null,
    val options: List<String> = emptyList(),
    val multiSelect: Boolean = false,
    val editable: Boolean = false,
    val dynamicOptions: Map<String, String>? = null,
    val startNumber: Int = 0,
    val endNumber: Int = 100,
    val decimalPlaces: Int = 0,
    val showWhen: FormFieldConditionDto? = null,
)

@Serializable
data class FormSchemaDto(
    val title: String = "",
    val submitTarget: String = "",
    val submitDestination: String = "",
    val deletable: Boolean = false,
    val fields: List<FormFieldDto> = emptyList(),
)

@Serializable
data class FormSubmissionDto(
    val submittedAt: Long? = null,
    val values: Map<String, JsonElement> = emptyMap(),
)
