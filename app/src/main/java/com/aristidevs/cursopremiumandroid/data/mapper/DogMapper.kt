package com.aristidevs.cursopremiumandroid.data.mapper

import com.aristidevs.cursopremiumandroid.core.di.DogApiConfig.BASE_URL
import com.aristidevs.cursopremiumandroid.data.api.response.DogDetailResponse
import com.aristidevs.cursopremiumandroid.data.local.DogEntity
import com.aristidevs.cursopremiumandroid.data.local.DogSource

/**
 * The detail response already carries every field the list response has,
 * plus weight, origin and temperament, so the refresh builds each remote
 * entity from the per-dog detail request (RF-08).
 */
fun DogDetailResponse.toEntity(position: Int): DogEntity {
    return DogEntity(
        source = DogSource.REMOTE,
        remoteId = id,
        position = position,
        createdAt = 0L,
        name = name,
        breed = breed,
        age = age,
        description = description,
        image = BASE_URL + image,
        weight = weight,
        origin = origin,
        temperament = temperament
    )
}
