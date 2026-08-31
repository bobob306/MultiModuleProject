package com.bsdevs.homescreen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Documents the structure of each form seed and serves as the contract for what
 * gets written to Firestore on first launch.
 *
 * HOW TO ADD A NEW FORM
 * =====================
 * 1. Add a new `val yourForm: Map<String, Any>` to [FormSeeds] with the required structure:
 *    - "title"              : String  - shown in the form's top bar
 *    - "submitTarget"       : String  - routing key in FormSubmitRouter; add a matching branch there
 *    - "submitDestination"  : String  - nav destination after submit ("home", "coffee_home", "baby_home")
 *    - "deletable"          : Boolean - shows the Delete button when editing an existing record
 *    - "fields"             : List    - ordered list of field maps (see field types below)
 *
 * 2. Add `formRepository.seedFormIfAbsent("yourFormId", FormSeeds.yourForm)` in
 *    HomeScreenViewModel.seedForms().
 *
 * 3. Add a matching when-branch in FormSubmitRouter.submit() for "yourFormId".
 *
 * 4. If the form supports edit/delete, add matching branches in FormPrefillerImpl
 *    and FormDeleterImpl.
 *
 * FIELD TYPES
 * ===========
 * TEXT_INPUT   : { fieldKey, type, label, required, index, placeholder? }
 * NUMBER_INPUT : { fieldKey, type, label, required, index, placeholder? }
 * DATE_INPUT   : { fieldKey, type, label, required, index }
 * TIME_INPUT   : { fieldKey, type, label, required, index }
 * SWITCH       : { fieldKey, type, label, required, index, defaultValue: Boolean }
 * RADIO        : { fieldKey, type, label, required, index, options: List<String> }
 * CHECKBOX_LIST: { fieldKey, type, label, required, index, options: List<String> }
 * DROPDOWN     : { fieldKey, type, label, required, index, options, multiSelect: Boolean }
 * WHEEL_INPUT  : { fieldKey, type, label, required, index, startNumber, endNumber,
 *                  decimalPlaces, defaultValue: Int }
 *
 * Conditional visibility: any field can include
 *   "showWhen": { "fieldKey": "otherField", "equals": <value> }
 *
 * UPDATING AN EXISTING FORM
 * =========================
 * seedFormIfAbsent only writes when the document is absent. To push an updated seed:
 *   - Option A (quick): delete the document in the Firebase console; it re-seeds on next launch.
 *   - Option B (automated): add "version": N to the seed map and update FormRepositoryImpl
 *     to overwrite when the stored version < seed version. See the version discussion in
 *     the session history for the implementation sketch.
 */
class FormSeedsTest {

    // --- coffeeLog ---

    @Test
    fun `coffeeLog has correct top-level metadata`() {
        val seed = FormSeeds.coffeeLog
        assertEquals("coffeeLog", seed["submitTarget"])
        assertEquals("coffee_home", seed["submitDestination"])
        assertFalse(seed["deletable"] as Boolean)
        assertNotNull(seed["title"])
    }

    @Test
    fun `coffeeLog fields are ordered by index and contain expected types`() {
        val fields = formFields(FormSeeds.coffeeLog)
        assertEquals("DROPDOWN", fields[0]["type"])  // bean_types
        assertEquals("DROPDOWN", fields[1]["type"])  // origin_countries
        assertEquals("DROPDOWN", fields[2]["type"])  // tasting_notes
        assertEquals("DROPDOWN", fields[3]["type"])  // preparation_method
        assertEquals("DROPDOWN", fields[4]["type"])  // roaster
        assertEquals("RADIO",    fields[5]["type"])  // is_decaf
        assertEquals("DATE_INPUT", fields[6]["type"]) // roast_date
    }

    @Test
    fun `coffeeLog multi-select dropdowns are marked correctly`() {
        val fields = formFields(FormSeeds.coffeeLog)
        assertTrue(fields[0]["multiSelect"] as Boolean)  // bean_types multi
        assertTrue(fields[1]["multiSelect"] as Boolean)  // origin_countries multi
        assertFalse(fields[3]["multiSelect"] as Boolean) // preparation_method single
        assertFalse(fields[4]["multiSelect"] as Boolean) // roaster single
    }

    @Test
    fun `coffeeLog is_decaf radio has Caffeinated and Decaffeinated options`() {
        val decafField = formFields(FormSeeds.coffeeLog).first { it["fieldKey"] == "is_decaf" }
        val options = decafField["options"] as List<*>
        assertTrue(options.contains("Caffeinated"))
        assertTrue(options.contains("Decaffeinated"))
    }

    // --- nappyLog ---

    @Test
    fun `nappyLog has correct top-level metadata`() {
        val seed = FormSeeds.nappyLog
        assertEquals("nappyLog", seed["submitTarget"])
        assertEquals("baby_home", seed["submitDestination"])
        assertTrue(seed["deletable"] as Boolean)
    }

    @Test
    fun `nappyLog fields cover date, time, type, comment`() {
        val fields = formFields(FormSeeds.nappyLog)
        assertEquals(4, fields.size)
        assertEquals("DATE_INPUT", fields[0]["type"])
        assertEquals("TIME_INPUT", fields[1]["type"])
        assertEquals("RADIO",      fields[2]["type"])
        assertEquals("TEXT_INPUT", fields[3]["type"])
    }

    @Test
    fun `nappyLog nappy_type radio has Wet Dirty Both options`() {
        val typeField = formFields(FormSeeds.nappyLog).first { it["fieldKey"] == "nappy_type" }
        val options = typeField["options"] as List<*>
        assertEquals(listOf("Wet", "Dirty", "Both"), options)
    }

    // --- temperatureLog ---

    @Test
    fun `temperatureLog has correct top-level metadata`() {
        val seed = FormSeeds.temperatureLog
        assertEquals("temperatureLog", seed["submitTarget"])
        assertEquals("baby_home", seed["submitDestination"])
        assertTrue(seed["deletable"] as Boolean)
    }

    @Test
    fun `temperatureLog wheel covers 35·0 to 42·0 celsius with 1 decimal place`() {
        val tempField = formFields(FormSeeds.temperatureLog).first { it["fieldKey"] == "temperature_value" }
        assertEquals("WHEEL_INPUT", tempField["type"])
        assertEquals(350, tempField["startNumber"])
        assertEquals(420, tempField["endNumber"])
        assertEquals(1, tempField["decimalPlaces"])
        assertEquals(370, tempField["defaultValue"]) // 37.0°C default
    }

    // --- measurementLog ---

    @Test
    fun `measurementLog has correct top-level metadata`() {
        val seed = FormSeeds.measurementLog
        assertEquals("measurementLog", seed["submitTarget"])
        assertEquals("baby_home", seed["submitDestination"])
        assertTrue(seed["deletable"] as Boolean)
    }

    @Test
    fun `measurementLog height wheel is conditional on record_height switch`() {
        val heightField = formFields(FormSeeds.measurementLog).first { it["fieldKey"] == "height_value" }
        val showWhen = heightField["showWhen"] as Map<*, *>
        assertEquals("record_height", showWhen["fieldKey"])
        assertEquals(true, showWhen["equals"])
    }

    @Test
    fun `measurementLog weight wheel is conditional on record_weight switch`() {
        val weightField = formFields(FormSeeds.measurementLog).first { it["fieldKey"] == "weight_value" }
        val showWhen = weightField["showWhen"] as Map<*, *>
        assertEquals("record_weight", showWhen["fieldKey"])
        assertEquals(true, showWhen["equals"])
    }

    @Test
    fun `measurementLog height wheel covers 30·0 to 120·0 cm`() {
        val field = formFields(FormSeeds.measurementLog).first { it["fieldKey"] == "height_value" }
        assertEquals(300, field["startNumber"])
        assertEquals(1200, field["endNumber"])
        assertEquals(1, field["decimalPlaces"])
    }

    @Test
    fun `measurementLog weight wheel covers 2·00 to 20·00 kg`() {
        val field = formFields(FormSeeds.measurementLog).first { it["fieldKey"] == "weight_value" }
        assertEquals(200, field["startNumber"])
        assertEquals(2000, field["endNumber"])
        assertEquals(2, field["decimalPlaces"])
    }

    @Suppress("UNCHECKED_CAST")
    private fun formFields(seed: Map<String, Any>): List<Map<String, Any>> =
        seed["fields"] as List<Map<String, Any>>
}
