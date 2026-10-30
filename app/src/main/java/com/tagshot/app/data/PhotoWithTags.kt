package com.tagshot.app.data

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class PhotoWithTags(
    @Embedded
    val photo: PhotoEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PhotoTagCrossRef::class,
            parentColumn = "photoId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)
