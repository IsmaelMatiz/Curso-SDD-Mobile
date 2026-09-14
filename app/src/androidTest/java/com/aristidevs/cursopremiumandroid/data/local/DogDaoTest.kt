package com.aristidevs.cursopremiumandroid.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DogDaoTest {

    private lateinit var database: DogDatabase
    private lateinit var dao: DogDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), DogDatabase::class.java
        ).build()
        dao = database.dogDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun remoteEntity(remoteId: Int, position: Int) = DogEntity(
        source = DogSource.REMOTE, remoteId = remoteId, position = position, createdAt = 0L,
        name = "Remoto $remoteId", breed = "Mestizo", age = 3, description = "d", image = "img$remoteId",
        weight = "10kg", origin = "España", temperament = "Tranquilo"
    )

    private fun localEntity(name: String, createdAt: Long) = DogEntity(
        source = DogSource.LOCAL, remoteId = null, position = 0, createdAt = createdAt,
        name = name, breed = "Mestiza", age = 2, description = "d", image = "file://$name.jpg",
        weight = null, origin = null, temperament = null
    )

    @Test
    fun ownAndRemoteDogsCoexistWithoutDuplicatesAndOwnDogsComeFirst() = runBlocking {
        dao.replaceRemoteCatalog(listOf(remoteEntity(1, 0), remoteEntity(2, 1)))
        dao.insertLocal(localEntity("Toby", createdAt = 100L))
        dao.insertLocal(localEntity("Nala", createdAt = 200L))

        val dogs = dao.observeDogs().first()

        assertEquals(4, dogs.size)
        assertEquals(4, dogs.map { it.id }.toSet().size) // CA-03: no duplicates
        // Own dogs first, most recently created first; then remote dogs in server order.
        assertEquals(listOf("Nala", "Toby", "Remoto 1", "Remoto 2"), dogs.map { it.name })
    }

    @Test
    fun replacingTheRemoteCatalogNeverTouchesOwnDogs() = runBlocking {
        val ownDogId = dao.insertLocal(localEntity("Kira", createdAt = 1L))
        dao.replaceRemoteCatalog(listOf(remoteEntity(1, 0)))

        dao.replaceRemoteCatalog(listOf(remoteEntity(2, 0), remoteEntity(3, 1)))

        val dogs = dao.observeDogs().first()
        val ownDog = dogs.first { it.id == ownDogId }
        assertEquals(DogSource.LOCAL, ownDog.source)
        assertEquals("Kira", ownDog.name)
        assertEquals(setOf(2, 3), dogs.filter { it.source == DogSource.REMOTE }.map { it.remoteId }.toSet())
    }

    @Test
    fun anOwnDogSurvivesReadingItBackById() = runBlocking {
        val id = dao.insertLocal(localEntity("Rex", createdAt = 1L))

        val stored = dao.observeDogById(id).first()

        assertTrue(stored != null)
        assertEquals("Rex", stored?.name)
        assertEquals(DogSource.LOCAL, stored?.source)
    }
}
