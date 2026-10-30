package com.tagshot.app.data

data class TagWithCount(
    val id: Long,
    val name: String,
    val normalizedName: String,
    val photoCount: Int
)
