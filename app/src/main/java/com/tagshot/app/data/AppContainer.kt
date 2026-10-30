package com.tagshot.app.data

import android.content.Context

interface AppContainer {
    val database: AppDatabase
    val fileManager: PhotoFileManager
    val importUseCase: ImportUseCase
    val orphanSweeper: OrphanSweeper
    val photoRepository: PhotoRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    override val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    override val fileManager: PhotoFileManager by lazy {
        PhotoFileManager(context)
    }

    override val importUseCase: ImportUseCase by lazy {
        ImportUseCase(context, fileManager, database.photoDao())
    }

    override val orphanSweeper: OrphanSweeper by lazy {
        OrphanSweeper(fileManager, database.photoDao())
    }

    override val photoRepository: PhotoRepository by lazy {
        DefaultPhotoRepository(database.photoDao(), fileManager, importUseCase, context)
    }
}
