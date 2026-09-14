package com.aristidevs.cursopremiumandroid.data.image

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class DogImageStoreTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store: DogImageStore = DogImageStoreImpl(context)

    @Test
    fun copyToInternalStorage_copiesContentAndSurvivesRereadingThePath() = runBlocking {
        val sourceFile = File(context.cacheDir, "source_photo.jpg")
        sourceFile.writeBytes(byteArrayOf(1, 2, 3, 4))
        val sourceUri = Uri.fromFile(sourceFile)

        val destinationUri = store.copyToInternalStorage(sourceUri)
        val destinationFile = File(requireNotNull(destinationUri.path))

        assertTrue("La copia debe existir", destinationFile.exists())
        assertEquals(
            "El contenido copiado debe ser igual al original",
            sourceFile.readBytes().toList(),
            destinationFile.readBytes().toList()
        )
        assertTrue(
            "La copia debe leerse igual releyendo la ruta guardada",
            File(destinationUri.path!!).readBytes().isNotEmpty()
        )

        sourceFile.delete()
        destinationFile.delete()
    }

    @Test
    fun delete_removesThePreviouslyCopiedFile() = runBlocking {
        val sourceFile = File(context.cacheDir, "source_photo_2.jpg")
        sourceFile.writeBytes(byteArrayOf(5, 6, 7))
        val destinationUri = store.copyToInternalStorage(Uri.fromFile(sourceFile))

        store.delete(destinationUri)

        assertFalse(File(destinationUri.path!!).exists())
        sourceFile.delete()
    }
}
