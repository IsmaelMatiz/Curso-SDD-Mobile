package com.aristidevs.cursopremiumandroid.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/**
 * Origin of a row in the `dogs` table: fetched from the remote catalog, or
 * created by the person on the device.
 */
enum class DogSource {
    REMOTE, LOCAL
}

class DogSourceConverter {
    @TypeConverter
    fun fromSource(source: DogSource): String = source.name

    @TypeConverter
    fun toSource(value: String): DogSource = DogSource.valueOf(value)
}

/**
 * Single table for both remote and locally created dogs (PLAN.md, decision 1).
 * [id] is the identity used by navigation and the UI; [remoteId] is the
 * server identifier, null for local dogs, so a server id can never collide
 * with a local dog's identity (RF-05).
 */
@Entity(
    tableName = "dogs",
    indices = [Index(value = ["source", "remoteId"], unique = true)]
)
data class DogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: DogSource,
    val remoteId: Int?,
    val position: Int,
    val createdAt: Long,
    val name: String,
    val breed: String,
    val age: Int,
    val description: String,
    val image: String,
    val weight: String?,
    val origin: String?,
    val temperament: String?
)
