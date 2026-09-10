package com.bsdevs.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bsdevs.network.dto.BabyDto
import com.bsdevs.network.dto.FormSchemaDto
import com.bsdevs.network.dto.FormSubmissionDto
import com.bsdevs.network.dto.ScreenDto
import com.bsdevs.network.dto.ShoppingListDto
import com.bsdevs.network.dto.UserDto
import com.bsdevs.network.dto.BabyEvent
import com.bsdevs.network.dto.CoffeeDto

@Entity(tableName = "screens")
data class ScreenEntity(
    @PrimaryKey val screenId: String,
    val components: List<ScreenDto>,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val profile: UserDto,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "babies")
data class BabyEntity(
    @PrimaryKey val id: String,
    val data: BabyDto,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey val id: String,
    val babyId: String,
    val item: ShoppingListDto,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isPendingSync: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(tableName = "baby_events")
data class BabyEventEntity(
    @PrimaryKey val id: String,
    val babyId: String,
    val date: String,
    val event: BabyEvent,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isPendingSync: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(tableName = "form_schemas")
data class FormSchemaEntity(
    @PrimaryKey val formId: String,
    val schema: FormSchemaDto,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "form_submissions")
data class FormSubmissionEntity(
    @PrimaryKey val id: String, // userId + "_" + formId
    val userId: String,
    val formId: String,
    val submission: FormSubmissionDto,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isPendingSync: Boolean = false
)

@Entity(tableName = "coffee_logs")
data class CoffeeEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val coffee: CoffeeDto,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isPendingSync: Boolean = false,
    val isDeleted: Boolean = false
)
