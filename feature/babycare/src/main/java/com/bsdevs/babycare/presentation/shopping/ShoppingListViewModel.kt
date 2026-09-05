package com.bsdevs.babycare.presentation.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.babycare.domain.ShoppingListRepository
import com.bsdevs.network.dto.ShoppingListDto
import com.bsdevs.network.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShoppingListUiState(
    val items: List<ShoppingListDto> = emptyList(),
    val newItemName: String = "",
    val editingItemId: String? = null,
    val editingName: String = "",
    val deletingItemId: String? = null
) {
    val itemToEdit: ShoppingListDto? = items.find { it.id == editingItemId }
    val itemToDelete: ShoppingListDto? = items.find { it.id == deletingItemId }
}

@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val shoppingListRepository: ShoppingListRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingListUiState())
    val uiState: StateFlow<ShoppingListUiState> = _uiState.asStateFlow()

    init {
        // Sync with repository data
        viewModelScope.launch {
            shoppingListRepository.shoppingList.collect { remoteItems ->
                _uiState.update { it.copy(items = remoteItems) }
            }
        }

        // Manage listeners based on babyId
        viewModelScope.launch {
            userRepository.userProfile.collectLatest { user ->
                val babyId = user?.babyId
                if (babyId != null) {
                    shoppingListRepository.startListening(babyId)
                } else {
                    shoppingListRepository.stopListening()
                }
            }
        }
    }

    fun onNewItemNameChange(name: String) {
        _uiState.update { it.copy(newItemName = name) }
    }

    fun onEditingNameChange(name: String) {
        _uiState.update { it.copy(editingName = name) }
    }

    fun setEditingItem(itemId: String?) {
        val name = uiState.value.items.find { it.id == itemId }?.name ?: ""
        _uiState.update { it.copy(editingItemId = itemId, editingName = name) }
    }

    fun addItem() {
        val name = uiState.value.newItemName
        if (name.isBlank()) return
        
        val babyId = userRepository.userProfile.value?.babyId ?: return
        viewModelScope.launch {
            shoppingListRepository.addShoppingItem(babyId, ShoppingListDto(name = name))
            _uiState.update { it.copy(newItemName = "") }
        }
    }

    fun saveEdit() {
        val babyId = userRepository.userProfile.value?.babyId ?: return
        val item = uiState.value.itemToEdit ?: return
        val newName = uiState.value.editingName
        
        viewModelScope.launch {
            shoppingListRepository.updateShoppingItem(babyId, item.copy(name = newName))
            setEditingItem(null)
        }
    }

    fun setDeletingItem(itemId: String?) {
        _uiState.update { it.copy(deletingItemId = itemId) }
    }

    fun confirmDelete() {
        val itemId = uiState.value.deletingItemId ?: return
        val babyId = userRepository.userProfile.value?.babyId ?: return
        viewModelScope.launch {
            shoppingListRepository.deleteShoppingItem(babyId, itemId)
            setDeletingItem(null)
        }
    }

    fun deleteItem(itemId: String) {
        val babyId = userRepository.userProfile.value?.babyId ?: return
        viewModelScope.launch {
            shoppingListRepository.deleteShoppingItem(babyId, itemId)
        }
    }
}
