package com.tagshot.app.data

import android.net.Uri
import com.tagshot.app.domain.TagNormalizer
import com.tagshot.app.domain.TagResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

interface PhotoRepository {
    fun observePhotos(): Flow<List<PhotoWithTags>>
    fun observeTags(): Flow<List<TagWithCount>>
    suspend fun getPhotoWithTagsById(id: Long): PhotoWithTags?
    suspend fun importUris(
        uris: List<Uri>,
        tags: List<TagResult.Valid>,
        onProgress: (BatchImportSummary) -> Unit
    ): BatchImportSummary
    suspend fun importCameraFile(file: File, tags: List<TagResult.Valid>): SingleImportResult
    suspend fun addTagsToPhotos(photoIds: List<Long>, tags: List<TagResult.Valid>)
    suspend fun removeTagFromPhoto(photoId: Long, tagId: Long)
    suspend fun updateNote(photoId: Long, note: String)
    suspend fun prepareDelete(photoIds: List<Long>): List<DeletedPhotoBackup>
    suspend fun restorePhotos(backups: List<DeletedPhotoBackup>)
    suspend fun finalizeDelete(backups: List<DeletedPhotoBackup>)
    suspend fun renameOrMergeTag(tagId: Long, newRaw: String): Boolean
    suspend fun deleteTagEverywhere(tagId: Long)
}

class DefaultPhotoRepository(
    private val photoDao: PhotoDao,
    private val fileManager: PhotoFileManager,
    private val importUseCase: ImportUseCase
) : PhotoRepository {

    override fun observePhotos(): Flow<List<PhotoWithTags>> = photoDao.observePhotosWithTags()

    override fun observeTags(): Flow<List<TagWithCount>> = photoDao.observeTagsWithCount()

    override suspend fun getPhotoWithTagsById(id: Long): PhotoWithTags? =
        photoDao.getPhotoWithTagsById(id)


    override suspend fun importUris(
        uris: List<Uri>,
        tags: List<TagResult.Valid>,
        onProgress: (BatchImportSummary) -> Unit
    ): BatchImportSummary = importUseCase.importBatch(uris, tags, onProgress)

    override suspend fun importCameraFile(file: File, tags: List<TagResult.Valid>): SingleImportResult {
        val result = importUseCase.importSingle(Uri.fromFile(file), tags)
        file.delete() // Cleanup camera temp file
        return result
    }

    override suspend fun addTagsToPhotos(photoIds: List<Long>, tags: List<TagResult.Valid>) {
        if (photoIds.isEmpty() || tags.isEmpty()) return
        withContext(Dispatchers.IO) {
            photoDao.addTagsToPhotos(photoIds, tags)
        }
    }

    override suspend fun removeTagFromPhoto(photoId: Long, tagId: Long) {
        withContext(Dispatchers.IO) {
            photoDao.removeTagFromPhotoAndPrune(photoId, tagId)
        }
    }

    override suspend fun updateNote(photoId: Long, note: String) {
        withContext(Dispatchers.IO) {
            photoDao.updateNote(photoId, note)
        }
    }

    override suspend fun prepareDelete(photoIds: List<Long>): List<DeletedPhotoBackup> = withContext(Dispatchers.IO) {
        val backups = photoIds.mapNotNull { id ->
            photoDao.getPhotoWithTagsById(id)?.let {
                DeletedPhotoBackup(photo = it.photo, tags = it.tags)
            }
        }
        photoDao.deletePhotosAndPrune(photoIds)
        backups
    }

    override suspend fun restorePhotos(backups: List<DeletedPhotoBackup>) = withContext(Dispatchers.IO) {
        for (backup in backups) {
            photoDao.restorePhotoWithTags(backup.photo, backup.tags)
        }
    }

    override suspend fun finalizeDelete(backups: List<DeletedPhotoBackup>) = withContext(Dispatchers.IO) {
        for (backup in backups) {
            // Delete original and thumbnail files from disk
            fileManager.deleteRelative(backup.photo.originalPath)
            fileManager.deleteRelative(backup.photo.thumbPath)
        }
    }

    override suspend fun renameOrMergeTag(tagId: Long, newRaw: String): Boolean = withContext(Dispatchers.IO) {
        val normalized = TagNormalizer.normalize(newRaw)
        if (normalized !is TagResult.Valid) return@withContext false
        photoDao.renameOrMergeTag(tagId, normalized.display, normalized.normalized)
        true
    }

    override suspend fun deleteTagEverywhere(tagId: Long): Unit = withContext(Dispatchers.IO) {
        photoDao.deleteTagById(tagId)
        photoDao.pruneUnusedTags()
        Unit
    }

}
