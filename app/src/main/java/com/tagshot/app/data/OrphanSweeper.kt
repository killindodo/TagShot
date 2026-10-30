package com.tagshot.app.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class OrphanSweeper(
    private val fileManager: PhotoFileManager,
    private val photoDao: PhotoDao
) {
    companion object {
        private const val TAG = "OrphanSweeper"
    }

    suspend fun sweep(): Int = withContext(Dispatchers.IO) {
        var deletedCount = 0

        // 1. Clean camera cache
        fileManager.cameraDir.listFiles()?.forEach { file ->
            if (file.isFile && file.delete()) {
                deletedCount++
                Log.d(TAG, "Deleted camera cache file: ${file.name}")
            }
        }

        // 2. Fetch all known referenced paths from DB
        val allPhotos = photoDao.getAllPhotos()
        val referencedOrigNames = allPhotos.map { it.originalPath.substringAfterLast('/') }.toSet()
        val referencedThumbNames = allPhotos.map { it.thumbPath.substringAfterLast('/') }.toSet()

        Log.d(TAG, "Sweep starting: ${allPhotos.size} photos in DB")

        // 3. Sweep orig/ directory
        fileManager.origDir.listFiles()?.forEach { file ->
            if (file.isFile && !referencedOrigNames.contains(file.name)) {
                if (file.delete()) {
                    deletedCount++
                    Log.d(TAG, "Deleted orphan original: ${file.name}")
                }
            }
        }

        // 4. Sweep thumbs/ directory
        fileManager.thumbDir.listFiles()?.forEach { file ->
            if (file.isFile && !referencedThumbNames.contains(file.name)) {
                if (file.delete()) {
                    deletedCount++
                    Log.d(TAG, "Deleted orphan thumbnail: ${file.name}")
                }
            }
        }

        Log.d(TAG, "Sweep finished. Total deleted: $deletedCount")
        deletedCount
    }
}
