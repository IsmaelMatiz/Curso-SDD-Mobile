package com.aristidevs.cursopremiumandroid.data

import android.net.Uri
import com.aristidevs.cursopremiumandroid.data.api.DogApiServices
import com.aristidevs.cursopremiumandroid.data.image.DogImageStore
import com.aristidevs.cursopremiumandroid.data.local.DogDao
import com.aristidevs.cursopremiumandroid.data.local.toDetailDomain
import com.aristidevs.cursopremiumandroid.data.local.toDomain
import com.aristidevs.cursopremiumandroid.data.local.toEntity
import com.aristidevs.cursopremiumandroid.data.mapper.toEntity
import com.aristidevs.cursopremiumandroid.domain.DogRepository
import com.aristidevs.cursopremiumandroid.domain.model.CatalogRefreshResult
import com.aristidevs.cursopremiumandroid.domain.model.Dog
import com.aristidevs.cursopremiumandroid.domain.model.DogDetailModel
import com.aristidevs.cursopremiumandroid.domain.model.NewDog
import jakarta.inject.Inject
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DogRepositoryImpl @Inject constructor(
    private val api: DogApiServices,
    private val dao: DogDao,
    private val imageStore: DogImageStore
) : DogRepository {

    override fun observeDogs(): Flow<List<Dog>> =
        dao.observeDogs().map { entities -> entities.map { it.toDomain() } }

    override fun observeDogDetail(id: Long): Flow<DogDetailModel?> =
        dao.observeDogById(id).map { entity -> entity?.toDetailDomain() }

    override suspend fun refreshCatalog(): CatalogRefreshResult {
        return try {
            val list = api.getDogs()
            // Every detail is fetched in parallel; if one fails the rest are
            // cancelled and nothing is written (RF-12, CA-19).
            val details = coroutineScope {
                list.map { dogResponse -> async { api.getDogDetail(dogResponse.id) } }.awaitAll()
            }
            val entities = details.mapIndexed { position, detail -> detail.toEntity(position) }
            dao.replaceRemoteCatalog(entities)
            CatalogRefreshResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            CatalogRefreshResult.NoConnection
        } catch (e: Exception) {
            CatalogRefreshResult.UnexpectedError(e.message)
        }
    }

    override suspend fun addDog(newDog: NewDog): Long {
        val pickedUri = Uri.parse(requireNotNull(newDog.photoUri) { "El alta requiere una foto" })
        val copiedUri = imageStore.copyToInternalStorage(pickedUri)
        return dao.insertLocal(newDog.toEntity(copiedUri.toString()))
    }
}
