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
