package com.bsdevs.data

import com.bsdevs.network.dto.FormFieldConditionDto
import com.bsdevs.network.dto.FormFieldDto
import com.bsdevs.network.dto.FormSchemaDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FormDataMapperTest {

    private lateinit var mapper: FormDataMapperImpl

    @Before
    fun setUp() {
        mapper = FormDataMapperImpl()
    }

    private fun schema(vararg fields: FormFieldDto, deletable: Boolean = false) = FormSchemaDto(
        title = "Test", submitTarget = "target", submitDestination = "home",
        deletable = deletable, fields = fields.toList(),
    )

    @Test
    fun `maps top-level schema fields`() {
        val result = mapper.mapToData("myForm", schema())
        assertEquals("myForm", result.formId)
        assertEquals("Test", result.title)
        assertEquals("target", result.submitTarget)
        assertEquals("home", result.submitDestination)
    }

    @Test
    fun `deletable false passes through`() {
        assertFalse(mapper.mapToData("f", schema(deletable = false)).deletable)
    }

    @Test
    fun `deletable true passes through`() {
        assertTrue(mapper.mapToData("f", schema(deletable = true)).deletable)
    }

    @Test
    fun `TEXT_INPUT maps to TextInputData`() {
        val field = FormFieldDto("name", "TEXT_INPUT", "Name", true, 0, "hint")
        val result = mapper.mapToData("f", schema(field)).fields[0]
        assertTrue(result is FormFieldData.TextInputData)
        val data = result as FormFieldData.TextInputData
        assertEquals("name", data.fieldKey)
        assertEquals("Name", data.label)
        assertTrue(data.required)
        assertEquals("hint", data.placeholder)
    }

    @Test
    fun `NUMBER_INPUT maps to NumberInputData`() {
        val field = FormFieldDto("age", "NUMBER_INPUT", "Age", false, 0, "e.g. 25")
        val result = mapper.mapToData("f", schema(field)).fields[0] as FormFieldData.NumberInputData
        assertEquals("age", result.fieldKey)
        assertEquals("e.g. 25", result.placeholder)
    }

    @Test
    fun `SWITCH maps to SwitchFieldData with default`() {
        val field = FormFieldDto("enabled", "SWITCH", "Enable", false, 0, defaultValue = true)
        val result = mapper.mapToData("f", schema(field)).fields[0] as FormFieldData.SwitchFieldData
        assertTrue(result.default)
    }

    @Test
    fun `SWITCH default is false when defaultValue absent`() {
        val field = FormFieldDto("enabled", "SWITCH", "Enable", false, 0)
        val result = mapper.mapToData("f", schema(field)).fields[0] as FormFieldData.SwitchFieldData
        assertFalse(result.default)
    }

    @Test
    fun `RADIO maps to RadioFieldData with options`() {
        val field = FormFieldDto("type", "RADIO", "Type", true, 0, options = listOf("Wet", "Dirty", "Both"))
        val result = mapper.mapToData("f", schema(field)).fields[0] as FormFieldData.RadioFieldData
        assertEquals(listOf("Wet", "Dirty", "Both"), result.options)
    }

    @Test
    fun `CHECKBOX_LIST maps to CheckboxListFieldData`() {
        val field = FormFieldDto("tags", "CHECKBOX_LIST", "Tags", false, 0, options = listOf("A", "B"))
        val result = mapper.mapToData("f", schema(field)).fields[0] as FormFieldData.CheckboxListFieldData
        assertEquals(listOf("A", "B"), result.options)
    }

    @Test
    fun `DROPDOWN single-select maps to DropdownFieldData`() {
        val field = FormFieldDto("roaster", "DROPDOWN", "Roaster", true, 0, options = listOf("X", "Y"), multiSelect = false)
        val result = mapper.mapToData("f", schema(field)).fields[0] as FormFieldData.DropdownFieldData
        assertFalse(result.multiSelect)
        assertEquals(listOf("X", "Y"), result.options)
    }

    @Test
    fun `DROPDOWN multi-select maps multiSelect true`() {
        val field = FormFieldDto("beans", "DROPDOWN", "Beans", true, 0, options = listOf("A"), multiSelect = true)
        val result = mapper.mapToData("f", schema(field)).fields[0] as FormFieldData.DropdownFieldData
        assertTrue(result.multiSelect)
    }

    @Test
    fun `DATE_INPUT maps to DateInputData`() {
        val field = FormFieldDto("date", "DATE_INPUT", "Date", true, 0)
        assertTrue(mapper.mapToData("f", schema(field)).fields[0] is FormFieldData.DateInputData)
    }

    @Test
    fun `TIME_INPUT maps to TimeInputData`() {
        val field = FormFieldDto("time", "TIME_INPUT", "Time", true, 0)
        assertTrue(mapper.mapToData("f", schema(field)).fields[0] is FormFieldData.TimeInputData)
    }

    @Test
    fun `unknown type maps to Unknown`() {
        val field = FormFieldDto("x", "FUTURE_TYPE", "X", false, 0)
        assertTrue(mapper.mapToData("f", schema(field)).fields[0] is FormFieldData.Unknown)
    }

    @Test
    fun `field index and required are preserved`() {
        val field = FormFieldDto("x", "TEXT_INPUT", "X", required = true, index = 7)
        val result = mapper.mapToData("f", schema(field)).fields[0]
        assertEquals(7, result.index)
        assertTrue(result.required)
    }

    @Test
    fun `WHEEL_INPUT maps to WheelInputData with correct properties`() {
        val field = FormFieldDto("temp", "WHEEL_INPUT", "Temperature", true, 0,
            startNumber = 350, endNumber = 420, decimalPlaces = 1, defaultValue = 370)
        val result = mapper.mapToData("f", schema(field)).fields[0] as FormFieldData.WheelInputData
        assertEquals(350, result.startNumber)
        assertEquals(420, result.endNumber)
        assertEquals(1, result.decimalPlaces)
        assertEquals(370, result.defaultValue)
    }

    @Test
    fun `WHEEL_INPUT defaultValue falls back to startNumber when absent`() {
        val field = FormFieldDto("temp", "WHEEL_INPUT", "Temperature", true, 0, startNumber = 350, endNumber = 420)
        val result = mapper.mapToData("f", schema(field)).fields[0] as FormFieldData.WheelInputData
        assertEquals(350, result.defaultValue)
    }

    @Test
    fun `showWhen condition maps to FormFieldCondition`() {
        val field = FormFieldDto("height", "WHEEL_INPUT", "Height", false, 0,
            showWhen = FormFieldConditionDto("record_height", true))
        val result = mapper.mapToData("f", schema(field)).fields[0]
        assertEquals("record_height", result.showWhen?.fieldKey)
        assertEquals(true, result.showWhen?.equals)
    }

    @Test
    fun `showWhen is null when condition equals is null`() {
        val field = FormFieldDto("x", "TEXT_INPUT", "X", false, 0,
            showWhen = FormFieldConditionDto("key", null))
        val result = mapper.mapToData("f", schema(field)).fields[0]
        assertNull(result.showWhen)
    }

    @Test
    fun `showWhen is null when absent`() {
        val field = FormFieldDto("x", "TEXT_INPUT", "X", false, 0)
        assertNull(mapper.mapToData("f", schema(field)).fields[0].showWhen)
    }
}
