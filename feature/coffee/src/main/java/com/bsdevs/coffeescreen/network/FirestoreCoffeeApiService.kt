package com.bsdevs.coffeescreen.network

import com.bsdevs.coffeescreen.network.CoffeeDto
import com.bsdevs.coffeescreen.screens.detailscreen.ShotDto
import com.bsdevs.coffeescreen.screens.inputscreen.CoffeeInputScreenDto
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreCoffeeApiService @Inject constructor(
    private val firestore: FirebaseFirestore
) : CoffeeApiService {

    override suspend fun getCoffeeInputScreenData(): CoffeeInputScreenDto? {
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
        firestore.collection("coffeeUploads").document(label).set(item).await()
    }

    override suspend fun getCoffeeById(userId: String, coffeeId: String): CoffeeDto? {
        val snapshot = firestore.collection("coffeeUploads")
            .whereEqualTo("userId", userId)
            .whereEqualTo("id", coffeeId)
            .get()
            .await()
        return snapshot.toObjects(CoffeeDto::class.java).firstOrNull()
    }

    override suspend fun getShotsForCoffee(coffeeLabel: String): List<ShotDto> {
        val snapshot = firestore.collection("coffeeUploads")
            .document(coffeeLabel)
            .collection("shots")
            .get()
            .await()
        return snapshot.toObjects(ShotDto::class.java)
    }

    override suspend fun uploadShot(coffeeLabel: String, shot: ShotDto) {
        val shotId = shot.id ?: UUID.randomUUID().toString()
        firestore.collection("coffeeUploads")
            .document(coffeeLabel)
            .collection("shots")
            .document(shotId)
            .set(shot)
            .await()
    }
}
