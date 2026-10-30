package com.tagshot.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.tagshot.app.domain.TagResult
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {

    @Transaction
    @Query("SELECT * FROM photos ORDER BY addedAt DESC, id DESC")
    fun observePhotosWithTags(): Flow<List<PhotoWithTags>>

    @Query(
        """
        SELECT 
            tags.id, 
            tags.name, 
            tags.normalizedName, 
            COUNT(photo_tag_cross_ref.photoId) AS photoCount 
        FROM tags 
        LEFT JOIN photo_tag_cross_ref ON tags.id = photo_tag_cross_ref.tagId 
        GROUP BY tags.id 
        ORDER BY photoCount DESC, tags.normalizedName ASC
        """
    )
    fun observeTagsWithCount(): Flow<List<TagWithCount>>

    @Transaction
    @Query("SELECT * FROM photos WHERE id = :id LIMIT 1")
    suspend fun getPhotoWithTagsById(id: Long): PhotoWithTags?

    @Query("SELECT * FROM photos WHERE sha256 = :sha256 LIMIT 1")
    suspend fun getPhotoBySha256(sha256: String): PhotoEntity?

    @Query("SELECT * FROM photos")
    suspend fun getAllPhotos(): List<PhotoEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPhoto(photo: PhotoEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("SELECT * FROM tags WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun findTagByNormalizedName(normalizedName: String): TagEntity?

    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    suspend fun findTagById(id: Long): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: PhotoTagCrossRef): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRefs(crossRefs: List<PhotoTagCrossRef>): List<Long>

    @Query("DELETE FROM photo_tag_cross_ref WHERE photoId = :photoId AND tagId = :tagId")
    suspend fun deleteCrossRef(photoId: Long, tagId: Long): Int

    @Query("DELETE FROM tags WHERE id NOT IN (SELECT tagId FROM photo_tag_cross_ref)")
    suspend fun pruneUnusedTags(): Int

    @Query("DELETE FROM photos WHERE id IN (:photoIds)")
    suspend fun deletePhotosByIds(photoIds: List<Long>): Int

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTagById(tagId: Long): Int

    @Query("UPDATE photos SET note = :note WHERE id = :photoId")
    suspend fun updateNote(photoId: Long, note: String): Int

    @Query("UPDATE tags SET name = :name, normalizedName = :normalizedName WHERE id = :id")
    suspend fun updateTag(id: Long, name: String, normalizedName: String): Int

    @Query(
        """
        INSERT OR IGNORE INTO photo_tag_cross_ref (photoId, tagId)
        SELECT photoId, :targetTagId FROM photo_tag_cross_ref WHERE tagId = :sourceTagId
        """
    )
    suspend fun repointCrossRefs(sourceTagId: Long, targetTagId: Long)

    /**
     * Upsert a tag: insert or ignore, then return the existing/new tag ID.
     */
    @Transaction
    suspend fun upsertTag(name: String, normalizedName: String): Long {
        insertTag(TagEntity(name = name, normalizedName = normalizedName))
        val existing = findTagByNormalizedName(normalizedName)
            ?: throw IllegalStateException("Tag with normalizedName '$normalizedName' must exist after insert")
        return existing.id
    }

    /**
     * Inserts a photo and links the provided parsed tags atomically.
     */
    @Transaction
    suspend fun insertPhotoWithTags(photo: PhotoEntity, tags: List<TagResult.Valid>): Long {
        val photoId = insertPhoto(photo)
        for (tag in tags) {
            val tagId = upsertTag(tag.display, tag.normalized)
            insertCrossRef(PhotoTagCrossRef(photoId = photoId, tagId = tagId))
        }
        return photoId
    }

    /**
     * Re-inserts a photo (preserving ID and SHA) and links tags by normalized name.
     * Used for Undo delete per Section 7 & 8.
     */
    @Transaction
    suspend fun restorePhotoWithTags(photo: PhotoEntity, tagList: List<TagEntity>) {
        insertPhoto(photo)
        for (tag in tagList) {
            val tagId = upsertTag(tag.name, tag.normalizedName)
            insertCrossRef(PhotoTagCrossRef(photoId = photo.id, tagId = tagId))
        }
    }

    /**
     * Deletes photos and prunes unreferenced tags in a single transaction.
     */
    @Transaction
    suspend fun deletePhotosAndPrune(photoIds: List<Long>): Int {
        val deletedCount = deletePhotosByIds(photoIds)
        pruneUnusedTags()
        return deletedCount
    }

    /**
     * Removes a tag from a photo and prunes unreferenced tags.
     */
    @Transaction
    suspend fun removeTagFromPhotoAndPrune(photoId: Long, tagId: Long) {
        deleteCrossRef(photoId, tagId)
        pruneUnusedTags()
    }

    /**
     * Adds tags to multiple photos and links them.
     */
    @Transaction
    suspend fun addTagsToPhotos(photoIds: List<Long>, tags: List<TagResult.Valid>) {
        val tagIds = tags.map { upsertTag(it.display, it.normalized) }
        for (photoId in photoIds) {
            for (tagId in tagIds) {
                insertCrossRef(PhotoTagCrossRef(photoId = photoId, tagId = tagId))
            }
        }
    }

    /**
     * Removes a tag from multiple photos and prunes.
     */
    @Transaction
    suspend fun removeTagFromPhotosAndPrune(photoIds: List<Long>, tagId: Long) {
        for (photoId in photoIds) {
            deleteCrossRef(photoId, tagId)
        }
        pruneUnusedTags()
    }

    /**
     * Renames or merges a tag atomically per Section 6.3.
     */
    @Transaction
    suspend fun renameOrMergeTag(tagId: Long, newDisplay: String, newNormalized: String) {
        val currentTag = findTagById(tagId) ?: return
        if (currentTag.normalizedName == newNormalized) {
            // Same normalized name: update display casing only
            updateTag(tagId, newDisplay, newNormalized)
            return
        }

        val targetTag = findTagByNormalizedName(newNormalized)
        if (targetTag != null && targetTag.id != tagId) {
            // Another tag already exists with newNormalized -> MERGE
            repointCrossRefs(sourceTagId = tagId, targetTagId = targetTag.id)
            deleteTagById(tagId)
        } else {
            // No conflict: rename in place
            updateTag(tagId, newDisplay, newNormalized)
        }
    }
}
