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
        val uuid = UUID.randomUUID().toString()
        val tempOrigFile = File(fileManager.origDir, "$uuid.tmp")

        // Step 1: Open stream via ContentResolver once. Null stream -> Failed(Unreadable)
        val initialStream: InputStream = try {
            contentResolver.openInputStream(uri)
        } catch (_: Exception) {
            null
        } ?: return@withContext SingleImportResult.Failed.Unreadable

        val sha256Hex: String
        var sizeBytes = 0L

        // Step 2: Stream-copy to temp file while calculating SHA-256 and byte count in a single pass
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
        } catch (_: Exception) {
            tempOrigFile.delete()
            return@withContext SingleImportResult.Failed.Unreadable
        }

        if (sizeBytes <= 0L) {
            tempOrigFile.delete()
            return@withContext SingleImportResult.Failed.Unreadable
        }

        // Step 3: Check duplicate SHA in DB
        val existing = photoDao.getPhotoBySha256(sha256Hex)
        if (existing != null) {
            val existingOrig = fileManager.resolve(existing.originalPath)
            val existingThumb = fileManager.resolve(existing.thumbPath)
            if (existingOrig.exists() && existingThumb.exists()) {
                tempOrigFile.delete()
                return@withContext SingleImportResult.Duplicate
            } else {
                // Dead/ghost DB row whose files were deleted on disk: prune it so re-import succeeds
                photoDao.deletePhotosAndPrune(listOf(existing.id))
            }
        }

        // Step 4: Decode bounds and verify the file is a valid, readable image
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(tempOrigFile.absolutePath, boundsOptions)
        val rawW = boundsOptions.outWidth
        val rawH = boundsOptions.outHeight

        if (rawW <= 0 || rawH <= 0) {
            tempOrigFile.delete()
            return@withContext SingleImportResult.Failed.Unsupported
        }

        // Determine extension and MIME type directly from bounds, content resolver, or URI path
        val crMime = try { contentResolver.getType(uri)?.lowercase(Locale.ROOT) } catch (_: Exception) { null }
        val extension = resolveExtensionFromMime(crMime)
            ?: resolveExtensionFromMime(boundsOptions.outMimeType)
            ?: resolveExtensionFromUri(uri)
            ?: "jpg"

        val mimeType = boundsOptions.outMimeType
            ?: crMime
            ?: resolveMimeType(extension)

        val origFile = File(fileManager.origDir, "$uuid.$extension")
        val thumbFile = File(fileManager.thumbDir, "$uuid.jpg")

        // Atomic rename from .tmp to final extension
        if (!tempOrigFile.renameTo(origFile)) {
            tempOrigFile.delete()
            return@withContext SingleImportResult.Failed.Error("Atomic rename failed")
        }

        // Step 5: Read EXIF orientation and DateTimeOriginal
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

        val isSwapped = orientation in listOf(
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_TRANSVERSE,
            ExifInterface.ORIENTATION_ROTATE_270
        )
        val orientedW = if (isSwapped) rawH else rawW
        val orientedH = if (isSwapped) rawW else rawH

        // Step 6: Generate thumbnail
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
        } catch (e: Exception) {
            origFile.delete()
            thumbFile.delete()
            return@withContext SingleImportResult.Failed.Error(e.message ?: "Thumbnail generation failed")
        }

        // Step 7: DB insert with compensation
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
            // Compensate on DB failure
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
            val result = try {
                importSingle(uri, tags)
            } catch (e: Exception) {
                SingleImportResult.Failed.Error(e.message ?: "Import failed")
            }
            when (result) {
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

    private fun resolveExtensionFromMime(mime: String?): String? {
        return when (mime?.lowercase(Locale.ROOT)) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/heic" -> "heic"
            "image/heif" -> "heif"
            "image/gif" -> "gif"
            else -> null
        }
    }

    private fun resolveExtensionFromUri(uri: Uri): String? {
        val path = uri.path ?: uri.toString()
        val dotExt = path.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return if (dotExt in listOf("jpg", "jpeg", "png", "webp", "heic", "heif", "gif")) {
            if (dotExt == "jpeg") "jpg" else dotExt
        } else null
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
