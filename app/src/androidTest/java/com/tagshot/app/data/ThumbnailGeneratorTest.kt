package com.tagshot.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tagshot.app.domain.ThumbnailMath
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class ThumbnailGeneratorTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun testExifOrientations1Through8TransformCoordinatesAccurately() {
        // Create 4x2 synthetic bitmap: width=4, height=2
        // Pixel (0,0) is Red, all others are Black
        val src = Bitmap.createBitmap(4, 2, Bitmap.Config.ARGB_8888)
        src.eraseColor(Color.BLACK)
        src.setPixel(0, 0, Color.RED)

        val orientations = listOf(
            ExifInterface.ORIENTATION_NORMAL to Pair(0, 0),
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL to Pair(3, 0),
            ExifInterface.ORIENTATION_ROTATE_180 to Pair(3, 1),
            ExifInterface.ORIENTATION_FLIP_VERTICAL to Pair(0, 1),
            ExifInterface.ORIENTATION_TRANSPOSE to Pair(0, 0),
            ExifInterface.ORIENTATION_ROTATE_90 to Pair(1, 0),
            ExifInterface.ORIENTATION_TRANSVERSE to Pair(1, 3),
            ExifInterface.ORIENTATION_ROTATE_270 to Pair(0, 3)
        )

        for ((orientation, expectedCoord) in orientations) {
            val matrix = ThumbnailGenerator.getExifMatrix(orientation, src.width.toFloat(), src.height.toFloat())
            val transformed = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, false)

            val (expX, expY) = expectedCoord
            val pixel = transformed.getPixel(expX, expY)

            assertEquals(
                "Orientation $orientation must place Red pixel at ($expX, $expY), was (${Color.red(pixel)}, ${Color.green(pixel)}, ${Color.blue(pixel)})",
                Color.RED,
                pixel
            )
            transformed.recycle()
        }
        src.recycle()
    }

    @Test
    fun testThumbnailGenerationDimensionsMatchThumbnailMath() {
        // Create a temporary 1000x800 image file
        val tempDir = context.cacheDir
        val origFile = File(tempDir, "orig_test_1000x800.png")
        val thumbFile = File(tempDir, "thumb_test_512.jpg")

        val bitmap = Bitmap.createBitmap(1000, 800, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLUE)
        FileOutputStream(origFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()

        val success = ThumbnailGenerator.generateThumbnail(origFile, thumbFile, ExifInterface.ORIENTATION_NORMAL)
        assertEquals(true, success)

        val expectedDimensions = ThumbnailMath.calculate(1000, 800, 512)
        val decodedThumb = android.graphics.BitmapFactory.decodeFile(thumbFile.absolutePath)

        assertEquals(expectedDimensions.first, decodedThumb.width)
        assertEquals(expectedDimensions.second, decodedThumb.height)

        decodedThumb.recycle()
        origFile.delete()
        thumbFile.delete()
    }

    @Test
    fun testThumbnailGenerationWithRotationSwapsDimensions() {
        val tempDir = context.cacheDir
        val origFile = File(tempDir, "orig_test_rot.png")
        val thumbFile = File(tempDir, "thumb_test_rot_512.jpg")

        val bitmap = Bitmap.createBitmap(1000, 800, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.GREEN)
        FileOutputStream(origFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()

        // ORIENTATION_ROTATE_90 swaps width and height: 1000x800 becomes 800x1000
        val success = ThumbnailGenerator.generateThumbnail(origFile, thumbFile, ExifInterface.ORIENTATION_ROTATE_90)
        assertEquals(true, success)

        val expectedDimensions = ThumbnailMath.calculate(800, 1000, 512)
        val decodedThumb = android.graphics.BitmapFactory.decodeFile(thumbFile.absolutePath)

        assertEquals(expectedDimensions.first, decodedThumb.width)
        assertEquals(expectedDimensions.second, decodedThumb.height)

        decodedThumb.recycle()
        origFile.delete()
        thumbFile.delete()
    }
}
