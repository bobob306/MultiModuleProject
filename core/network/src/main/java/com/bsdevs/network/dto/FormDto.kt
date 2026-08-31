package com.bsdevs.network.dto

import com.google.firebase.Timestamp

data class FormFieldConditionDto(
    val fieldKey: String = "",
    val equals: Any? = null,
)

data class FormFieldDto(
    val fieldKey: String = "",
    val type: String = "",
    val label: String = "",
    val required: Boolean = false,
    val index: Int = 0,
    val placeholder: String? = null,
    val defaultValue: Any? = null,
    val options: List<String> = emptyList(),
    val multiSelect: Boolean = false,
    val startNumber: Int = 0,
    val endNumber: Int = 100,
    val decimalPlaces: Int = 0,
    val showWhen: FormFieldConditionDto? = null,
)

data class FormSchemaDto(
    val title: String = "",
    val submitTarget: String = "",
    val submitDestination: String = "",
    val deletable: Boolean = false,
    val fields: List<FormFieldDto> = emptyList(),
)

data class FormSubmissionDto(
    val submittedAt: Timestamp? = null,
    val values: Map<String, Any> = emptyMap(),
)
