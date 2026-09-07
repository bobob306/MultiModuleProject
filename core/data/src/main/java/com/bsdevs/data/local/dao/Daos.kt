package com.bsdevs.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bsdevs.data.local.entities.BabyEntity
import com.bsdevs.data.local.entities.BabyEventEntity
import com.bsdevs.data.local.entities.FormSchemaEntity
import com.bsdevs.data.local.entities.FormSubmissionEntity
import com.bsdevs.data.local.entities.ScreenEntity
import com.bsdevs.data.local.entities.ShoppingItemEntity
import com.bsdevs.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenDao {
    @Query("SELECT * FROM screens WHERE screenId = :screenId")
    suspend fun getScreen(screenId: String): ScreenEntity?

    @Query("SELECT * FROM screens WHERE screenId = :screenId")
    fun getScreenFlow(screenId: String): Flow<ScreenEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreen(screen: ScreenEntity)

    @Query("DELETE FROM screens WHERE screenId = :screenId")
    suspend fun deleteScreen(screenId: String)

    @Query("DELETE FROM screens")
    suspend fun clearAll()
}

@Dao
interface UserBabyDao {
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUser(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserFlow(userId: String): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM babies WHERE id = :babyId")
    suspend fun getBaby(babyId: String): BabyEntity?

    @Query("SELECT * FROM babies WHERE id = :babyId")
    fun getBabyFlow(babyId: String): Flow<BabyEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBaby(baby: BabyEntity)

    @Query("DELETE FROM users")
    suspend fun clearUsers()

    @Query("DELETE FROM babies")
    suspend fun clearBabies()
}

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_items WHERE babyId = :babyId AND isDeleted = 0")
    fun getShoppingItems(babyId: String): Flow<List<ShoppingItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ShoppingItemEntity>)

    @Query("UPDATE shopping_items SET isDeleted = 1, isPendingSync = 1 WHERE id = :itemId")
    suspend fun markDeleted(itemId: String)

    @Query("SELECT * FROM shopping_items WHERE isPendingSync = 1")
    suspend fun getPendingSync(): List<ShoppingItemEntity>

    @Query("DELETE FROM shopping_items WHERE id = :itemId")
    suspend fun deleteById(itemId: String)

    @Query("DELETE FROM shopping_items")
    suspend fun clearAll()
}

@Dao
interface BabyEventDao {
    @Query("SELECT * FROM baby_events WHERE babyId = :babyId AND isDeleted = 0 ORDER BY date DESC")
    fun getEvents(babyId: String): Flow<List<BabyEventEntity>>

    @Query("SELECT * FROM baby_events WHERE babyId = :babyId AND date = :date AND isDeleted = 0")
    suspend fun getEventsForDate(babyId: String, date: String): List<BabyEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<BabyEventEntity>)

    @Query("UPDATE baby_events SET isDeleted = 1, isPendingSync = 1 WHERE id = :eventId")
    suspend fun markDeleted(eventId: String)

    @Query("SELECT * FROM baby_events WHERE isPendingSync = 1")
    suspend fun getPendingSync(): List<BabyEventEntity>

    @Query("DELETE FROM baby_events WHERE id = :eventId")
    suspend fun deleteById(eventId: String)
    
    @Query("DELETE FROM baby_events")
    suspend fun clearAll()
}

@Dao
interface FormDao {
    @Query("SELECT * FROM form_schemas WHERE formId = :formId")
    suspend fun getSchema(formId: String): FormSchemaEntity?

    @Query("SELECT * FROM form_schemas WHERE formId = :formId")
    fun getSchemaFlow(formId: String): Flow<FormSchemaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchema(schema: FormSchemaEntity)

    @Query("SELECT * FROM form_submissions WHERE userId = :userId AND formId = :formId")
    suspend fun getSubmission(userId: String, formId: String): FormSubmissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmission(submission: FormSubmissionEntity)

    @Query("SELECT * FROM form_submissions WHERE isPendingSync = 1")
    suspend fun getPendingSubmissions(): List<FormSubmissionEntity>

    @Query("DELETE FROM form_schemas")
    suspend fun clearAll()
}
