package com.imageforge.app.image

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

data class ImageProcessRequest(
    val source: Uri,
    val quality: Int = 82,
    val maxDimension: Int? = null,
    val format: OutputFormat = OutputFormat.JPEG,
    val targetBytes: Long? = null
)

data class ImageProcessResult(
    val outputUri: Uri,
    val bytes: Long,
    val width: Int,
    val height: Int,
    val format: OutputFormat,
    val qualityUsed: Int,
    val targetBytes: Long? = null,
    val targetMet: Boolean = true
)

enum class OutputFormat(val label: String, val extension: String, val mimeType: String) {
    JPEG("JPG", "jpg", "image/jpeg"), PNG("PNG", "png", "image/png"), WEBP("WebP", "webp", "image/webp")
}

private data class EncodedCandidate(val bitmap: Bitmap, val bytes: ByteArray, val quality: Int)

object ImageEngine {
    fun processAndSave(context: Context, request: ImageProcessRequest): ImageProcessResult {
        val resolver = context.contentResolver
        val source = decodeSampled(resolver, request.source, request.maxDimension)
            ?: error("Image could not be decoded")
        var working = resizeIfNeeded(source, request.maxDimension)
        if (working !== source) source.recycle()

        val target = request.targetBytes?.takeIf { it > 0 && request.format != OutputFormat.PNG }
        val candidate = if (target != null) solveTarget(working, request.format, target) else {
            EncodedCandidate(working, encode(working, request.format, request.quality), request.quality.coerceIn(1, 100))
        }
        if (candidate.bitmap !== working) working.recycle()
        val uri = saveToPictures(context, resolver, candidate.bytes, request.format)
        val result = ImageProcessResult(
            outputUri = uri,
            bytes = candidate.bytes.size.toLong(),
            width = candidate.bitmap.width,
            height = candidate.bitmap.height,
            format = request.format,
            qualityUsed = candidate.quality,
            targetBytes = target,
            targetMet = target == null || candidate.bytes.size <= target
        )
        candidate.bitmap.recycle()
        return result
    }

    private fun solveTarget(initial: Bitmap, format: OutputFormat, target: Long): EncodedCandidate {
        var bitmap = initial
        var best: EncodedCandidate? = null
        repeat(8) {
            var low = 20
            var high = 100
            var roundBest: EncodedCandidate? = null
            while (low <= high) {
                val q = (low + high) / 2
                val bytes = encode(bitmap, format, q)
                if (bytes.size <= target) {
                    roundBest = EncodedCandidate(bitmap, bytes, q)
                    low = q + 1
                } else high = q - 1
            }
            if (roundBest != null) return roundBest

            val fallback = encode(bitmap, format, 20)
            if (best == null || fallback.size < best!!.bytes.size) best = EncodedCandidate(bitmap, fallback, 20)
            if (max(bitmap.width, bitmap.height) <= 480) return best!!

            val nextW = (bitmap.width * 0.85f).roundToInt().coerceAtLeast(1)
            val nextH = (bitmap.height * 0.85f).roundToInt().coerceAtLeast(1)
            val smaller = Bitmap.createScaledBitmap(bitmap, nextW, nextH, true)
            if (bitmap !== initial) bitmap.recycle()
            bitmap = smaller
        }
        return best ?: EncodedCandidate(bitmap, encode(bitmap, format, 20), 20)
    }

    private fun encode(bitmap: Bitmap, format: OutputFormat, quality: Int): ByteArray =
        ByteArrayOutputStream().use { stream ->
            if (!bitmap.compress(compressFormat(format), quality.coerceIn(1, 100), stream)) error("Image encoding failed")
            stream.toByteArray()
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
            val file = File(dir, name); file.outputStream().use { it.write(bytes) }; return Uri.fromFile(file)
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name); put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ImageForge"); put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("Could not create output file")
        try {
            resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Could not open output file")
            values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0); resolver.update(uri, values, null, null); return uri
        } catch (t: Throwable) { resolver.delete(uri, null, null); throw t }
    }
}
