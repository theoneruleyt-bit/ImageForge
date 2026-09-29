package com.imageforge.app.image

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

data class ImageMetadata(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val make: String? = null,
    val model: String? = null,
    val dateTime: String? = null,
    val software: String? = null,
    val lensModel: String? = null
) {
    val hasLocation: Boolean get() = latitude != null && longitude != null
    val presentCount: Int get() = listOf(make, model, dateTime, software, lensModel).count { !it.isNullOrBlank() } + if (hasLocation) 1 else 0
}

object MetadataEngine {
    fun read(context: Context, uri: Uri): ImageMetadata {
        val exif = context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
            ?: return ImageMetadata()
        val latLong = exif.latLong
        return ImageMetadata(
            latitude = latLong?.getOrNull(0),
            longitude = latLong?.getOrNull(1),
            make = exif.getAttribute(ExifInterface.TAG_MAKE),
            model = exif.getAttribute(ExifInterface.TAG_MODEL),
            dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME),
            software = exif.getAttribute(ExifInterface.TAG_SOFTWARE),
            lensModel = exif.getAttribute(ExifInterface.TAG_LENS_MODEL)
        )
    }
}
