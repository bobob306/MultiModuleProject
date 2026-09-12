package com.bsdevs.authentication

import com.google.firebase.auth.FirebaseAuth
import com.bsdevs.common.FirebaseLogger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AccountServiceImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AccountService {

    override val currentUser: Flow<User?>
        get() = callbackFlow {
            FirebaseLogger.logCall("Listen Auth State")
            val listener =
                FirebaseAuth.AuthStateListener { auth ->
                    this.trySend(auth.currentUser?.let { User(it.uid) })
                }
            auth.addAuthStateListener(listener)
            awaitClose { auth.removeAuthStateListener(listener) }
        }

    override val currentUserId: String
        get() = auth.currentUser?.uid.orEmpty()

    override fun hasUser(): Boolean {
        return auth.currentUser != null
    }

    override suspend fun signIn(email: String, password: String) {
        FirebaseLogger.logCall("Auth SignIn: $email")
        auth.signInWithEmailAndPassword(email, password).await()
    }

    override suspend fun signUp(email: String, password: String) {
        FirebaseLogger.logCall("Auth SignUp: $email")
        auth.createUserWithEmailAndPassword(email, password).await()
    }

    override suspend fun signOut() {
        FirebaseLogger.logCall("Auth SignOut")
        auth.signOut()
    }

    override suspend fun deleteAccount() {
        FirebaseLogger.logCall("Auth Delete Account")
        auth.currentUser!!.delete().await()
    }
}
