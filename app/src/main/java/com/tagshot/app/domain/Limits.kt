package com.tagshot.app.domain

/**
 * Global operational and sizing limits for TagShot.
 *
 * All values are specified in Section 4.3 of the implementation specification.
 */
const val MAX_TAG_CODEPOINTS = 32
const val MAX_TAGS_PER_PHOTO = 20
const val MAX_NOTE_CHARS = 500
const val THUMB_MAX_EDGE = 512
const val THUMB_JPEG_QUALITY = 85
const val DETAIL_MAX_EDGE = 3072
const val MAX_ZOOM = 8f
const val DOUBLE_TAP_ZOOM = 3f
const val UNDO_WINDOW_MS = 8000L
const val LIBRARY_BUDGET = 20000

object Limits {
    const val MAX_TAG_CODEPOINTS = com.tagshot.app.domain.MAX_TAG_CODEPOINTS
    const val MAX_TAGS_PER_PHOTO = com.tagshot.app.domain.MAX_TAGS_PER_PHOTO
    const val MAX_NOTE_CHARS = com.tagshot.app.domain.MAX_NOTE_CHARS
    const val THUMB_MAX_EDGE = com.tagshot.app.domain.THUMB_MAX_EDGE
    const val THUMB_JPEG_QUALITY = com.tagshot.app.domain.THUMB_JPEG_QUALITY
    const val DETAIL_MAX_EDGE = com.tagshot.app.domain.DETAIL_MAX_EDGE
    const val MAX_ZOOM = com.tagshot.app.domain.MAX_ZOOM
    const val DOUBLE_TAP_ZOOM = com.tagshot.app.domain.DOUBLE_TAP_ZOOM
    const val UNDO_WINDOW_MS = com.tagshot.app.domain.UNDO_WINDOW_MS
    const val LIBRARY_BUDGET = com.tagshot.app.domain.LIBRARY_BUDGET
}

