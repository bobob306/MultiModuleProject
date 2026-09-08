package com.bsdevs.coffeescreen.data

import com.bsdevs.coffeescreen.network.CoffeeApiService
import com.bsdevs.network.dto.CoffeeDto
import com.bsdevs.network.dto.ShotDto
import com.bsdevs.network.dto.CoffeeInputScreenDto
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.data.SyncManager
import com.bsdevs.data.Syncable
import com.bsdevs.data.local.dao.CoffeeDao
import com.bsdevs.data.local.entities.CoffeeEntity
import com.bsdevs.data.repository.Clearable
import com.bsdevs.data.repository.UserRepository
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface CoffeeRepository {
    val allCoffee: StateFlow<List<CoffeeDto>>
    suspend fun loadInitialData(userId: String)
    suspend fun uploadCoffee(userId: String, coffee: CoffeeDto)
    suspend fun getCoffeeById(userId: String, coffeeId: String): CoffeeDto?
    suspend fun getShotsForCoffee(coffeeLabel: String): List<ShotDto>
    suspend fun uploadShot(coffeeLabel: String, shot: ShotDto)
    suspend fun getCoffeeInputScreenData(): CoffeeInputScreenDto?
}

@Singleton
class CoffeeRepositoryImpl @Inject constructor(
    private val apiService: CoffeeApiService,
    private val dispatchers: DispatcherProvider,
    private val coffeeDao: CoffeeDao,
    private val userRepository: UserRepository,
    syncManager: SyncManager
) : CoffeeRepository, Clearable, Syncable {

    init {
        userRepository.registerClearable(this)
        syncManager.registerSyncable(this)
    }

    private val _allCoffee = MutableStateFlow<List<CoffeeDto>>(emptyList())
    override val allCoffee: StateFlow<List<CoffeeDto>> = _allCoffee.asStateFlow()

    override suspend fun sync() {
        val userId = userRepository.userProfile.value?.id ?: return
        val pending = coffeeDao.getPendingSync()
        for (entity in pending) {
            try {
                if (entity.isDeleted) {
                    apiService.uploadCoffee(userId, entity.coffee.copy(id = "DELETED_${entity.id}")) // Proxy for delete if API lacks it
                    coffeeDao.deleteById(entity.id)
                } else {
                    apiService.uploadCoffee(userId, entity.coffee)
                    coffeeDao.insertCoffee(listOf(entity.copy(isPendingSync = false)))
                }
            } catch (e: Exception) {
                Log.e("COFFEE_REPO", "Sync failed for coffee ${entity.id}", e)
            }
        }
    }

    override suspend fun loadInitialData(userId: String) = withContext(dispatchers.io) {
        // 📂 Load from DB first
        if (_allCoffee.value.isEmpty()) {
            val local = coffeeDao.getCoffeeLogs(userId).first()
            if (local.isNotEmpty()) {
                _allCoffee.value = local.map { it.coffee }.sortedByDescending { it.roastDate }
            }
        }

        try {
            val coffee = apiService.getAllCoffee(userId)
            val sorted = coffee.sortedByDescending { it.roastDate }
            _allCoffee.value = sorted
            
            // Sync to Local DB
            val entities = sorted.map { CoffeeEntity(it.id ?: "", userId, it) }
            coffeeDao.insertCoffee(entities)
        } catch (e: Exception) {
            Log.e("COFFEE_REPO", "Failed to fetch coffee from network", e)
        }
        Unit
    }

    override suspend fun uploadCoffee(userId: String, coffee: CoffeeDto) = withContext(dispatchers.io) {
        val coffeeId = coffee.id ?: ""
        
        // 1. 📂 Save to Local DB with pending sync flag
        val entity = CoffeeEntity(coffeeId, userId, coffee, isPendingSync = true)
        coffeeDao.insertCoffee(listOf(entity))

        // 2. 🧠 Update in-memory cache IMMEDIATELY
        _allCoffee.value = (listOf(coffee) + _allCoffee.value.filter { it.id != coffeeId })
            .sortedByDescending { it.roastDate }

        // 3. 🌐 Background sync attempt
        try {
            apiService.uploadCoffee(userId, coffee)
            coffeeDao.insertCoffee(listOf(entity.copy(isPendingSync = false)))
        } catch (e: Exception) {
            Log.e("COFFEE_REPO", "Network upload failed, will retry later", e)
        }
        Unit
    }

    override suspend fun getCoffeeById(userId: String, coffeeId: String): CoffeeDto? = withContext(dispatchers.io) {
        _allCoffee.value.firstOrNull { it.id == coffeeId } ?: run {
            apiService.getCoffeeById(userId, coffeeId)
        }
    }

    override suspend fun getShotsForCoffee(coffeeLabel: String): List<ShotDto> = withContext(dispatchers.io) {
        apiService.getShotsForCoffee(coffeeLabel)
    }

    override suspend fun uploadShot(coffeeLabel: String, shot: ShotDto) = withContext(dispatchers.io) {
        apiService.uploadShot(coffeeLabel, shot)
    }

    override suspend fun getCoffeeInputScreenData(): CoffeeInputScreenDto? = withContext(dispatchers.io) {
        apiService.getCoffeeInputScreenData()
    }

    override fun clearCache() {
        _allCoffee.value = emptyList()
    }
}
