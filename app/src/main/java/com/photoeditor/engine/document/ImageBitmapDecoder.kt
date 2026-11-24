package com.photoeditor.engine.document

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

/**
 * Decodes imported images at a bounded working resolution.
 *
 * Bounds are read first so a phone never allocates a full-resolution camera
 * image only to scale it down after decoding.
 */
object ImageBitmapDecoder {
    const val MAX_IMPORT_PIXELS: Long = 12_000_000L
    const val MAX_IMPORT_DIMENSION: Int = 8_192

    fun decode(contentResolver: ContentResolver, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: return null

        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) return null

        var sampleSize = 1
        while (true) {
            val sampledWidth = (width.toLong() + sampleSize - 1L) / sampleSize
            val sampledHeight = (height.toLong() + sampleSize - 1L) / sampleSize
            val withinDimensionLimit =
                sampledWidth <= MAX_IMPORT_DIMENSION && sampledHeight <= MAX_IMPORT_DIMENSION
            val withinPixelLimit = sampledWidth * sampledHeight <= MAX_IMPORT_PIXELS
            if (withinDimensionLimit && withinPixelLimit) break
            if (sampleSize > (1 shl 29)) return null
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inDither = false
        }
        return contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }
}
