package com.aristidevs.cursopremiumandroid.fakes

import com.aristidevs.cursopremiumandroid.domain.DogRepository
import com.aristidevs.cursopremiumandroid.domain.model.CatalogRefreshResult
import com.aristidevs.cursopremiumandroid.domain.model.Dog
import com.aristidevs.cursopremiumandroid.domain.model.DogDetailModel
import com.aristidevs.cursopremiumandroid.domain.model.NewDog
import kotlinx.coroutines.flow.MutableStateFlow

class FakeDogRepository : DogRepository {

    val dogs = MutableStateFlow<List<Dog>>(emptyList())
    val detail = MutableStateFlow<DogDetailModel?>(null)
    var refreshResult: CatalogRefreshResult = CatalogRefreshResult.Success
    var refreshCallCount: Int = 0

    val addedDogs = mutableListOf<NewDog>()
    var nextAddedId: Long = 1L

    override fun observeDogs() = dogs

    override fun observeDogDetail(id: Long) = detail

    override suspend fun refreshCatalog(): CatalogRefreshResult {
        refreshCallCount++
        return refreshResult
    }

    override suspend fun addDog(newDog: NewDog): Long {
        addedDogs += newDog
        return nextAddedId++
    }
}
