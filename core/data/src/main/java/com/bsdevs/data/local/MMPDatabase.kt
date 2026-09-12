package com.bsdevs.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.AutoMigration
import com.bsdevs.data.local.dao.*
import com.bsdevs.data.local.entities.*

@Database(
    entities = [
        ScreenEntity::class,
        UserEntity::class,
        BabyEntity::class,
        ShoppingItemEntity::class,
        BabyEventEntity::class,
        FormSchemaEntity::class,
        FormSubmissionEntity::class,
        CoffeeEntity::class,
        DynamicOptionsEntity::class
    ],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 2, to = 3)
    ],
    exportSchema = true
)
@TypeConverters(MMPTypeConverters::class)
abstract class MMPDatabase : RoomDatabase() {
    abstract fun screenDao(): ScreenDao
    abstract fun userBabyDao(): UserBabyDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun babyEventDao(): BabyEventDao
    abstract fun formDao(): FormDao
    abstract fun coffeeDao(): CoffeeDao
}
