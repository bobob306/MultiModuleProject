package com.bsdevs.babycare.presentation.shopping

import app.cash.turbine.test
import com.bsdevs.network.dto.ShoppingListDto
import com.bsdevs.network.dto.TaskDto
import com.bsdevs.network.dto.UserDto
import com.bsdevs.data.repository.UserRepository
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
    private lateinit var taskRepository: FakeTaskRepository
    private lateinit var userRepository: UserRepository
    private lateinit var viewModel: ShoppingListViewModel
    private val userProfileFlow = MutableStateFlow<UserDto?>(null)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        shoppingRepository = FakeShoppingListRepository()
        taskRepository = FakeTaskRepository()
        userRepository = mockk(relaxed = true)
        every { userRepository.userProfile } returns userProfileFlow
        
        viewModel = ShoppingListViewModel(shoppingRepository, taskRepository, userRepository)
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
        assertEquals("b1", taskRepository.lastBabyId)
        assertEquals(1, taskRepository.startListeningCallCount)
    }

    @Test
    fun `stops listening when user logs out`() = runTest {
        userProfileFlow.value = UserDto(id = "u1", babyId = "b1")
        val initialStopCalls = shoppingRepository.stopListeningCallCount
        val initialTaskStopCalls = taskRepository.stopListeningCallCount
        userProfileFlow.value = null
        assertEquals(initialStopCalls + 1, shoppingRepository.stopListeningCallCount)
        assertEquals(initialTaskStopCalls + 1, taskRepository.stopListeningCallCount)
    }

    @Test
    fun `uiState reflects repository items`() = runTest {
        val items = listOf(ShoppingListDto(id = "1", name = "Diapers"))
        val tasks = listOf(TaskDto(id = "2", name = "Buy Socks"))
        shoppingRepository.emitItems(items)
        taskRepository.emitItems(tasks)
        
        viewModel.uiState.test {
            val state = awaitItem()
            if (state.items.isEmpty() || state.taskItems.isEmpty()) {
                val nextState = awaitItem()
                assertEquals(items, nextState.items)
                assertEquals(tasks, nextState.taskItems)
            } else {
                assertEquals(items, state.items)
                assertEquals(tasks, state.taskItems)
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
    fun `addItem calls shopping repository when shopping tab is selected`() = runTest {
        userProfileFlow.value = UserDto(id = "u1", babyId = "b1")
        viewModel.selectTab(ShoppingListTab.SHOPPING)
        viewModel.onNewItemNameChange("Milk")
        runCurrent()
        
        viewModel.addItem()
        runCurrent()
        
        assertEquals("Milk", shoppingRepository.shoppingList.value.first().name)
        assertEquals("", viewModel.uiState.value.newItemName)
    }

    @Test
    fun `addItem calls task repository when tasks tab is selected`() = runTest {
        userProfileFlow.value = UserDto(id = "u1", babyId = "b1")
        viewModel.selectTab(ShoppingListTab.TASKS)
        viewModel.onNewItemNameChange("Buy Socks")
        runCurrent()
        
        viewModel.addItem()
        runCurrent()
        
        assertEquals("Buy Socks", taskRepository.taskList.value.first().name)
        assertEquals("", viewModel.uiState.value.newItemName)
    }

    @Test
    fun `setEditingItem populates editing name for shopping item`() = runTest {
        val items = listOf(ShoppingListDto(id = "e1", name = "Edit Me"))
        shoppingRepository.emitItems(items)
        viewModel.selectTab(ShoppingListTab.SHOPPING)
        runCurrent()
        
        viewModel.setEditingItem("e1")
        runCurrent()
        
        assertEquals("e1", viewModel.uiState.value.editingItemId)
        assertEquals("Edit Me", viewModel.uiState.value.editingName)
    }

    @Test
    fun `setEditingItem populates editing name for task item`() = runTest {
        val tasks = listOf(TaskDto(id = "t1", name = "Edit Task"))
        taskRepository.emitItems(tasks)
        viewModel.selectTab(ShoppingListTab.TASKS)
        runCurrent()
        
        viewModel.setEditingItem("t1")
        runCurrent()
        
        assertEquals("t1", viewModel.uiState.value.editingItemId)
        assertEquals("Edit Task", viewModel.uiState.value.editingName)
    }

    @Test
    fun `saveEdit updates shopping repository and clears editing state`() = runTest {
        userProfileFlow.value = UserDto(id = "u1", babyId = "b1")
        val items = listOf(ShoppingListDto(id = "e1", name = "Old Name"))
        shoppingRepository.emitItems(items)
        viewModel.selectTab(ShoppingListTab.SHOPPING)
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
    fun `saveEdit updates task repository and clears editing state`() = runTest {
        userProfileFlow.value = UserDto(id = "u1", babyId = "b1")
        val tasks = listOf(TaskDto(id = "t1", name = "Old Task Name"))
        taskRepository.emitItems(tasks)
        viewModel.selectTab(ShoppingListTab.TASKS)
        runCurrent()
        
        viewModel.setEditingItem("t1")
        viewModel.onEditingNameChange("New Task Name")
        runCurrent()
        viewModel.saveEdit()
        runCurrent()
        
        assertEquals("New Task Name", taskRepository.taskList.value.first().name)
        assertNull(viewModel.uiState.value.editingItemId)
    }

    @Test
    fun `confirmDelete calls shopping repository and clears state`() = runTest {
        userProfileFlow.value = UserDto(id = "u1", babyId = "b1")
        shoppingRepository.emitItems(listOf(ShoppingListDto(id = "d1", name = "Delete Me")))
        viewModel.selectTab(ShoppingListTab.SHOPPING)
        
        viewModel.setDeletingItem("d1")
        viewModel.confirmDelete()
        
        assertEquals(0, shoppingRepository.shoppingList.value.size)
        assertNull(viewModel.uiState.value.deletingItemId)
    }

    @Test
    fun `confirmDelete calls task repository and clears state`() = runTest {
        userProfileFlow.value = UserDto(id = "u1", babyId = "b1")
        taskRepository.emitItems(listOf(TaskDto(id = "t1", name = "Delete Task")))
        viewModel.selectTab(ShoppingListTab.TASKS)
        
        viewModel.setDeletingItem("t1")
        viewModel.confirmDelete()
        
        assertEquals(0, taskRepository.taskList.value.size)
        assertNull(viewModel.uiState.value.deletingItemId)
    }
}
