package com.bsdevs.babycare.domain

import com.bsdevs.network.dto.ShoppingListDto
import kotlinx.coroutines.flow.StateFlow

interface ShoppingListRepository {
    val shoppingList: StateFlow<List<ShoppingListDto>>
    suspend fun startListening(babyId: String)
    suspend fun stopListening()
    suspend fun addShoppingItem(babyId: String, item: ShoppingListDto)
    suspend fun updateShoppingItem(babyId: String, item: ShoppingListDto)
    suspend fun deleteShoppingItem(babyId: String, itemId: String)
}
