package com.tagshot.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class OrphanSweeper(
    private val fileManager: PhotoFileManager,
    private val photoDao: PhotoDao
) {

    suspend fun sweep(): Int = withContext(Dispatchers.IO) {
        var deletedCount = 0

        // 1. Clean camera cache
        fileManager.cameraDir.listFiles()?.forEach { file ->
            if (file.isFile && file.delete()) {
                deletedCount++
            }
        }

        // 2. Fetch all known referenced paths from DB
        val allPhotos = photoDao.getAllPhotos()
        val referencedOrigFiles = allPhotos.map { File(fileManager.resolve(it.originalPath).name) }.toSet()
        val referencedThumbFiles = allPhotos.map { File(fileManager.resolve(it.thumbPath).name) }.toSet()

        // 3. Sweep orig/ directory
        fileManager.origDir.listFiles()?.forEach { file ->
            if (file.isFile && !referencedOrigFiles.contains(File(file.name))) {
                if (file.delete()) {
                    deletedCount++
                }
            }
        }

        // 4. Sweep thumbs/ directory
        fileManager.thumbDir.listFiles()?.forEach { file ->
            if (file.isFile && !referencedThumbFiles.contains(File(file.name))) {
                if (file.delete()) {
                    deletedCount++
                }
            }
        }

        deletedCount
    }
}
