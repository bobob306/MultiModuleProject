package com.bsdevs.babycare.presentation.shopping

import com.bsdevs.babycare.domain.ShoppingListRepository
import com.bsdevs.network.dto.ShoppingListDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeShoppingListRepository : ShoppingListRepository {
    private val _shoppingList = MutableStateFlow<List<ShoppingListDto>>(emptyList())
    override val shoppingList: StateFlow<List<ShoppingListDto>> = _shoppingList.asStateFlow()

    var startListeningCallCount = 0
    var stopListeningCallCount = 0
    var lastBabyId: String? = null

    override suspend fun startListening(babyId: String) {
        startListeningCallCount++
        lastBabyId = babyId
    }

    override suspend fun stopListening() {
        stopListeningCallCount++
    }

    override suspend fun addShoppingItem(babyId: String, item: ShoppingListDto) {
        _shoppingList.value = _shoppingList.value + item.copy(id = "new_id")
    }

    override suspend fun updateShoppingItem(babyId: String, item: ShoppingListDto) {
        _shoppingList.value = _shoppingList.value.map { if (it.id == item.id) item else it }
    }

    override suspend fun deleteShoppingItem(babyId: String, itemId: String) {
        _shoppingList.value = _shoppingList.value.filterNot { it.id == itemId }
    }

    fun emitItems(items: List<ShoppingListDto>) {
        _shoppingList.value = items
    }
}
