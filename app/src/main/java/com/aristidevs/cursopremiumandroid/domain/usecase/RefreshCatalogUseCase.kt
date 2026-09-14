package com.aristidevs.cursopremiumandroid.domain.usecase

import com.aristidevs.cursopremiumandroid.domain.DogRepository
import com.aristidevs.cursopremiumandroid.domain.model.CatalogRefreshResult
import javax.inject.Inject

class RefreshCatalogUseCase @Inject constructor(private val repository: DogRepository) {

    suspend operator fun invoke(): CatalogRefreshResult = repository.refreshCatalog()
}
