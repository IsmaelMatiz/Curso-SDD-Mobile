package com.aristidevs.cursopremiumandroid.domain

import com.aristidevs.cursopremiumandroid.domain.model.CatalogRefreshResult
import com.aristidevs.cursopremiumandroid.domain.model.Dog
import com.aristidevs.cursopremiumandroid.domain.model.DogDetailModel
import com.aristidevs.cursopremiumandroid.domain.model.NewDog
import kotlinx.coroutines.flow.Flow

interface DogRepository {

    /** The single source of truth for the UI: local storage, remote dogs and own dogs alike. */
    fun observeDogs(): Flow<List<Dog>>

    fun observeDogDetail(id: Long): Flow<DogDetailModel?>

    /**
     * Downloads the full remote catalog (list and every detail) and replaces
     * the remote block in storage in one transaction. Never touches own dogs.
     */
    suspend fun refreshCatalog(): CatalogRefreshResult

    /** Persists a new own dog, copying its photo, and returns its local id. */
    suspend fun addDog(newDog: NewDog): Long
}
