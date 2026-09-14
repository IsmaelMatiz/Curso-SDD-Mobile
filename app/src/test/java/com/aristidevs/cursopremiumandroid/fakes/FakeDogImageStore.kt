package com.aristidevs.cursopremiumandroid.fakes

import android.net.Uri
import com.aristidevs.cursopremiumandroid.data.image.DogImageStore

/** Never touches `android.net.Uri`'s real implementation: just echoes the URI back. */
class FakeDogImageStore : DogImageStore {
    var deletedUris: MutableList<Uri> = mutableListOf()

    override suspend fun copyToInternalStorage(sourceUri: Uri): Uri = sourceUri

    override suspend fun delete(uri: Uri) {
        deletedUris += uri
    }
}
