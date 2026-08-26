package com.bsdevs.login.loginscreen

import com.bsdevs.authentication.AccountService
import com.bsdevs.authentication.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAccountService : AccountService {
    override var currentUserId: String = ""
    override val currentUser: Flow<User?> = MutableStateFlow(null)
    
    var shouldSucceed: Boolean = true
    var lastSignedInEmail: String? = null

    override fun hasUser(): Boolean = currentUserId.isNotEmpty()

    override suspend fun signIn(email: String, password: String) {
        kotlinx.coroutines.delay(1)
        if (!shouldSucceed) throw RuntimeException("Auth failed")
        lastSignedInEmail = email
        currentUserId = "fake_uid"
    }

    override suspend fun signUp(email: String, password: String) {}
    override suspend fun signOut() {
        currentUserId = ""
    }
    override suspend fun deleteAccount() {}
}
