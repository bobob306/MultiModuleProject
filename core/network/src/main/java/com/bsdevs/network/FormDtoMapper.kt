package com.bsdevs.network

import com.bsdevs.network.dto.FormFieldConditionDto
import com.bsdevs.network.dto.FormFieldDto
import com.bsdevs.network.dto.FormSchemaDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import javax.inject.Inject

interface FormDtoMapper : FirebaseMapper<Map<*, *>, FormSchemaDto>

class FormDtoMapperImpl @Inject constructor() : FormDtoMapper {
    private val json = Json { ignoreUnknownKeys = true }

    override fun mapToDto(map: Map<*, *>): FormSchemaDto {
        val rawFields = (map["fields"] as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
        return FormSchemaDto(
            title = map["title"] as? String ?: "",
            submitTarget = map["submitTarget"] as? String ?: "",
            submitDestination = map["submitDestination"] as? String ?: "",
            deletable = map["deletable"] as? Boolean ?: false,
            fields = rawFields.mapIndexed { idx, field ->
                val showWhenMap = field["showWhen"] as? Map<*, *>
                FormFieldDto(
                    fieldKey = field["fieldKey"] as? String ?: "",
                    type = field["type"] as? String ?: "",
                    label = field["label"] as? String ?: "",
                    required = field["required"] as? Boolean ?: false,
                    index = (field["index"] as? Number)?.toInt() ?: idx,
                    placeholder = field["placeholder"] as? String,
                    defaultValue = field["defaultValue"]?.let { anyToJson(it) },
                    options = (field["options"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    multiSelect = field["multiSelect"] as? Boolean ?: false,
                    editable = field["editable"] as? Boolean ?: false,
                    dynamicOptions = (field["dynamicOptions"] as? Map<*, *>)?.map { it.key.toString() to it.value.toString() }?.toMap(),
                    startNumber = (field["startNumber"] as? Number)?.toInt() ?: 0,
                    endNumber = (field["endNumber"] as? Number)?.toInt() ?: 100,
                    decimalPlaces = (field["decimalPlaces"] as? Number)?.toInt() ?: 0,
                    showWhen = showWhenMap?.let {
                        FormFieldConditionDto(
                            fieldKey = it["fieldKey"] as? String ?: "",
                            equals = it["equals"]?.let { anyToJson(it) },
                        )
                    },
                )
            }.sortedBy { it.index }
        )
    }

    private fun anyToJson(any: Any): JsonElement {
        return when (any) {
            is String -> json.encodeToJsonElement(any)
            is Boolean -> json.encodeToJsonElement(any)
            is Int -> json.encodeToJsonElement(any)
            is Long -> json.encodeToJsonElement(any)
            is Double -> json.encodeToJsonElement(any)
            is Float -> json.encodeToJsonElement(any)
            else -> json.encodeToJsonElement(any.toString())
        }
    }
}
