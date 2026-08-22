package com.bsdevs.babycare.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.domain.BabyCareRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed interface MigrationState {
    object Idle : MigrationState
    object Loading : MigrationState
    data class Success(val processedCount: Int) : MigrationState
    data class Error(val message: String) : MigrationState
}

@HiltViewModel
class BabyGraphViewModel @Inject constructor(
    private val repository: BabyCareRepository,
    private val accountService: AccountService
) : ViewModel() {

}