package com.bsdevs.homescreen

import app.cash.turbine.test
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.common.result.Result
import com.bsdevs.data.ScreenDataMapperImpl
import com.bsdevs.network.dto.ScreenDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeScreenRepository
    private lateinit var formRepository: FakeFormRepository
    private lateinit var mapper: ScreenDataMapperImpl
    private lateinit var viewModel: HomeScreenViewModel
    private lateinit var dispatchers: DispatcherProvider

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        dispatchers = object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

        repository = FakeScreenRepository()
        formRepository = FakeFormRepository()
        mapper = ScreenDataMapperImpl()
        viewModel = HomeScreenViewModel(repository, formRepository, mapper, dispatchers)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() = runTest {
        assertEquals(Result.Loading, viewModel.viewData.value)
    }

    @Test
    fun `viewData updates to Success when repository emits data`() = runTest {
        // Given
        val screenDtoList = listOf(
            ScreenDto.TitleDto(index = 0, content = "Welcome Home")
        )
        
        viewModel.viewData.test {
            assertEquals(Result.Loading, awaitItem())

            // When
            repository.emitScreenData("home", screenDtoList)

            // Then
            val state = awaitItem()
            assertTrue(state is Result.Success)
            val successData = (state as Result.Success).data
            assertEquals(1, successData.size)
            assertEquals("Welcome Home", (successData[0] as com.bsdevs.data.NetworkScreenData.TitleDataNetwork).content)
        }
    }

    @Test
    fun `viewData updates to Error when repository emits error`() = runTest {
        // Given
        val exception = Exception("Network Error")

        viewModel.viewData.test {
            assertEquals(Result.Loading, awaitItem())

            // When
            repository.emitError("home", exception)

            // Then
            val state = awaitItem()
            assertTrue(state is Result.Error)
            assertEquals("Network Error", (state as Result.Error).exception.message)
        }
    }

    @Test
    fun `viewData handles all component types correctly`() = runTest {
        // Given a complex screen definition
        val complexList = listOf(
            ScreenDto.TitleDto(index = 0, content = "Title"),
            ScreenDto.SubtitleDto(index = 1, content = "Subtitle"),
            ScreenDto.SpacerDto(index = 2, size = com.bsdevs.network.dto.SizeDto(size = 16, type = com.bsdevs.network.dto.SpacerType.HEIGHT)),
            ScreenDto.ImageDto(index = 3, url = "url", height = 100, width = 100),
            ScreenDto.CardDto(
                index = 4, 
                title = "Card", 
                subtitle = "Sub", 
                image = ScreenDto.ImageDto(index = 0, url = "u", height = 10, width = 10),
                backgroundColor = 0
            ),
            ScreenDto.NavigationButtonDto(
                index = 5, 
                label = "Go", 
                destination = "dest", 
                location = com.bsdevs.network.dto.LocationType.INTERNAL, 
                sort = com.bsdevs.network.dto.ButtonType.PRIMARY
            )
        )

        viewModel.viewData.test {
            assertEquals(Result.Loading, awaitItem())

            // When
            repository.emitScreenData("home", complexList)

            // Then
            val state = awaitItem() as Result.Success
            assertEquals(6, state.data.size)
            assertTrue(state.data[0] is com.bsdevs.data.NetworkScreenData.TitleDataNetwork)
            assertTrue(state.data[2] is com.bsdevs.data.NetworkScreenData.SpacerDataNetwork)
            assertTrue(state.data[5] is com.bsdevs.data.NetworkScreenData.NavigationButtonDataNetwork)
        }
    }

    @Test
    fun `manual refresh triggers state transition`() = runTest {
        viewModel.viewData.test {
            assertEquals(Result.Loading, awaitItem())

            // 1. First emission
            repository.emitScreenData("home", listOf(ScreenDto.TitleDto(0, "First")))
            assertTrue(awaitItem() is Result.Success)

            // 2. Manual refresh (simulate loading state from repo)
            repository.emitLoading("home")
            assertEquals(Result.Loading, awaitItem())

            // 3. Second emission
            repository.emitScreenData("home", listOf(ScreenDto.TitleDto(0, "Second")))
            val finalState = awaitItem() as Result.Success
            assertEquals("Second", (finalState.data[0] as com.bsdevs.data.NetworkScreenData.TitleDataNetwork).content)
        }
    }
}
