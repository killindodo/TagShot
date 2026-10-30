package com.tagshot.app.data

data class DeletedPhotoBackup(
    val photo: PhotoEntity,
    val tags: List<TagEntity>
)
