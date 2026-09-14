package com.aristidevs.cursopremiumandroid.core.di

import android.content.Context
import androidx.room.Room
import com.aristidevs.cursopremiumandroid.data.image.DogImageStore
import com.aristidevs.cursopremiumandroid.data.local.DogDao
import com.aristidevs.cursopremiumandroid.data.local.DogDatabase
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
    fun provideDogDatabase(@ApplicationContext context: Context): DogDatabase {
        return Room.databaseBuilder(context, DogDatabase::class.java, "dogs.db").build()
    }

    @Provides
    @Singleton
    fun provideDogDao(database: DogDatabase): DogDao = database.dogDao()

    @Provides
    @Singleton
    fun provideDogImageStore(@ApplicationContext context: Context): DogImageStore {
        return DogImageStore(context)
    }
}
