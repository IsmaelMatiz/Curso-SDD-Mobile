package com.aristidevs.cursopremiumandroid.core.di

import com.aristidevs.cursopremiumandroid.data.DogRepositoryImpl
import com.aristidevs.cursopremiumandroid.domain.DogRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDogRepository(impl: DogRepositoryImpl): DogRepository
}
