package com.bsdevs.babycare.presentation.shopping

import app.cash.turbine.test
import com.bsdevs.network.dto.ShoppingListDto
import com.bsdevs.network.dto.UserDto
import com.bsdevs.network.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var shoppingRepository: FakeShoppingListRepository
    private lateinit var userRepository: UserRepository
    private lateinit var viewModel: ShoppingListViewModel
    private val userProfileFlow = MutableStateFlow<UserDto?>(null)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        shoppingRepository = FakeShoppingListRepository()
        userRepository = mockk(relaxed = true)
        every { userRepository.userProfile } returns userProfileFlow
        
        viewModel = ShoppingListViewModel(shoppingRepository, userRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts listening when babyId is available`() = runTest {
        userProfileFlow.value = UserDto(id = "u1", babyId = "b1")
        assertEquals("b1", shoppingRepository.lastBabyId)
        assertEquals(1, shoppingRepository.startListeningCallCount)
    }

    @Test
    fun `stops listening when user logs out`() = runTest {
        userProfileFlow.value = UserDto(id = "u1", babyId = "b1")
        val initialStopCalls = shoppingRepository.stopListeningCallCount
        userProfileFlow.value = null
        assertEquals(initialStopCalls + 1, shoppingRepository.stopListeningCallCount)
    }

    @Test
    fun `uiState reflects repository items`() = runTest {
        val items = listOf(ShoppingListDto(id = "1", name = "Diapers"))
        shoppingRepository.emitItems(items)
        
        viewModel.uiState.test {
            // Skip initial default state if needed, or wait for the items
            val first = awaitItem()
            if (first.items.isEmpty()) {
                assertEquals(items, awaitItem().items)
            } else {
                assertEquals(items, first.items)
            }
        }
    }

    @Test
    fun `onNewItemNameChange updates uiState`() = runTest {
        viewModel.onNewItemNameChange("Milk")
        runCurrent()
        assertEquals("Milk", viewModel.uiState.value.newItemName)
    }

    @Test
    fun `addItem calls repository with current babyId and clears input`() = runTest {
        userProfileFlow.value = UserDto(id = "u1", babyId = "b1")
        viewModel.onNewItemNameChange("Milk")
        runCurrent()
        
        viewModel.addItem()
        runCurrent()
        
        assertEquals("Milk", shoppingRepository.shoppingList.value.first().name)
        assertEquals("", viewModel.uiState.value.newItemName)
    }

    @Test
    fun `setEditingItem populates editing name`() = runTest {
        val items = listOf(ShoppingListDto(id = "e1", name = "Edit Me"))
        shoppingRepository.emitItems(items)
        runCurrent()
        
        viewModel.setEditingItem("e1")
        runCurrent()
        
        assertEquals("e1", viewModel.uiState.value.editingItemId)
        assertEquals("Edit Me", viewModel.uiState.value.editingName)
    }

    @Test
    fun `saveEdit updates repository and clears editing state`() = runTest {
        userProfileFlow.value = UserDto(id = "u1", babyId = "b1")
        val items = listOf(ShoppingListDto(id = "e1", name = "Old Name"))
        shoppingRepository.emitItems(items)
        runCurrent()
        
        viewModel.setEditingItem("e1")
        viewModel.onEditingNameChange("New Name")
        runCurrent()
        viewModel.saveEdit()
        runCurrent()
        
        assertEquals("New Name", shoppingRepository.shoppingList.value.first().name)
        assertNull(viewModel.uiState.value.editingItemId)
    }

    @Test
    fun `setDeletingItem updates uiState`() = runTest {
        viewModel.setDeletingItem("d1")
        assertEquals("d1", viewModel.uiState.value.deletingItemId)
    }

    @Test
    fun `confirmDelete calls repository and clears state`() = runTest {
        userProfileFlow.value = UserDto(id = "u1", babyId = "b1")
        shoppingRepository.emitItems(listOf(ShoppingListDto(id = "d1", name = "Delete Me")))
        
        viewModel.setDeletingItem("d1")
        viewModel.confirmDelete()
        
        assertEquals(0, shoppingRepository.shoppingList.value.size)
        assertNull(viewModel.uiState.value.deletingItemId)
    }

    @Test
    fun `deleteItem calls repository`() = runTest {
        userProfileFlow.value = UserDto(id = "u1", babyId = "b1")
        shoppingRepository.emitItems(listOf(ShoppingListDto(id = "d1", name = "Delete Me")))
        
        viewModel.deleteItem("d1")
        
        assertEquals(0, shoppingRepository.shoppingList.value.size)
    }
}
