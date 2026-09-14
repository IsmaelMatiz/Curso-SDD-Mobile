package com.aristidevs.cursopremiumandroid.domain.usecase

import com.aristidevs.cursopremiumandroid.domain.DogRepository
import com.aristidevs.cursopremiumandroid.domain.model.DogDetailModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetDogDetailUseCase @Inject constructor(private val repository: DogRepository) {

    operator fun invoke(id: Long): Flow<DogDetailModel?> = repository.observeDogDetail(id)
}
