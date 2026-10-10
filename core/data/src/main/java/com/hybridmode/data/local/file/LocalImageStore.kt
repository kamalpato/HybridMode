package com.hybridmode.data.local.file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class LocalImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val rootDir: File
        get() = File(context.filesDir, "uploads").also { if (!it.exists()) it.mkdirs() }

    fun persistFromUri(sourceUri: String, mimeType: String): File {
        val target = File(rootDir, "${UUID.randomUUID()}.jpg")
        val bytes = context.contentResolver.openInputStream(Uri.parse(sourceUri)).use { input ->
            requireNotNull(input) { "Unable to open selected image" }
            input.readBytes()
        }
        require(bytes.isNotEmpty()) { "Selected image is empty" }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        val sample = sampleSizeFor(bounds.outWidth, bounds.outHeight, MAX_EDGE_PX)
        val decode = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decode)
        if (bitmap == null) {
            val extension = when {
                mimeType.contains("png") -> "png"
                mimeType.contains("webp") -> "webp"
                else -> "bin"
            }
            val fallback = File(rootDir, "${UUID.randomUUID()}.$extension")
            fallback.outputStream().use { it.write(bytes) }
            require(fallback.exists() && fallback.length() > 0L) { "Failed to save image locally" }
            return fallback
        }

        target.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        bitmap.recycle()
        require(target.exists() && target.length() > 0L) { "Failed to save image locally" }
        return target
    }

    fun deleteIfExists(path: String) {
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }

    private fun sampleSizeFor(width: Int, height: Int, maxEdge: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sample = 1
        var largest = max(width, height)
        while (largest / (sample * 2) >= maxEdge) {
            sample *= 2
        }
        return sample.coerceAtLeast(1)
    }

    private companion object {
        const val MAX_EDGE_PX = 1600
        const val JPEG_QUALITY = 82
    }
}
