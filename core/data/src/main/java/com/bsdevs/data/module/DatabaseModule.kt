package com.bsdevs.data.module

import android.content.Context
import androidx.room.Room
import com.bsdevs.data.local.MMPDatabase
import com.bsdevs.data.local.dao.BabyEventDao
import com.bsdevs.data.local.dao.ScreenDao
import com.bsdevs.data.local.dao.ShoppingDao
import com.bsdevs.data.local.dao.UserBabyDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MMPDatabase {
        return Room.databaseBuilder(
            context,
            MMPDatabase::class.java,
            "mmp_database"
        ).build()
    }

    @Provides
    fun provideScreenDao(db: MMPDatabase): ScreenDao = db.screenDao()

    @Provides
    fun provideUserBabyDao(db: MMPDatabase): UserBabyDao = db.userBabyDao()

    @Provides
    fun provideShoppingDao(db: MMPDatabase): ShoppingDao = db.shoppingDao()

    @Provides
    fun provideBabyEventDao(db: MMPDatabase): BabyEventDao = db.babyEventDao()
}
