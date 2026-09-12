package com.bsdevs.network

import com.bsdevs.network.dto.AppMetadataDto
import com.bsdevs.network.dto.FormSchemaDto
import com.bsdevs.network.dto.ScreenDto
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MetadataDtoMapperTest {

    private lateinit var screenMapper: ScreenDtoMapper
    private lateinit var formMapper: FormDtoMapper
    private lateinit var mapper: MetadataDtoMapperImpl

    @Before
    fun setUp() {
        screenMapper = mockk()
        formMapper = mockk()
        mapper = MetadataDtoMapperImpl(screenMapper, formMapper)
    }

    @Test
    fun `mapToDto maps screens and forms correctly`() {
        val rawScreens = mapOf("home" to listOf(mapOf("type" to "TITLE")))
        val rawForms = mapOf("login" to mapOf("title" to "Login"))
        val rawMap = mapOf(
            "screens" to rawScreens,
            "forms" to rawForms
        )

        val screenDtos = listOf(ScreenDto.TitleDto(0, "Home"))
        val formDto = FormSchemaDto(title = "Login")

        every { screenMapper.mapToDto(any()) } returns screenDtos
        every { formMapper.mapToDto(any()) } returns formDto

        val result = mapper.mapToDto(rawMap)

        assertEquals(1, result.screens.size)
        assertEquals(screenDtos, result.screens["home"])
        assertEquals(1, result.forms.size)
        assertEquals(formDto, result.forms["login"])
    }

    @Test
    fun `mapToFirebase maps to map correctly`() {
        val screenDtos = listOf(ScreenDto.TitleDto(0, "Home"))
        val formDto = FormSchemaDto(title = "Login")
        val metadata = AppMetadataDto(
            screens = mapOf("home" to screenDtos),
            forms = mapOf("login" to formDto)
        )

        every { screenMapper.mapToFirebase(screenDtos) } returns mapOf("components" to listOf(mapOf("type" to "TITLE")))
        
        val result = mapper.mapToFirebase(metadata)

        val screens = result["screens"] as Map<*, *>
        val forms = result["forms"] as Map<*, *>

        assertEquals(1, screens.size)
        assertEquals(1, forms.size)
        assertEquals("Login", (forms["login"] as Map<*, *>)["title"])
    }
}
