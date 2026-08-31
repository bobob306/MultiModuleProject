package com.bsdevs.data

data class FormFieldCondition(
    val fieldKey: String,
    val equals: Any,
)

sealed class FormFieldData(
    open val fieldKey: String,
    open val label: String,
    open val required: Boolean,
    open val index: Int,
    open val showWhen: FormFieldCondition? = null,
) {
    data class TextInputData(
        override val fieldKey: String,
        override val label: String,
        override val required: Boolean,
        override val index: Int,
        val placeholder: String?,
        override val showWhen: FormFieldCondition? = null,
    ) : FormFieldData(fieldKey, label, required, index, showWhen)

    data class NumberInputData(
        override val fieldKey: String,
        override val label: String,
        override val required: Boolean,
        override val index: Int,
        val placeholder: String?,
        override val showWhen: FormFieldCondition? = null,
    ) : FormFieldData(fieldKey, label, required, index, showWhen)

    data class SwitchFieldData(
        override val fieldKey: String,
        override val label: String,
        override val required: Boolean,
        override val index: Int,
        val default: Boolean,
        override val showWhen: FormFieldCondition? = null,
    ) : FormFieldData(fieldKey, label, required, index, showWhen)

    data class RadioFieldData(
        override val fieldKey: String,
        override val label: String,
        override val required: Boolean,
        override val index: Int,
        val options: List<String>,
        override val showWhen: FormFieldCondition? = null,
    ) : FormFieldData(fieldKey, label, required, index, showWhen)

    data class CheckboxListFieldData(
        override val fieldKey: String,
        override val label: String,
        override val required: Boolean,
        override val index: Int,
        val options: List<String>,
        override val showWhen: FormFieldCondition? = null,
    ) : FormFieldData(fieldKey, label, required, index, showWhen)

    data class DropdownFieldData(
        override val fieldKey: String,
        override val label: String,
        override val required: Boolean,
        override val index: Int,
        val options: List<String>,
        val multiSelect: Boolean,
        override val showWhen: FormFieldCondition? = null,
    ) : FormFieldData(fieldKey, label, required, index, showWhen)

    data class DateInputData(
        override val fieldKey: String,
        override val label: String,
        override val required: Boolean,
        override val index: Int,
        override val showWhen: FormFieldCondition? = null,
    ) : FormFieldData(fieldKey, label, required, index, showWhen)

    data class TimeInputData(
        override val fieldKey: String,
        override val label: String,
        override val required: Boolean,
        override val index: Int,
        override val showWhen: FormFieldCondition? = null,
    ) : FormFieldData(fieldKey, label, required, index, showWhen)

    data class WheelInputData(
        override val fieldKey: String,
        override val label: String,
        override val required: Boolean,
        override val index: Int,
        val startNumber: Int,
        val endNumber: Int,
        val decimalPlaces: Int,
        val defaultValue: Int,
        override val showWhen: FormFieldCondition? = null,
    ) : FormFieldData(fieldKey, label, required, index, showWhen)

    data class Unknown(
        override val fieldKey: String,
        override val label: String,
        override val required: Boolean,
        override val index: Int,
        override val showWhen: FormFieldCondition? = null,
    ) : FormFieldData(fieldKey, label, required, index, showWhen)
}
