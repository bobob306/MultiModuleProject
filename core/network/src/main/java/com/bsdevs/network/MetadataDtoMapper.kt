package com.bsdevs.network

import com.bsdevs.network.dto.AppMetadataDto
import com.bsdevs.network.dto.FormSchemaDto
import javax.inject.Inject

interface MetadataDtoMapper : FirebaseMapper<Map<*, *>, AppMetadataDto> {
    fun mapToFirebase(dto: AppMetadataDto): Map<String, Any?>
}

class MetadataDtoMapperImpl @Inject constructor(
    private val screenMapper: ScreenDtoMapper,
    private val formMapper: FormDtoMapper
) : MetadataDtoMapper {
    override fun mapToDto(map: Map<*, *>): AppMetadataDto {
        val rawScreens = map["screens"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
        val rawForms = map["forms"] as? Map<*, *> ?: emptyMap<Any?, Any?>()

        val screens = rawScreens.map { (key, value) ->
            key.toString() to screenMapper.mapToDto(hashMapOf(key.toString() to value))
        }.toMap()

        val forms = rawForms.map { (key, value) ->
            key.toString() to formMapper.mapToDto(value as Map<*, *>)
        }.toMap()

        return AppMetadataDto(screens, forms)
    }

    override fun mapToFirebase(dto: AppMetadataDto): Map<String, Any?> {
        val screens = dto.screens.map { (id, components) ->
            val mapped = screenMapper.mapToFirebase(components)
            id to mapped["components"]
        }.toMap()

        val forms = dto.forms.map { (id, schema) ->
            id to mapFormToFirebase(schema)
        }.toMap()

        return mapOf(
            "screens" to screens,
            "forms" to forms
        )
    }

    // Since FormDtoMapper doesn't have mapToFirebase, we implement a simple version here
    private fun mapFormToFirebase(schema: FormSchemaDto): Map<String, Any?> {
        return mapOf(
            "title" to schema.title,
            "submitTarget" to schema.submitTarget,
            "submitDestination" to schema.submitDestination,
            "deletable" to schema.deletable,
            "fields" to schema.fields.map { field ->
                mapOf(
                    "fieldKey" to field.fieldKey,
                    "type" to field.type,
                    "label" to field.label,
                    "required" to field.required,
                    "index" to field.index,
                    "placeholder" to field.placeholder,
                    "defaultValue" to field.defaultValue, // Firestore can handle JsonElement if translated or just raw values
                    "options" to field.options,
                    "multiSelect" to field.multiSelect,
                    "editable" to field.editable,
                    "dynamicOptions" to field.dynamicOptions,
                    "startNumber" to field.startNumber,
                    "endNumber" to field.endNumber,
                    "decimalPlaces" to field.decimalPlaces,
                    "showWhen" to field.showWhen?.let {
                        mapOf(
                            "fieldKey" to it.fieldKey,
                            "equals" to it.equals
                        )
                    }
                )
            }
        )
    }
}
