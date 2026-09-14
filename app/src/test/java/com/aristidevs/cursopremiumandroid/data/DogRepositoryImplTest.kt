package com.aristidevs.cursopremiumandroid.data

import com.aristidevs.cursopremiumandroid.data.local.DogEntity
import com.aristidevs.cursopremiumandroid.data.local.DogSource
import com.aristidevs.cursopremiumandroid.domain.model.CatalogRefreshResult
import com.aristidevs.cursopremiumandroid.fakes.FakeDogApiServices
import com.aristidevs.cursopremiumandroid.fakes.FakeDogDao
import com.aristidevs.cursopremiumandroid.fakes.FakeDogImageStore
import com.aristidevs.cursopremiumandroid.fakes.dogDetailResponse
import com.aristidevs.cursopremiumandroid.fakes.dogResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DogRepositoryImplTest {

    private lateinit var api: FakeDogApiServices
    private lateinit var dao: FakeDogDao
    private lateinit var repository: DogRepositoryImpl

    @Before
    fun setUp() {
        api = FakeDogApiServices()
        dao = FakeDogDao()
        repository = DogRepositoryImpl(api, dao, FakeDogImageStore())
    }

    @Test
    fun `refreshCatalog never alters an own dog even if a remote dog reuses its identifier`() = runTest {
        val ownDogId = dao.insertLocal(
            DogEntity(
                source = DogSource.LOCAL, remoteId = null, position = 0, createdAt = 1L,
                name = "Toby", breed = "Mestizo", age = 2, description = "Mio", image = "file://own.jpg",
                weight = null, origin = null, temperament = null
            )
        )
        val collidingRemoteId = ownDogId.toInt()
        api.dogs = listOf(dogResponse(id = collidingRemoteId))
        api.details = mapOf(collidingRemoteId to dogDetailResponse(id = collidingRemoteId))

        val result = repository.refreshCatalog()

        assertEquals(CatalogRefreshResult.Success, result)
        val ownDog = dao.dogs.value.first { it.id == ownDogId }
        assertEquals(DogSource.LOCAL, ownDog.source)
        assertEquals("Toby", ownDog.name)
        assertTrue(dao.dogs.value.any { it.source == DogSource.REMOTE && it.remoteId == collidingRemoteId })
    }

    @Test
    fun `refreshCatalog removes a remote dog the server no longer offers, keeping own dogs`() = runTest {
        val ownDogId = dao.insertLocal(
            DogEntity(
                source = DogSource.LOCAL, remoteId = null, position = 0, createdAt = 1L,
                name = "Mia", breed = "Mestizo", age = 4, description = "Mia", image = "file://mia.jpg",
                weight = null, origin = null, temperament = null
            )
        )
        api.dogs = listOf(dogResponse(1), dogResponse(2))
        api.details = mapOf(1 to dogDetailResponse(1), 2 to dogDetailResponse(2))
        repository.refreshCatalog()
        assertEquals(2, dao.dogs.value.count { it.source == DogSource.REMOTE })

        // The server catalog shrinks to a single dog.
        api.dogs = listOf(dogResponse(1))
        api.details = mapOf(1 to dogDetailResponse(1))
        val result = repository.refreshCatalog()

        assertEquals(CatalogRefreshResult.Success, result)
        val remoteIds = dao.dogs.value.filter { it.source == DogSource.REMOTE }.map { it.remoteId }
        assertEquals(listOf(1), remoteIds)
        assertTrue(dao.dogs.value.any { it.id == ownDogId && it.source == DogSource.LOCAL })
    }

    @Test
    fun `refreshCatalog keeps the previous catalog intact when a detail request fails`() = runTest {
        api.dogs = listOf(dogResponse(1))
        api.details = mapOf(1 to dogDetailResponse(1))
        repository.refreshCatalog()
        val previousState = dao.dogs.value

        api.dogs = listOf(dogResponse(1), dogResponse(2))
        api.details = mapOf(1 to dogDetailResponse(1), 2 to dogDetailResponse(2))
        api.failingDetailIds = setOf(2)

        val result = repository.refreshCatalog()

        assertTrue(result is CatalogRefreshResult.NoConnection)
        assertEquals(previousState, dao.dogs.value)
    }

    @Test
    fun `observeDogs maps entities to domain dogs marking own dogs`() = runTest {
        dao.insertLocal(
            DogEntity(
                source = DogSource.LOCAL, remoteId = null, position = 0, createdAt = 1L,
                name = "Rex", breed = "Mestizo", age = 5, description = "d", image = "file://rex.jpg",
                weight = null, origin = null, temperament = null
            )
        )

        val dogs = repository.observeDogs().first()

        assertEquals(1, dogs.size)
        assertTrue(dogs.first().isOwn)
    }
}
