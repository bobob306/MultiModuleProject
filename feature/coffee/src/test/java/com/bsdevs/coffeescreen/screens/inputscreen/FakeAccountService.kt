package com.bsdevs.coffeescreen.screens.inputscreen

import com.bsdevs.authentication.AccountService
import com.bsdevs.authentication.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAccountService(
    override val currentUserId: String = "testUser"
) : AccountService {
    override val currentUser: Flow<User?> = MutableStateFlow(User(currentUserId))
    override fun hasUser(): Boolean = true
    override suspend fun signIn(email: String, password: String) {}
    override suspend fun signUp(email: String, password: String) {}
    override suspend fun signOut() {}
    override suspend fun deleteAccount() {}
}
