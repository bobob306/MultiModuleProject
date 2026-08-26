package com.bsdevs.coffeescreen.network

import com.bsdevs.coffeescreen.screens.inputscreen.CoffeeInputScreenDto
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
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
}
