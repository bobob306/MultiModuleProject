package com.bsdevs.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bsdevs.data.local.dao.BabyEventDao
import com.bsdevs.data.local.dao.ScreenDao
import com.bsdevs.data.local.dao.ShoppingDao
import com.bsdevs.data.local.dao.UserBabyDao
import com.bsdevs.data.local.entities.BabyEntity
import com.bsdevs.data.local.entities.BabyEventEntity
import com.bsdevs.data.local.entities.ScreenEntity
import com.bsdevs.data.local.entities.ShoppingItemEntity
import com.bsdevs.data.local.entities.UserEntity

@Database(
    entities = [
        ScreenEntity::class,
        UserEntity::class,
        BabyEntity::class,
        ShoppingItemEntity::class,
        BabyEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(MMPTypeConverters::class)
abstract class MMPDatabase : RoomDatabase() {
    abstract fun screenDao(): ScreenDao
    abstract fun userBabyDao(): UserBabyDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun babyEventDao(): BabyEventDao
}
