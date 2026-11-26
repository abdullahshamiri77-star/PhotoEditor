package com.photoeditor.engine.document

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.IOException
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Decodes imported images at a bounded working resolution.
 *
 * Android 9+ uses ImageDecoder so formats supported by the platform image
 * decoder (including HEIF/HEIC on supported devices) can be imported and
 * image orientation metadata is applied by the platform. Older Android
 * versions retain the BitmapFactory path.
 */
object ImageBitmapDecoder {
    const val MAX_IMPORT_PIXELS: Long = 12_000_000L
    const val MAX_IMPORT_DIMENSION: Int = 8_192

    fun decode(contentResolver: ContentResolver, uri: Uri): Bitmap {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeWithImageDecoder(contentResolver, uri)
        } else {
            decodeWithBitmapFactory(contentResolver, uri)
        }

        if (bitmap.width <= 0 || bitmap.height <= 0 ||
            bitmap.width > MAX_IMPORT_DIMENSION ||
            bitmap.height > MAX_IMPORT_DIMENSION ||
            bitmap.width.toLong() * bitmap.height.toLong() > MAX_IMPORT_PIXELS
        ) {
            bitmap.recycle()
            throw IOException("Decoded image dimensions exceed the supported import limits")
        }
        return bitmap
    }

    private fun decodeWithImageDecoder(contentResolver: ContentResolver, uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val targetSize = calculateTargetSize(info.size.width, info.size.height)
                ?: throw IOException("Image has invalid dimensions")
            decoder.setTargetSize(targetSize.first, targetSize.second)
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
            decoder.setMutableRequired(true)
        }
    }

    @Suppress("DEPRECATION")
    private fun decodeWithBitmapFactory(contentResolver: ContentResolver, uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: throw IOException("Unable to open the selected image")

        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) {
            throw IOException("Selected file is not a supported image or has invalid dimensions")
        }

        val targetSize = calculateTargetSize(width, height)
            ?: throw IOException("Image has invalid dimensions")
        var sampleSize = 1
        while (ceilDivide(width, sampleSize) > targetSize.first ||
            ceilDivide(height, sampleSize) > targetSize.second
        ) {
            if (sampleSize >= (1 shl 29)) {
                throw IOException("Image dimensions are too large to decode safely")
            }
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inDither = false
        }
        return contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw IOException("Image data could not be decoded")
    }

    /**
     * Returns a bounded width/height pair while preserving the source aspect ratio.
     * Null means the source dimensions are invalid.
     */
    internal fun calculateTargetSize(width: Int, height: Int): Pair<Int, Int>? {
        if (width <= 0 || height <= 0) return null

        val sourcePixels = width.toDouble() * height.toDouble()
        val dimensionScale = MAX_IMPORT_DIMENSION.toDouble() / maxOf(width, height)
        val pixelScale = sqrt(MAX_IMPORT_PIXELS.toDouble() / sourcePixels)
        val scale = min(1.0, min(dimensionScale, pixelScale))

        val targetWidth = maxOf(1, floor(width * scale).toInt())
        val targetHeight = maxOf(1, floor(height * scale).toInt())
        return targetWidth to targetHeight
    }

    private fun ceilDivide(value: Int, divisor: Int): Long =
        (value.toLong() + divisor - 1L) / divisor
}
