package com.imageforge.app.image

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import java.io.File
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

data class ImageProcessRequest(
    val source: Uri,
    val quality: Int = 82,
    val maxDimension: Int? = null,
    val format: OutputFormat = OutputFormat.JPEG
)

data class ImageProcessResult(
    val outputUri: Uri,
    val bytes: Long,
    val width: Int,
    val height: Int,
    val format: OutputFormat
)

enum class OutputFormat(val label: String, val extension: String, val mimeType: String) {
    JPEG("JPG", "jpg", "image/jpeg"), PNG("PNG", "png", "image/png"), WEBP("WebP", "webp", "image/webp")
}

object ImageEngine {
    fun processAndSave(context: Context, request: ImageProcessRequest): ImageProcessResult {
        val resolver = context.contentResolver
        val source = decodeSampled(resolver, request.source, request.maxDimension)
            ?: error("Image could not be decoded")
        val resized = resizeIfNeeded(source, request.maxDimension)
        if (resized !== source) source.recycle()

        val bytes = ByteArrayOutputStream().use { stream ->
            val ok = resized.compress(compressFormat(request.format), request.quality.coerceIn(1, 100), stream)
            if (!ok) error("Image encoding failed")
            stream.toByteArray()
        }
        val uri = saveToPictures(context, resolver, bytes, request.format)
        val result = ImageProcessResult(uri, bytes.size.toLong(), resized.width, resized.height, request.format)
        resized.recycle()
        return result
    }

    private fun decodeSampled(resolver: ContentResolver, uri: Uri, maxDimension: Int?): Bitmap? {
        if (maxDimension == null) return resolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDimension) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun resizeIfNeeded(bitmap: Bitmap, maxDimension: Int?): Bitmap {
        if (maxDimension == null || max(bitmap.width, bitmap.height) <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / max(bitmap.width, bitmap.height)
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).roundToInt(), (bitmap.height * scale).roundToInt(), true)
    }

    @Suppress("DEPRECATION")
    private fun compressFormat(format: OutputFormat): Bitmap.CompressFormat = when (format) {
        OutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
        OutputFormat.PNG -> Bitmap.CompressFormat.PNG
        OutputFormat.WEBP -> if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
    }

    private fun saveToPictures(context: Context, resolver: ContentResolver, bytes: ByteArray, format: OutputFormat): Uri {
        val name = "ImageForge_${System.currentTimeMillis()}.${format.extension}"
        if (Build.VERSION.SDK_INT < 29) {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "ImageForge").apply { mkdirs() }
            val file = File(dir, name)
            file.outputStream().use { it.write(bytes) }
            return Uri.fromFile(file)
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ImageForge")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create output file")
        try {
            resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Could not open output file")
            values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0); resolver.update(uri, values, null, null)
            return uri
        } catch (t: Throwable) {
            resolver.delete(uri, null, null)
            throw t
        }
    }
}
