package com.bsdevs.coffeescreen.network

import com.bsdevs.network.dto.CoffeeInputScreenDto
import com.bsdevs.network.FirestoreHolder
import com.bsdevs.common.FirebaseLogger
import com.bsdevs.network.dto.CoffeeDto
import com.bsdevs.network.dto.ShotDto
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoffeeApiServiceImpl @Inject constructor(
    private val firestoreHolder: FirestoreHolder
) : CoffeeApiService {

    private val firestore get() = firestoreHolder.firestore

    override suspend fun getCoffeeInputScreenData(): CoffeeInputScreenDto? {
        FirebaseLogger.logCall("Read Screen: coffeeInput")
        val documentSnapshot = firestore.collection("screens").document("coffeeInput").get().await()
        return documentSnapshot.toObject(CoffeeInputScreenDto::class.java)
    }

    override suspend fun uploadCoffee(userId: String, coffee: CoffeeDto) {
        val item = mapOf(
            "isDecaf" to coffee.isDecaf,
            "roastDate" to coffee.roastDate,
            "beanTypes" to coffee.beanTypes,
            "originCountries" to coffee.originCountries,
            "tastingNotes" to coffee.tastingNotes,
            "beanPreparationMethod" to coffee.beanPreparationMethod,
            "roaster" to coffee.roaster,
            "label" to coffee.label,
            "userId" to userId,
            "id" to coffee.id
        )
        val label = coffee.label ?: coffee.id ?: "unknown"
        FirebaseLogger.logCall("Write Coffee: $label")
        firestore.collection("coffeeUploads").document(label).set(item).await()
    }

    override suspend fun getCoffeeById(userId: String, coffeeId: String): CoffeeDto? {
        FirebaseLogger.logCall("Read Coffee By ID: $coffeeId")
        val snapshot = firestore.collection("coffeeUploads")
            .whereEqualTo("userId", userId)
            .whereEqualTo("id", coffeeId)
            .get()
            .await()
        return snapshot.toObjects(CoffeeDto::class.java).firstOrNull()
    }

    override suspend fun getAllCoffee(userId: String): List<CoffeeDto> {
        FirebaseLogger.logCall("Read All Coffee for user: $userId")
        val snapshot = firestore.collection("coffeeUploads")
            .whereEqualTo("userId", userId)
            .get()
            .await()
        return snapshot.toObjects(CoffeeDto::class.java)
    }

    override suspend fun getShotsForCoffee(coffeeLabel: String): List<ShotDto> {
        FirebaseLogger.logCall("Read Shots for Coffee: $coffeeLabel")
        val snapshot = firestore.collection("coffeeUploads")
            .document(coffeeLabel)
            .collection("shots")
            .get()
            .await()
        return snapshot.toObjects(ShotDto::class.java)
    }

    override suspend fun uploadShot(coffeeLabel: String, shot: ShotDto) {
        val shotId = shot.id ?: UUID.randomUUID().toString()
        FirebaseLogger.logCall("Write Shot: $shotId for $coffeeLabel")
        firestore.collection("coffeeUploads")
            .document(coffeeLabel)
            .collection("shots")
            .document(shotId)
            .set(shot)
            .await()
    }
}
