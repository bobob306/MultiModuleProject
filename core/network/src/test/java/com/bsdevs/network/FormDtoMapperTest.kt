package com.bsdevs.network

import com.bsdevs.network.dto.FormSchemaDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FormDtoMapperTest {

    private lateinit var mapper: FormDtoMapperImpl

    @Before
    fun setUp() {
        mapper = FormDtoMapperImpl()
    }

    private fun rootMap(vararg fields: HashMap<*, *>, extra: Map<String, Any> = emptyMap()): HashMap<String, Any> {
        val base: HashMap<String, Any> = hashMapOf(
            "title" to "Test Form",
            "submitTarget" to "testTarget",
            "submitDestination" to "home",
            "fields" to fields.toList(),
        )
        base.putAll(extra)
        return base
    }

    @Test
    fun `maps title, submitTarget, submitDestination`() {
        val result = mapper.mapToDto(rootMap())
        assertEquals("Test Form", result.title)
        assertEquals("testTarget", result.submitTarget)
        assertEquals("home", result.submitDestination)
    }

    @Test
    fun `deletable defaults to false when absent`() {
        val result = mapper.mapToDto(rootMap())
        assertFalse(result.deletable)
    }

    @Test
    fun `deletable maps to true when set`() {
        val result = mapper.mapToDto(rootMap(extra = mapOf("deletable" to true)))
        assertTrue(result.deletable)
    }

    @Test
    fun `maps TEXT_INPUT field`() {
        val field = hashMapOf(
            "fieldKey" to "name", "type" to "TEXT_INPUT", "label" to "Name",
            "required" to true, "index" to 0, "placeholder" to "Enter name",
        )
        val dto = mapper.mapToDto(rootMap(field)).fields[0]
        assertEquals("name", dto.fieldKey)
        assertEquals("TEXT_INPUT", dto.type)
        assertTrue(dto.required)
        assertEquals(0, dto.index)
        assertEquals("Enter name", dto.placeholder)
    }

    @Test
    fun `maps DROPDOWN field with options and multiSelect`() {
        val field = hashMapOf(
            "fieldKey" to "origins", "type" to "DROPDOWN", "label" to "Origins",
            "required" to true, "index" to 0,
            "options" to listOf("Brazil", "Colombia"),
            "multiSelect" to true,
        )
        val dto = mapper.mapToDto(rootMap(field)).fields[0]
        assertEquals(listOf("Brazil", "Colombia"), dto.options)
        assertTrue(dto.multiSelect)
    }

    @Test
    fun `maps RADIO field`() {
        val field = hashMapOf(
            "fieldKey" to "type", "type" to "RADIO", "label" to "Type",
            "required" to true, "index" to 0,
            "options" to listOf("Wet", "Dirty", "Both"),
        )
        val dto = mapper.mapToDto(rootMap(field)).fields[0]
        assertEquals("RADIO", dto.type)
        assertEquals(listOf("Wet", "Dirty", "Both"), dto.options)
    }

    @Test
    fun `maps SWITCH field with defaultValue`() {
        val field = hashMapOf(
            "fieldKey" to "subscribe", "type" to "SWITCH", "label" to "Subscribe",
            "required" to false, "index" to 0, "defaultValue" to true,
        )
        val dto = mapper.mapToDto(rootMap(field)).fields[0]
        assertEquals("SWITCH", dto.type)
        assertEquals(true, dto.defaultValue)
    }

    @Test
    fun `maps DATE_INPUT and TIME_INPUT fields`() {
        val dateField = hashMapOf("fieldKey" to "date", "type" to "DATE_INPUT", "label" to "Date", "required" to true, "index" to 0)
        val timeField = hashMapOf("fieldKey" to "time", "type" to "TIME_INPUT", "label" to "Time", "required" to true, "index" to 1)
        val dtos = mapper.mapToDto(rootMap(dateField, timeField)).fields
        assertEquals("DATE_INPUT", dtos[0].type)
        assertEquals("TIME_INPUT", dtos[1].type)
    }

    @Test
    fun `sorts fields by index`() {
        val field0 = hashMapOf("fieldKey" to "first", "type" to "TEXT_INPUT", "label" to "First", "required" to false, "index" to 0)
        val field2 = hashMapOf("fieldKey" to "third", "type" to "TEXT_INPUT", "label" to "Third", "required" to false, "index" to 2)
        val field1 = hashMapOf("fieldKey" to "second", "type" to "TEXT_INPUT", "label" to "Second", "required" to false, "index" to 1)
        val dtos = mapper.mapToDto(rootMap(field2, field0, field1)).fields
        assertEquals("first", dtos[0].fieldKey)
        assertEquals("second", dtos[1].fieldKey)
        assertEquals("third", dtos[2].fieldKey)
    }

    @Test
    fun `index falls back to list position when absent`() {
        val field = hashMapOf("fieldKey" to "name", "type" to "TEXT_INPUT", "label" to "Name", "required" to false)
        val dto = mapper.mapToDto(rootMap(field)).fields[0]
        assertEquals(0, dto.index)
    }

    @Test
    fun `missing placeholder maps to null`() {
        val field = hashMapOf("fieldKey" to "name", "type" to "TEXT_INPUT", "label" to "Name", "required" to false, "index" to 0)
        assertNull(mapper.mapToDto(rootMap(field)).fields[0].placeholder)
    }

    @Test
    fun `empty fields list maps to empty list`() {
        val result = mapper.mapToDto(rootMap())
        assertTrue(result.fields.isEmpty())
    }

    @Test
    fun `maps WHEEL_INPUT field with startNumber, endNumber, decimalPlaces`() {
        val field = hashMapOf(
            "fieldKey" to "temp", "type" to "WHEEL_INPUT", "label" to "Temperature",
            "required" to true, "index" to 0,
            "startNumber" to 350, "endNumber" to 420, "decimalPlaces" to 1, "defaultValue" to 370,
        )
        val dto = mapper.mapToDto(rootMap(field)).fields[0]
        assertEquals("WHEEL_INPUT", dto.type)
        assertEquals(350, dto.startNumber)
        assertEquals(420, dto.endNumber)
        assertEquals(1, dto.decimalPlaces)
        assertEquals(370, (dto.defaultValue as Number).toInt())
    }

    @Test
    fun `WHEEL_INPUT startNumber defaults to 0 when absent`() {
        val field = hashMapOf("fieldKey" to "x", "type" to "WHEEL_INPUT", "label" to "X", "required" to false, "index" to 0)
        val dto = mapper.mapToDto(rootMap(field)).fields[0]
        assertEquals(0, dto.startNumber)
    }

    @Test
    fun `maps showWhen condition`() {
        val field = hashMapOf(
            "fieldKey" to "height", "type" to "WHEEL_INPUT", "label" to "Height",
            "required" to false, "index" to 0,
            "showWhen" to mapOf("fieldKey" to "record_height", "equals" to true),
        )
        val dto = mapper.mapToDto(rootMap(field)).fields[0]
        assertNull(null) // showWhen is present
        assertEquals("record_height", dto.showWhen?.fieldKey)
        assertEquals(true, dto.showWhen?.equals)
    }

    @Test
    fun `showWhen is null when absent`() {
        val field = hashMapOf("fieldKey" to "x", "type" to "TEXT_INPUT", "label" to "X", "required" to false, "index" to 0)
        val dto = mapper.mapToDto(rootMap(field)).fields[0]
        assertNull(dto.showWhen)
    }
}
