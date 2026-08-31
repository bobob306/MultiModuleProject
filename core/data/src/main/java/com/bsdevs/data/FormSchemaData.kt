package com.bsdevs.data

data class FormSchemaData(
    val formId: String,
    val title: String,
    val submitTarget: String,
    val submitDestination: String,
    val deletable: Boolean,
    val fields: List<FormFieldData>,
)
