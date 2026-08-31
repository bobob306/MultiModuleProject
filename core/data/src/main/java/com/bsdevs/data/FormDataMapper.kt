package com.bsdevs.data

import com.bsdevs.network.dto.FormFieldConditionDto
import com.bsdevs.network.dto.FormFieldDto
import com.bsdevs.network.dto.FormSchemaDto
import javax.inject.Inject

interface FormDataMapper {
    fun mapToData(formId: String, dto: FormSchemaDto): FormSchemaData
}

class FormDataMapperImpl @Inject constructor() : FormDataMapper {
    override fun mapToData(formId: String, dto: FormSchemaDto): FormSchemaData = FormSchemaData(
        formId = formId,
        title = dto.title,
        submitTarget = dto.submitTarget,
        submitDestination = dto.submitDestination,
        deletable = dto.deletable,
        fields = dto.fields.map { it.toFieldData() },
    )

    private fun FormFieldDto.toFieldData(): FormFieldData {
        val condition = showWhen?.toCondition()
        return when (type) {
            "TEXT_INPUT" -> FormFieldData.TextInputData(fieldKey, label, required, index, placeholder, condition)
            "NUMBER_INPUT" -> FormFieldData.NumberInputData(fieldKey, label, required, index, placeholder, condition)
            "SWITCH" -> FormFieldData.SwitchFieldData(fieldKey, label, required, index, defaultValue as? Boolean ?: false, condition)
            "RADIO" -> FormFieldData.RadioFieldData(fieldKey, label, required, index, options, condition)
            "CHECKBOX_LIST" -> FormFieldData.CheckboxListFieldData(fieldKey, label, required, index, options, condition)
            "DROPDOWN" -> FormFieldData.DropdownFieldData(fieldKey, label, required, index, options, multiSelect, condition)
            "DATE_INPUT" -> FormFieldData.DateInputData(fieldKey, label, required, index, condition)
            "TIME_INPUT" -> FormFieldData.TimeInputData(fieldKey, label, required, index, condition)
            "WHEEL_INPUT" -> FormFieldData.WheelInputData(fieldKey, label, required, index, startNumber, endNumber, decimalPlaces, (defaultValue as? Number)?.toInt() ?: startNumber, condition)
            else -> FormFieldData.Unknown(fieldKey, label, required, index, condition)
        }
    }

    private fun FormFieldConditionDto.toCondition(): FormFieldCondition? {
        val value = equals ?: return null
        return FormFieldCondition(fieldKey, value)
    }
}
