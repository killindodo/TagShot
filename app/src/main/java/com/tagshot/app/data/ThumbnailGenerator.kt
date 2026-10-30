package com.tagshot.app.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.tagshot.app.domain.Limits
import com.tagshot.app.domain.ThumbnailMath
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ThumbnailGenerator {

    /**
     * Maps EXIF orientation (1..8) to a 2D Transformation Matrix.
     */
    fun getExifMatrix(orientation: Int, width: Float, height: Float): Matrix {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_NORMAL, 0 -> {
                // Identity
            }
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                matrix.postScale(-1f, 1f)
                matrix.postTranslate(width, 0f)
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                matrix.postRotate(180f)
                matrix.postTranslate(width, height)
            }
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.postScale(1f, -1f)
                matrix.postTranslate(0f, height)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                // Transpose: Flip along top-left to bottom-right diagonal
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
                // oriented bounds: height x width
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                matrix.postRotate(90f)
                matrix.postTranslate(height, 0f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                // Transverse: rotate 270 then flip H
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
                matrix.postTranslate(0f, width)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                matrix.postRotate(270f)
                matrix.postTranslate(0f, width)
            }
        }
        return matrix
    }

    /**
     * Generates a JPEG thumbnail file at [destinationFile] from [origFile].
     * Recycles all intermediate bitmaps.
     */
    fun generateThumbnail(
        origFile: File,
        destinationFile: File,
        orientation: Int = ExifInterface.ORIENTATION_NORMAL
    ): Boolean {
        destinationFile.parentFile?.mkdirs()

        // 1. Decode bounds
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(origFile.absolutePath, boundsOptions)
        val rawW = boundsOptions.outWidth
        val rawH = boundsOptions.outHeight
        if (rawW <= 0 || rawH <= 0) return false

        val isSwapped = orientation in listOf(
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_TRANSVERSE,
            ExifInterface.ORIENTATION_ROTATE_270
        )
        val orientedW = if (isSwapped) rawH else rawW
        val orientedH = if (isSwapped) rawW else rawH

        // 2. Compute inSampleSize
        val sampleSize = ThumbnailMath.computeInSampleSize(orientedW, orientedH, Limits.THUMB_MAX_EDGE)

        // 3. Decode sampled bitmap
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val sampledBitmap = BitmapFactory.decodeFile(origFile.absolutePath, decodeOptions) ?: return false

        // 4. Transform orientation and final scale
        val matrix = getExifMatrix(orientation, sampledBitmap.width.toFloat(), sampledBitmap.height.toFloat())

        val (targetW, targetH) = ThumbnailMath.calculate(orientedW, orientedH, Limits.THUMB_MAX_EDGE)
        val currentOrientedW = if (isSwapped) sampledBitmap.height else sampledBitmap.width
        val currentOrientedH = if (isSwapped) sampledBitmap.width else sampledBitmap.height

        val scaleX = targetW.toFloat() / currentOrientedW
        val scaleY = targetH.toFloat() / currentOrientedH
        matrix.postScale(scaleX, scaleY)

        val finalBitmap = try {
            Bitmap.createBitmap(
                sampledBitmap,
                0,
                0,
                sampledBitmap.width,
                sampledBitmap.height,
                matrix,
                true
            )
        } catch (oom: OutOfMemoryError) {
            sampledBitmap.recycle()
            throw oom
        }

        if (finalBitmap !== sampledBitmap) {
            sampledBitmap.recycle()
        }

        // 5. Compress to JPEG
        val tempThumb = File(destinationFile.parentFile, "${destinationFile.name}.tmp")
        try {
            FileOutputStream(tempThumb).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, Limits.THUMB_JPEG_QUALITY, out)
                out.fd.sync()
            }
            if (tempThumb.renameTo(destinationFile)) {
                return true
            } else {
                tempThumb.delete()
                return false
            }
        } finally {
            finalBitmap.recycle()
        }
    }
}
