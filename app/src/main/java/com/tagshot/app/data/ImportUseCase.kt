package com.tagshot.app.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.tagshot.app.domain.TagResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

sealed interface SingleImportResult {
    data class Success(val photoId: Long) : SingleImportResult
    data object Duplicate : SingleImportResult
    sealed interface Failed : SingleImportResult {
        data object Unreadable : Failed
        data object Unsupported : Failed
        data object OutOfMemory : Failed
        data class Error(val message: String) : Failed
    }
}

data class BatchImportSummary(
    val done: Int,
    val total: Int,
    val imported: Int,
    val duplicates: Int,
    val failed: Int
)

class ImportUseCase(
    private val context: Context,
    private val fileManager: PhotoFileManager,
    private val photoDao: PhotoDao
) {

    private val contentResolver: ContentResolver get() = context.contentResolver

    suspend fun importSingle(
        uri: Uri,
        tags: List<TagResult.Valid> = emptyList()
    ): SingleImportResult = withContext(Dispatchers.IO) {
        // Step 1 per Section 7: Open stream via ContentResolver. Null stream -> Failed(Unreadable)
        val initialStream: InputStream = try {
            contentResolver.openInputStream(uri)
        } catch (_: Exception) {
            null
        } ?: return@withContext SingleImportResult.Failed.Unreadable

        // Step 2 per Section 7: Pick extension. Unknown -> Failed(Unsupported)
        val extension = resolveExtension(uri) ?: run {
            try { initialStream.close() } catch (_: Exception) {}
            return@withContext SingleImportResult.Failed.Unsupported
        }

        val uuid = UUID.randomUUID().toString()
        val origFile = File(fileManager.origDir, "$uuid.$extension")
        val tempOrigFile = File(fileManager.origDir, "$uuid.tmp")
        val thumbFile = File(fileManager.thumbDir, "$uuid.jpg")

        var sha256Hex = ""
        var sizeBytes = 0L

        // Step 3 per Section 7: Stream-copy while hashing
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            DigestInputStream(initialStream, digest).use { dis ->
                FileOutputStream(tempOrigFile).use { fos ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (dis.read(buffer).also { bytesRead = it } != -1) {
                        fos.write(buffer, 0, bytesRead)
                        sizeBytes += bytesRead
                    }
                    fos.fd.sync()
                }
            }

            sha256Hex = digest.digest().joinToString("") { "%02x".format(it) }

            if (!tempOrigFile.renameTo(origFile)) {
                tempOrigFile.delete()
                return@withContext SingleImportResult.Failed.Error("Atomic rename failed")
            }
        } catch (e: Exception) {
            tempOrigFile.delete()
            origFile.delete()
            return@withContext SingleImportResult.Failed.Unreadable
        }

        // Step 4 per Section 7: Check duplicate SHA in DB
        val existing = photoDao.getPhotoBySha256(sha256Hex)
        if (existing != null) {
            origFile.delete()
            return@withContext SingleImportResult.Duplicate
        }

        // Step 5 per Section 7: Read EXIF orientation and DateTimeOriginal
        var orientation = ExifInterface.ORIENTATION_NORMAL
        var takenAt: Long? = null
        try {
            val exif = ExifInterface(origFile.absolutePath)
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val dateStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            if (!dateStr.isNullOrBlank()) {
                val sdf = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
                takenAt = sdf.parse(dateStr)?.time
            }
        } catch (_: Exception) {
            // EXIF errors are never fatal
        }

        // Step 6 per Section 7: Decode bounds
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(origFile.absolutePath, boundsOptions)
        val rawW = boundsOptions.outWidth
        val rawH = boundsOptions.outHeight
        val mimeType = boundsOptions.outMimeType ?: resolveMimeType(extension)

        if (rawW <= 0 || rawH <= 0) {
            origFile.delete()
            return@withContext SingleImportResult.Failed.Unsupported
        }

        val isSwapped = orientation in listOf(
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_TRANSVERSE,
            ExifInterface.ORIENTATION_ROTATE_270
        )
        val orientedW = if (isSwapped) rawH else rawW
        val orientedH = if (isSwapped) rawW else rawH

        // Generate thumbnail
        try {
            val thumbSuccess = ThumbnailGenerator.generateThumbnail(origFile, thumbFile, orientation)
            if (!thumbSuccess) {
                origFile.delete()
                thumbFile.delete()
                return@withContext SingleImportResult.Failed.Unsupported
            }
        } catch (oom: OutOfMemoryError) {
            origFile.delete()
            thumbFile.delete()
            return@withContext SingleImportResult.Failed.OutOfMemory
        }

        // Step 7 per Section 7: DB insert with compensation
        val relativeOrig = "photos/orig/${origFile.name}"
        val relativeThumb = "photos/thumbs/${thumbFile.name}"

        val photoEntity = PhotoEntity(
            originalPath = relativeOrig,
            thumbPath = relativeThumb,
            sha256 = sha256Hex,
            mimeType = mimeType,
            width = orientedW,
            height = orientedH,
            sizeBytes = sizeBytes,
            addedAt = System.currentTimeMillis(),
            takenAt = takenAt,
            note = ""
        )

        try {
            val photoId = photoDao.insertPhotoWithTags(photoEntity, tags)
            SingleImportResult.Success(photoId)
        } catch (e: Exception) {
            // Compensate
            origFile.delete()
            thumbFile.delete()
            SingleImportResult.Failed.Error(e.message ?: "Database insert failed")
        }
    }

    suspend fun importBatch(
        uris: List<Uri>,
        tags: List<TagResult.Valid> = emptyList(),
        onProgress: (BatchImportSummary) -> Unit = {}
    ): BatchImportSummary = withContext(Dispatchers.IO) {
        var imported = 0
        var duplicates = 0
        var failed = 0
        val total = uris.size

        for ((index, uri) in uris.withIndex()) {
            when (importSingle(uri, tags)) {
                is SingleImportResult.Success -> imported++
                is SingleImportResult.Duplicate -> duplicates++
                is SingleImportResult.Failed -> failed++
            }
            onProgress(
                BatchImportSummary(
                    done = index + 1,
                    total = total,
                    imported = imported,
                    duplicates = duplicates,
                    failed = failed
                )
            )
        }

        BatchImportSummary(
            done = total,
            total = total,
            imported = imported,
            duplicates = duplicates,
            failed = failed
        )
    }

    private fun resolveExtension(uri: Uri): String? {
        val mime = contentResolver.getType(uri)?.lowercase(Locale.ROOT)
        if (mime != null) {
            when (mime) {
                "image/jpeg", "image/jpg" -> return "jpg"
                "image/png" -> return "png"
                "image/webp" -> return "webp"
                "image/heic" -> return "heic"
                "image/heif" -> return "heif"
                "image/gif" -> return "gif"
            }
        }

        // Check path extension
        val path = uri.path ?: uri.toString()
        val dotExt = path.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (dotExt in listOf("jpg", "jpeg", "png", "webp", "heic", "heif", "gif")) {
            return if (dotExt == "jpeg") "jpg" else dotExt
        }

        // Try decoding bounds mime
        return try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, options)
                when (options.outMimeType?.lowercase(Locale.ROOT)) {
                    "image/jpeg", "image/jpg" -> "jpg"
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    "image/gif" -> "gif"
                    else -> null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveMimeType(extension: String): String {
        return when (extension.lowercase(Locale.ROOT)) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "heic" -> "image/heic"
            "heif" -> "image/heif"
            else -> "image/jpeg"
        }
    }
}
