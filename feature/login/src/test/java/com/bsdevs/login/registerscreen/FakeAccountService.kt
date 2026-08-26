package com.bsdevs.login.registerscreen

import com.bsdevs.authentication.AccountService
import com.bsdevs.authentication.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAccountService : AccountService {
    override var currentUserId: String = ""
    override val currentUser: Flow<User?> = MutableStateFlow(null)

    var shouldSucceed: Boolean = true
    var lastSignedUpEmail: String? = null
    var signUpCallCount = 0

    override fun hasUser(): Boolean = currentUserId.isNotEmpty()

    override suspend fun signIn(email: String, password: String) {}

    override suspend fun signUp(email: String, password: String) {
        delay(1)
        signUpCallCount++
        if (!shouldSucceed) throw RuntimeException("Registration failed")
        lastSignedUpEmail = email
        currentUserId = "fake_uid_reg"
    }

    override suspend fun signOut() {
        currentUserId = ""
    }

    override suspend fun deleteAccount() {}
}
