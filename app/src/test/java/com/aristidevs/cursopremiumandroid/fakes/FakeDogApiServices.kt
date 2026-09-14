package com.aristidevs.cursopremiumandroid.fakes

import com.aristidevs.cursopremiumandroid.data.api.DogApiServices
import com.aristidevs.cursopremiumandroid.data.api.response.DogDetailResponse
import com.aristidevs.cursopremiumandroid.data.api.response.DogResponse
import java.io.IOException

class FakeDogApiServices : DogApiServices {

    var dogs: List<DogResponse> = emptyList()
    var details: Map<Int, DogDetailResponse> = emptyMap()
    var failingDetailIds: Set<Int> = emptySet()
    var listFailsWithConnectionError: Boolean = false

    override suspend fun getDogs(): List<DogResponse> {
        if (listFailsWithConnectionError) throw IOException("no connection")
        return dogs
    }

    override suspend fun getDogDetail(id: Int): DogDetailResponse {
        if (id in failingDetailIds) throw IOException("no connection")
        return details.getValue(id)
    }
}

fun dogResponse(id: Int, name: String = "Firulais") = DogResponse(
    id = id, name = name, age = 3, description = "Un perro", image = "$id.jpg", breed = "Mestizo"
)

fun dogDetailResponse(
    id: Int,
    name: String = "Firulais",
    weight: String = "10kg",
    origin: String = "España",
    temperament: String = "Tranquilo"
) = DogDetailResponse(
    id = id,
    name = name,
    breed = "Mestizo",
    age = 3,
    description = "Un perro",
    image = "$id.jpg",
    weight = weight,
    origin = origin,
    temperament = temperament
)
