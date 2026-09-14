package com.aristidevs.cursopremiumandroid.data.local

import com.aristidevs.cursopremiumandroid.core.di.DogApiConfig.BASE_URL
import com.aristidevs.cursopremiumandroid.data.mapper.toEntity
import com.aristidevs.cursopremiumandroid.domain.model.NewDog
import com.aristidevs.cursopremiumandroid.fakes.dogDetailResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapperTest {

    @Test
    fun `remote detail response maps to a REMOTE entity with the full image URL`() {
        val response = dogDetailResponse(id = 7, weight = "12kg", origin = "Francia", temperament = "Juguetón")

        val entity = response.toEntity(position = 3)

        assertEquals(DogSource.REMOTE, entity.source)
        assertEquals(7, entity.remoteId)
        assertEquals(3, entity.position)
        assertEquals(BASE_URL + "7.jpg", entity.image)
        assertEquals("12kg", entity.weight)
        assertEquals("Francia", entity.origin)
        assertEquals("Juguetón", entity.temperament)
    }

    @Test
    fun `remote entity maps to a domain dog that is not own`() {
        val entity = dogDetailResponse(id = 1).toEntity(position = 0)

        val dog = entity.toDomain()

        assertTrue(!dog.isOwn)
    }

    @Test
    fun `new dog with a file uri maps to a LOCAL entity using that uri as image and blank optionals as null`() {
        val newDog = NewDog(
            name = "Nala", breed = "Mestiza", age = 4, description = "Cariñosa",
            photoUri = "file://picked.jpg", weight = "", origin = null, temperament = "Tranquila"
        )

        val entity = newDog.toEntity(copiedPhotoUri = "file://internal/copy.jpg")

        assertEquals(DogSource.LOCAL, entity.source)
        assertNull(entity.remoteId)
        assertEquals("file://internal/copy.jpg", entity.image)
        assertNull(entity.weight)
        assertNull(entity.origin)
        assertEquals("Tranquila", entity.temperament)
    }
}
