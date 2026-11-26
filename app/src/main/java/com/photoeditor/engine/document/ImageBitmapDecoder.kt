package com.photoeditor.engine.document

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

/**
 * Decodes imported images at a bounded working resolution.
 * Uses independent descriptor/stream attempts because some content providers
 * behave differently for repeated stream decoding.
 */
object ImageBitmapDecoder {
    const val MAX_IMPORT_PIXELS: Long = 12_000_000L
    const val MAX_IMPORT_DIMENSION: Int = 8_192

    fun decode(contentResolver: ContentResolver, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsRead = try {
            contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                BitmapFactory.decodeFileDescriptor(descriptor.fileDescriptor, null, bounds)
                true
            } ?: false
        } catch (_: Exception) {
            false
        }

        // Some providers do not expose a usable file descriptor. Fall back to a
        // stream for bounds only; do not decode an unbounded full-size image.
        if (!boundsRead || bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val streamBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(stream, null, streamBounds)
                    bounds.outWidth = streamBounds.outWidth
                    bounds.outHeight = streamBounds.outHeight
                } ?: return null
            } catch (_: Exception) {
                return null
            }
        }

        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) return null
        var sampleSize = calculateSampleSize(width, height)

        // If one decode attempt fails, progressively reduce working resolution.
        // This handles providers/codecs that reject a particular sampling size
        // without ever attempting an unrestricted full-resolution allocation.
        while (sampleSize <= (1 shl 29)) {
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inDither = false
            }
            val decoded = try {
                contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                    BitmapFactory.decodeFileDescriptor(descriptor.fileDescriptor, null, options)
                }
            } catch (_: Exception) {
                null
            } ?: try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }
            } catch (_: Exception) {
                null
            }
            if (decoded != null && decoded.width > 0 && decoded.height > 0) return decoded
            sampleSize *= 2
        }
        return null
    }

    internal fun calculateSampleSize(width: Int, height: Int): Int {
        require(width > 0 && height > 0)
        var sampleSize = 1
        while (true) {
            val sampledWidth = (width.toLong() + sampleSize - 1L) / sampleSize
            val sampledHeight = (height.toLong() + sampleSize - 1L) / sampleSize
            if (sampledWidth <= MAX_IMPORT_DIMENSION &&
                sampledHeight <= MAX_IMPORT_DIMENSION &&
                sampledWidth * sampledHeight <= MAX_IMPORT_PIXELS
            ) return sampleSize
            if (sampleSize > (1 shl 29)) return sampleSize
            sampleSize *= 2
        }
    }
}
