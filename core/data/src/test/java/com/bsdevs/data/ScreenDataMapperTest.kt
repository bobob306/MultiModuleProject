package com.bsdevs.data

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

class ScreenDataMapperTest {

    private lateinit var mapper: ScreenDataMapperImpl

    @Before
    fun setUp() {
        mapper = ScreenDataMapperImpl()
    }

    @Test
    fun `mapToData converts TitleDto correctly`() {
        val dto = listOf(ScreenDto.TitleDto(index = 0, content = "Hello Title"))
        val result = mapper.mapToData(dto)

        assertEquals(1, result.size)
        val titleData = result[0] as NetworkScreenData.TitleDataNetwork
        assertEquals(0, titleData.index)
        assertEquals("Hello Title", titleData.content)
    }

    @Test
    fun `mapToData converts SmallTitleDto correctly`() {
        val dto = listOf(ScreenDto.SmallTitleDto(index = 0, content = "Small Title"))
        val result = mapper.mapToData(dto)

        assertEquals(1, result.size)
        val titleData = result[0] as NetworkScreenData.SmallTitleDataNetwork
        assertEquals(0, titleData.index)
        assertEquals("Small Title", titleData.content)
    }

    @Test
    fun `mapToData converts SubtitleDto correctly`() {
        val dto = listOf(ScreenDto.SubtitleDto(index = 1, content = "Hello Subtitle"))
        val result = mapper.mapToData(dto)

        assertEquals(1, result.size)
        val subtitleData = result[0] as NetworkScreenData.SubtitleDataNetwork
        assertEquals(1, subtitleData.index)
        assertEquals("Hello Subtitle", subtitleData.content)
    }

    @Test
    fun `mapToData converts SpacerDto correctly`() {
        val dto = listOf(
            ScreenDto.SpacerDto(
                index = 2,
                size = SizeDto(type = SpacerType.HEIGHT, size = 16)
            )
        )
        val result = mapper.mapToData(dto)

        assertEquals(1, result.size)
        val spacerData = result[0] as NetworkScreenData.SpacerDataNetwork
        assertEquals(2, spacerData.index)
        assertEquals(SpacerTypeData.HEIGHT, spacerData.size.type)
        assertEquals(16, spacerData.size.height)
    }

    @Test
    fun `mapToData converts SpacerDto with WEIGHT correctly`() {
        val dto = listOf(
            ScreenDto.SpacerDto(
                index = 2,
                size = SizeDto(type = SpacerType.WEIGHT, weight = 1.5f)
            )
        )
        val result = mapper.mapToData(dto)

        val spacerData = result[0] as NetworkScreenData.SpacerDataNetwork
        assertEquals(SpacerTypeData.WEIGHT, spacerData.size.type)
        assertEquals(1.5f, spacerData.size.weight)
    }

    @Test
    fun `mapToData converts ImageDto correctly`() {
        val dto = listOf(
            ScreenDto.ImageDto(
                index = 3,
                url = "https://example.com/image.png",
                contentDescription = "Descr",
                height = 100,
                width = 200
            )
        )
        val result = mapper.mapToData(dto)

        assertEquals(1, result.size)
        val imageData = result[0] as NetworkScreenData.ImageDataNetwork
        assertEquals(3, imageData.index)
        assertEquals("https://example.com/image.png", imageData.url)
        assertEquals("Descr", imageData.contentDescription)
        assertEquals(100, imageData.height)
        assertEquals(200, imageData.width)
    }

    @Test
    fun `mapToData converts CardDto correctly`() {
        val imageDto = ScreenDto.ImageDto(
            index = 4,
            url = "url",
            height = 10,
            width = 10
        )
        val dto = listOf(
            ScreenDto.CardDto(
                index = 4,
                image = imageDto,
                title = "Card Title",
                subtitle = "Card Subtitle",
                backgroundColor = 0xFFFFFF
            )
        )
        val result = mapper.mapToData(dto)

        assertEquals(1, result.size)
        val cardData = result[0] as NetworkScreenData.CardDataNetwork
        assertEquals(4, cardData.index)
        assertEquals("Card Title", cardData.title)
        assertEquals("Card Subtitle", cardData.subtitle)
        assertEquals(0xFFFFFF, cardData.backgroundColor)
        assertEquals("url", cardData.image.url)
    }

    @Test
    fun `mapToData converts NavigationButtonDto correctly`() {
        val dto = listOf(
            ScreenDto.NavigationButtonDto(
                index = 5,
                label = "Go",
                destination = "dest",
                location = LocationType.EXTERNAL,
                sort = ButtonType.SECONDARY
            )
        )
        val result = mapper.mapToData(dto)

        assertEquals(1, result.size)
        val btnData = result[0] as NetworkScreenData.NavigationButtonDataNetwork
        assertEquals(5, btnData.index)
        assertEquals("Go", btnData.label)
        assertEquals("dest", btnData.destination)
        assertEquals(LocationTypeData.EXTERNAL, btnData.location)
        assertEquals(ButtonTypeData.SECONDARY, btnData.sort)
    }

    @Test
    fun `mapToData converts ActivityFeedDto correctly`() {
        val dto = listOf(ScreenDto.ActivityFeedDto(index = 5))
        val result = mapper.mapToData(dto)

        assertEquals(1, result.size)
        val feedData = result[0] as NetworkScreenData.ActivityFeedDataNetwork
        assertEquals(5, feedData.index)
    }

    @Test
    fun `mapToData converts TileRowDto correctly`() {
        val tile = ScreenDto.TileDto(
            index = 0,
            title = "T1",
            iconName = "I1",
            destination = "D1",
            subtitleType = "S1",
            sharedElementKey = "K1"
        )
        val dto = listOf(ScreenDto.TileRowDto(index = 6, tiles = listOf(tile)))
        val result = mapper.mapToData(dto)

        assertEquals(1, result.size)
        val tileRowData = result[0] as NetworkScreenData.TileRowDataNetwork
        assertEquals(6, tileRowData.index)
        assertEquals(1, tileRowData.tiles.size)
        assertEquals("T1", tileRowData.tiles[0].title)
        assertEquals("K1", tileRowData.tiles[0].sharedElementKey)
    }

    @Test
    fun `mapToData converts NavigationButtonDto with defaults correctly`() {
        val dto = listOf(
            ScreenDto.NavigationButtonDto(
                index = 5,
                label = "Go",
                destination = "dest",
                location = null,
                sort = null
            )
        )
        val result = mapper.mapToData(dto)

        val btnData = result[0] as NetworkScreenData.NavigationButtonDataNetwork
        assertEquals(LocationTypeData.INTERNAL, btnData.location) // Default behavior
        assertEquals(ButtonTypeData.PRIMARY, btnData.sort) // Default behavior
    }

    @Test
    fun `mapToData converts ImageDto with null contentDescription correctly`() {
        val dto = listOf(
            ScreenDto.ImageDto(index = 0, url = "url", contentDescription = null, height = 10, width = 10)
        )
        val result = mapper.mapToData(dto)
        val imageData = result[0] as NetworkScreenData.ImageDataNetwork
        assertNull(imageData.contentDescription)
    }

    @Test
    fun `mapToData converts CardDto with null backgroundColor correctly`() {
        val imageDto = ScreenDto.ImageDto(index = 0, url = "url", height = 10, width = 10)
        val dto = listOf(
            ScreenDto.CardDto(
                index = 0,
                image = imageDto,
                title = "T",
                subtitle = "S",
                backgroundColor = null
            )
        )
        val result = mapper.mapToData(dto)
        val cardData = result[0] as NetworkScreenData.CardDataNetwork
        assertNull(cardData.backgroundColor)
    }

    @Test
    fun `mapToData handles very long lists`() {
        val dto = List(100) { i -> ScreenDto.TitleDto(index = i, content = "T$i") }
        val result = mapper.mapToData(dto)
        assertEquals(100, result.size)
        assertEquals(99, result.last().index)
    }

    @Test
    fun `mapToData handles Unknown DTO gracefully`() {
        val dto = listOf(ScreenDto.Unknown(index = 99))
        val result = mapper.mapToData(dto)

        assertEquals(1, result.size)
        assertTrue(result[0] is NetworkScreenData.Unknown)
    }

    @Test
    fun `mapToData handles empty list`() {
        val result = mapper.mapToData(emptyList())
        assertTrue(result.isEmpty())
    }
}
