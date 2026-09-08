package com.bsdevs.uicomponents

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.pow

class WheelPickerTests {

    @Test
    fun `decimal divisor calculation is correct`() {
        // Test the logic used in WheelInput to format numbers
        val decimalPlaces = 1
        val divisor = 10.0.pow(decimalPlaces.toDouble())
        assertEquals(10.0, divisor, 0.001)
        
        val selectedItem = 372
        val formatted = String.format("%.1f", selectedItem.toDouble() / divisor)
        // We use a dot as decimal separator for test comparison
        assertEquals("37.2", formatted.replace(",", "."))
    }

    @Test
    fun `decimal divisor for two places is correct`() {
        val decimalPlaces = 2
        val divisor = 10.0.pow(decimalPlaces.toDouble())
        assertEquals(100.0, divisor, 0.001)
        
        val selectedItem = 755
        val formatted = String.format("%.2f", selectedItem.toDouble() / divisor)
        assertEquals("7.55", formatted.replace(",", "."))
    }
}
