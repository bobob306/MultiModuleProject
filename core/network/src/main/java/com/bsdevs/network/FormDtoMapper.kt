package com.bsdevs.network

import com.bsdevs.network.dto.FormFieldConditionDto
import com.bsdevs.network.dto.FormFieldDto
import com.bsdevs.network.dto.FormSchemaDto
import javax.inject.Inject

interface FormDtoMapper : FirebaseMapper<HashMap<*, *>, FormSchemaDto>

class FormDtoMapperImpl @Inject constructor() : FormDtoMapper {
    override fun mapToDto(map: HashMap<*, *>): FormSchemaDto {
        val rawFields = (map["fields"] as? List<*>)?.filterIsInstance<HashMap<*, *>>() ?: emptyList()
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
                    defaultValue = field["defaultValue"],
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
                            equals = it["equals"],
                        )
                    },
                )
            }.sortedBy { it.index }
        )
    }
}
