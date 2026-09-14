package com.aristidevs.cursopremiumandroid.data.image

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

/**
 * Copies a photo picked through the system selector into the app's internal
 * storage. The selector only grants a temporary read permission on its URI;
 * without this copy the photo would stop being visible after the process
 * ends (RF-09, PLAN.md decision 4).
 */
class DogImageStore @Inject constructor(private val context: Context) {

    suspend fun copyToInternalStorage(sourceUri: Uri): Uri = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, DOGS_DIRECTORY).apply { mkdirs() }
        val destination = File(directory, "${UUID.randomUUID()}.jpg")
        val input = context.contentResolver.openInputStream(sourceUri)
            ?: error("No se pudo leer la foto seleccionada")
        input.use { source ->
            destination.outputStream().use { output -> source.copyTo(output) }
        }
        Uri.fromFile(destination)
    }

    /** Removes a copy made by [copyToInternalStorage], e.g. when an add flow is discarded. */
    suspend fun delete(uri: Uri) = withContext(Dispatchers.IO) {
        uri.path?.let { path -> File(path).delete() }
        Unit
    }

    private companion object {
        const val DOGS_DIRECTORY = "dog_photos"
    }
}
