package com.aristidevs.cursopremiumandroid.data.local

import com.aristidevs.cursopremiumandroid.domain.model.Dog
import com.aristidevs.cursopremiumandroid.domain.model.DogDetailModel
import com.aristidevs.cursopremiumandroid.domain.model.NewDog

fun DogEntity.toDomain(): Dog {
    return Dog(
        id = id,
        name = name,
        age = age,
        description = description,
        image = image,
        breed = breed,
        isOwn = source == DogSource.LOCAL
    )
}

fun DogEntity.toDetailDomain(): DogDetailModel {
    return DogDetailModel(
        id = id,
        name = name,
        breed = breed,
        age = age,
        description = description,
        image = image,
        weight = weight,
        origin = origin,
        temperament = temperament
    )
}

/** [copiedPhotoUri] is already the internal, permanent URI (data layer's responsibility). */
fun NewDog.toEntity(copiedPhotoUri: String): DogEntity {
    return DogEntity(
        source = DogSource.LOCAL,
        remoteId = null,
        position = 0,
        createdAt = System.currentTimeMillis(),
        name = name,
        breed = breed,
        age = requireNotNull(age) { "El alta solo se persiste tras validar que la edad no es nula" },
        description = description,
        image = copiedPhotoUri,
        weight = weight?.takeIf { it.isNotBlank() },
        origin = origin?.takeIf { it.isNotBlank() },
        temperament = temperament?.takeIf { it.isNotBlank() }
    )
}
