package com.aristidevs.cursopremiumandroid.fakes

import com.aristidevs.cursopremiumandroid.data.local.DogDao
import com.aristidevs.cursopremiumandroid.data.local.DogEntity
import com.aristidevs.cursopremiumandroid.data.local.DogSource
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/** In-memory double of [DogDao], replicating its ordering and identity rules. */
class FakeDogDao : DogDao {

    private val nextId = AtomicLong(1)
    private val _dogs = MutableStateFlow<List<DogEntity>>(emptyList())
    val dogs: StateFlow<List<DogEntity>> = _dogs

    override fun observeDogs() = _dogs.map { entities ->
        entities.sortedWith(
            compareBy(
                { it.source != DogSource.LOCAL },
                { if (it.source == DogSource.LOCAL) -it.createdAt else 0L },
                { if (it.source == DogSource.REMOTE) it.position else 0 }
            )
        )
    }

    override fun observeDogById(id: Long) = _dogs.map { entities -> entities.firstOrNull { it.id == id } }

    override suspend fun insertLocal(dog: DogEntity): Long {
        val id = nextId.getAndIncrement()
        _dogs.value = _dogs.value + dog.copy(id = id)
        return id
    }

    override suspend fun deleteRemoteDogs() {
        _dogs.value = _dogs.value.filterNot { it.source == DogSource.REMOTE }
    }

    override suspend fun insertRemoteDogs(dogs: List<DogEntity>) {
        _dogs.value = _dogs.value + dogs.map { it.copy(id = nextId.getAndIncrement()) }
    }
    // replaceRemoteCatalog is inherited from DogDao's default implementation.
}
