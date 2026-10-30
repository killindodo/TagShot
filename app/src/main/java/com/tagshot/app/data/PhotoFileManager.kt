package com.tagshot.app.data

import android.content.Context
import java.io.File
import java.util.UUID

open class PhotoFileManager(
    private val context: Context,
    private val baseDir: File = context.filesDir
) {

    val origDir: File get() = File(baseDir, "photos/orig").apply { mkdirs() }
    val thumbDir: File get() = File(baseDir, "photos/thumbs").apply { mkdirs() }
    val cameraDir: File get() = File(context.cacheDir, "camera").apply { mkdirs() }

    fun resolve(relativePath: String): File {
        return File(baseDir, relativePath)
    }

    fun deleteRelative(relativePath: String): Boolean {
        val file = resolve(relativePath)
        return if (file.exists()) file.delete() else true
    }

    fun createCameraFile(): File {
        cameraDir.mkdirs()
        return File(cameraDir, "${UUID.randomUUID()}.jpg")
    }
}
