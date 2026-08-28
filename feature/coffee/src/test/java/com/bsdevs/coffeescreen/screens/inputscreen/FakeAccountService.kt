package com.bsdevs.coffeescreen.screens.inputscreen

import com.bsdevs.authentication.AccountService
import com.bsdevs.authentication.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAccountService(
    userId: String = "testUser"
) : AccountService {
    private val _currentUser = MutableStateFlow<User?>(User(userId))
    override val currentUser: Flow<User?> = _currentUser.asStateFlow()
    
    override var currentUserId: String = userId
        get() {
            if (field.isEmpty()) throw RuntimeException("No user")
            return field
        }

    override fun hasUser(): Boolean = currentUserId.isNotEmpty()

    override suspend fun signIn(email: String, password: String) {
        currentUserId = "fake_uid"
        _currentUser.value = User(currentUserId)
    }

    override suspend fun signUp(email: String, password: String) {
        currentUserId = "fake_uid"
        _currentUser.value = User(currentUserId)
    }

    override suspend fun signOut() {
        currentUserId = ""
        _currentUser.value = null
    }

    override suspend fun deleteAccount() {
        currentUserId = ""
        _currentUser.value = null
    }
}
