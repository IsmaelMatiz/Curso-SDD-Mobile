package com.aristidevs.cursopremiumandroid.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DogDao {

    /**
     * Local dogs first (most recently created first), then remote dogs in the
     * order the server sent them (RF-03).
     */
    @Query(
        """
        SELECT * FROM dogs
        ORDER BY
            CASE source WHEN 'LOCAL' THEN 0 ELSE 1 END ASC,
            CASE source WHEN 'LOCAL' THEN createdAt END DESC,
            CASE source WHEN 'REMOTE' THEN position END ASC
        """
    )
    fun observeDogs(): Flow<List<DogEntity>>

    @Query("SELECT * FROM dogs WHERE id = :id")
    fun observeDogById(id: Long): Flow<DogEntity?>

    @Insert
    suspend fun insertLocal(dog: DogEntity): Long

    @Query("DELETE FROM dogs WHERE source = 'REMOTE'")
    suspend fun deleteRemoteDogs()

    @Insert
    suspend fun insertRemoteDogs(dogs: List<DogEntity>)

    /**
     * Replaces the whole remote block in one transaction: either every remote
     * row ends up updated, or nothing changes (RF-12, CA-19). Never touches
     * rows with source = LOCAL (RF-05, CA-05).
     */
    @Transaction
    suspend fun replaceRemoteCatalog(dogs: List<DogEntity>) {
        deleteRemoteDogs()
        insertRemoteDogs(dogs)
    }
}
