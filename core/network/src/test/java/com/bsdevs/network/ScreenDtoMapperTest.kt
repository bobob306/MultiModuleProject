package com.bsdevs.network

import com.bsdevs.network.dto.ButtonType
import com.bsdevs.network.dto.LocationType
import com.bsdevs.network.dto.ScreenDto
import com.bsdevs.network.dto.SizeDto
import com.bsdevs.network.dto.SpacerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScreenDtoMapperTest {

    private lateinit var mapper: ScreenDtoMapperImpl

    @Before
    fun setUp() {
        mapper = ScreenDtoMapperImpl()
    }

    @Test
    fun `mapToDto converts TitleHashMap correctly`() {
        val titleMap = hashMapOf(
            "type" to "TITLE",
            "index" to 0,
            "content" to "Title Content"
        )
        val rootMap = hashMapOf("items" to listOf(titleMap))

        val result = mapper.mapToDto(rootMap)

        assertEquals(1, result.size)
        val titleDto = result[0] as ScreenDto.TitleDto
        assertEquals(0, titleDto.index)
        assertEquals("Title Content", titleDto.content)
    }

    @Test
    fun `mapToDto converts SpacerHashMap correctly`() {
        // [type, value]
        val sizeList = arrayListOf("HEIGHT", 24)
        val spacerMap = hashMapOf(
            "type" to "SPACER",
            "index" to 1,
            "size" to sizeList
        )
        val rootMap = hashMapOf("items" to listOf(spacerMap))

        val result = mapper.mapToDto(rootMap)

        assertEquals(1, result.size)
        val spacerDto = result[0] as ScreenDto.SpacerDto
        assertEquals(1, spacerDto.index)
        assertEquals(SpacerType.HEIGHT, spacerDto.size.type)
        assertEquals(24, spacerDto.size.size)
    }

    @Test
    fun `mapToDto converts SpacerHashMap with WEIGHT correctly`() {
        val sizeList = arrayListOf("WEIGHT", 1.5)
        val spacerMap = hashMapOf(
            "type" to "SPACER",
            "index" to 1,
            "size" to sizeList
        )
        val rootMap = hashMapOf("items" to listOf(spacerMap))

        val result = mapper.mapToDto(rootMap)

        val spacerDto = result[0] as ScreenDto.SpacerDto
        assertEquals(SpacerType.WEIGHT, spacerDto.size.type)
        assertEquals(1.5f, spacerDto.size.weight)
    }

    @Test
    fun `mapToDto converts CardHashMap correctly`() {
        val imageMap = hashMapOf(
            "index" to 10,
            "url" to "url",
            "contentDescription" to "desc",
            "height" to 50,
            "width" to 50
        )
        val cardMap = hashMapOf(
            "type" to "CARD",
            "index" to 2,
            "title" to "Card Title",
            "subtitle" to "Card Subtitle",
            "backgroundColor" to 123456,
            "IMAGE" to imageMap
        )
        val rootMap = hashMapOf("items" to listOf(cardMap))

        val result = mapper.mapToDto(rootMap)

        assertEquals(1, result.size)
        val cardDto = result[0] as ScreenDto.CardDto
        assertEquals(2, cardDto.index)
        assertEquals("Card Title", cardDto.title)
        assertEquals(123456, cardDto.backgroundColor)
        assertEquals("url", cardDto.image.url)
    }

    @Test
    fun `mapToDto converts standalone IMAGE correctly`() {
        val imageMap = hashMapOf(
            "type" to "IMAGE",
            "index" to 10,
            "url" to "url",
            "contentDescription" to "desc",
            "height" to 50,
            "width" to 50
        )
        val rootMap = hashMapOf("items" to listOf(imageMap))

        val result = mapper.mapToDto(rootMap)

        assertEquals(1, result.size)
        val imageDto = result[0] as ScreenDto.ImageDto
        assertEquals("url", imageDto.url)
        assertEquals(50, imageDto.height)
    }

    @Test
    fun `mapToDto converts NavigationButtonHashMap correctly`() {
        val btnMap = hashMapOf(
            "type" to "NAVIGATION_BUTTON",
            "index" to 3,
            "label" to "Back",
            "destination" to "home",
            "location" to "EXTERNAL",
            "sort" to "SECONDARY"
        )
        val rootMap = hashMapOf("items" to listOf(btnMap))

        val result = mapper.mapToDto(rootMap)

        assertEquals(1, result.size)
        val btnDto = result[0] as ScreenDto.NavigationButtonDto
        assertEquals(3, btnDto.index)
        assertEquals("Back", btnDto.label)
        assertEquals(LocationType.EXTERNAL, btnDto.location)
        assertEquals(ButtonType.SECONDARY, btnDto.sort)
    }

    @Test
    fun `mapToDto handles Unknown type`() {
        val unknownMap = hashMapOf("type" to "WHATEVER", "index" to 5)
        val rootMap = hashMapOf("items" to listOf(unknownMap))

        val result = mapper.mapToDto(rootMap)

        assertEquals(1, result.size)
        assertTrue(result[0] is ScreenDto.Unknown)
    }

    @Test
    fun `mapToDto flattens multiple categories`() {
        val map1 = hashMapOf("type" to "TITLE", "index" to 0, "content" to "T1")
        val map2 = hashMapOf("type" to "TITLE", "index" to 1, "content" to "T2")
        
        val rootMap = hashMapOf(
            "cat1" to listOf(map1),
            "cat2" to listOf(map2)
        )

        val result = mapper.mapToDto(rootMap)

        assertEquals(2, result.size)
    }

    @Test(expected = Exception::class)
    fun `mapToDto crashes if required field index is missing`() {
        // This test documents a current fragility in the mapper
        val titleMap = hashMapOf(
            "type" to "TITLE",
            "content" to "No Index"
        )
        val rootMap = hashMapOf("items" to listOf(titleMap))

        mapper.mapToDto(rootMap)
    }

    @Test
    fun `mapToDto handles numeric values arriving as Long correctly`() {
        // Firestore often returns numbers as Longs
        val titleMap = hashMapOf(
            "type" to "TITLE",
            "index" to 5L,
            "content" to "T"
        )
        val rootMap = hashMapOf("items" to listOf(titleMap))
        val result = mapper.mapToDto(rootMap)
        assertEquals(5, result[0].index)
    }

    @Test
    fun `mapToDto handles backgroundColor as hex string safely`() {
        val cardMap = hashMapOf(
            "type" to "CARD",
            "index" to 0,
            "title" to "T",
            "subtitle" to "S",
            "backgroundColor" to "invalid",
            "IMAGE" to hashMapOf("index" to 0, "url" to "u", "contentDescription" to "d", "height" to 1, "width" to 1)
        )
        val rootMap = hashMapOf("items" to listOf(cardMap))
        val result = mapper.mapToDto(rootMap) as List<ScreenDto.CardDto>
        assertNull(result[0].backgroundColor)
    }

    @Test
    fun `mapToDto conversion of NAVIGATION_BUTTON handles non-standard enum strings`() {
        val btnMap = hashMapOf(
            "type" to "NAVIGATION_BUTTON",
            "index" to 0,
            "label" to "L",
            "destination" to "D",
            "location" to "GHOST_LOCATION", // Invalid
            "sort" to "GHOST_SORT" // Invalid
        )
        val rootMap = hashMapOf("items" to listOf(btnMap))
        val result = mapper.mapToDto(rootMap) as List<ScreenDto.NavigationButtonDto>
        
        assertEquals(LocationType.INTERNAL, result[0].location)
        assertEquals(ButtonType.PRIMARY, result[0].sort)
    }

    @Test
    fun `mapToDto handles multiple list entries in root map`() {
        val rootMap = hashMapOf(
            "list1" to listOf(hashMapOf("type" to "TITLE", "index" to 0, "content" to "T1")),
            "list2" to listOf(hashMapOf("type" to "TITLE", "index" to 1, "content" to "T2")),
            "list3" to emptyList<Any>()
        )
        val result = mapper.mapToDto(rootMap)
        assertEquals(2, result.size)
    }

    @Test(expected = Exception::class)
    fun `mapToDto crashes if index arrives as decimal Double`() {
        // This confirms a bug: .toString().toInt() fails on "1.0"
        val titleMap = hashMapOf(
            "type" to "TITLE",
            "index" to 1.0,
            "content" to "T"
        )
        val rootMap = hashMapOf("items" to listOf(titleMap))
        mapper.mapToDto(rootMap)
    }

    @Test
    fun `mapToDto handles Spacer with WEIGHT correctly`() {
        val sizeList = arrayListOf("WEIGHT", 0.5)
        val spacerMap = hashMapOf("type" to "SPACER", "index" to 0, "size" to sizeList)
        val rootMap = hashMapOf("items" to listOf(spacerMap))
        
        val result = mapper.mapToDto(rootMap) as List<ScreenDto.SpacerDto>
        assertEquals(SpacerType.WEIGHT, result[0].size.type)
        assertEquals(0.5f, result[0].size.weight!!, 0.01f)
    }
}
