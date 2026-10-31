package com.tagshot.app.domain

enum class SortOrder {
    NEWEST,
    OLDEST,
    DATE_TAKEN_NEWEST,
    DATE_TAKEN_OLDEST,
    TAG_NAME
}

data class SortablePhoto(
    val id: Long,
    val addedAt: Long,
    val normalizedTags: Set<String>,
    val takenAt: Long? = null
)

object PhotoSorter {

    /**
     * Comparator for [SortOrder.NEWEST]:
     * Primary: `addedAt` descending.
     * Tie-break: `id` descending.
     */
    val newestComparator = Comparator<SortablePhoto> { a, b ->
        val addedCmp = b.addedAt.compareTo(a.addedAt)
        if (addedCmp != 0) addedCmp else b.id.compareTo(a.id)
    }

    /**
     * Comparator for [SortOrder.OLDEST]:
     * Primary: `addedAt` ascending.
     * Tie-break: `id` ascending.
     */
    val oldestComparator = Comparator<SortablePhoto> { a, b ->
        val addedCmp = a.addedAt.compareTo(b.addedAt)
        if (addedCmp != 0) addedCmp else a.id.compareTo(b.id)
    }

    /**
     * Comparator for [SortOrder.TAG_NAME]:
     * Primary: Lexicographically smallest `normalizedTag` among the photo's tags,
     * compared with [String.compareTo] (UTF-16 code unit order per Section 5.4).
     * Untagged photos are placed last.
     * Tie-break: `addedAt` descending, then `id` descending.
     */
    val tagNameComparator = Comparator<SortablePhoto> { a, b ->
        val keyA = a.normalizedTags.minOrNull()
        val keyB = b.normalizedTags.minOrNull()
        when {
            keyA == null && keyB != null -> 1 // untagged a goes after tagged b
            keyA != null && keyB == null -> -1 // tagged a goes before untagged b
            keyA != null && keyB != null -> {
                val tagCmp = keyA.compareTo(keyB)
                if (tagCmp != 0) {
                    tagCmp
                } else {
                    val addedCmp = b.addedAt.compareTo(a.addedAt)
                    if (addedCmp != 0) addedCmp else b.id.compareTo(a.id)
                }
            }
            else -> {
                // Both untagged
                val addedCmp = b.addedAt.compareTo(a.addedAt)
                if (addedCmp != 0) addedCmp else b.id.compareTo(a.id)
            }
        }
    }

    /**
     * Comparator for [SortOrder.DATE_TAKEN_NEWEST]:
     * Primary: `takenAt ?: addedAt` descending.
     * Tie-break: `id` descending.
     */
    val dateTakenNewestComparator = Comparator<SortablePhoto> { a, b ->
        val timeA = a.takenAt ?: a.addedAt
        val timeB = b.takenAt ?: b.addedAt
        val timeCmp = timeB.compareTo(timeA)
        if (timeCmp != 0) timeCmp else b.id.compareTo(a.id)
    }

    /**
     * Comparator for [SortOrder.DATE_TAKEN_OLDEST]:
     * Primary: `takenAt ?: addedAt` ascending.
     * Tie-break: `id` ascending.
     */
    val dateTakenOldestComparator = Comparator<SortablePhoto> { a, b ->
        val timeA = a.takenAt ?: a.addedAt
        val timeB = b.takenAt ?: b.addedAt
        val timeCmp = timeA.compareTo(timeB)
        if (timeCmp != 0) timeCmp else a.id.compareTo(b.id)
    }

    fun getComparator(order: SortOrder): Comparator<SortablePhoto> = when (order) {
        SortOrder.NEWEST -> newestComparator
        SortOrder.OLDEST -> oldestComparator
        SortOrder.DATE_TAKEN_NEWEST -> dateTakenNewestComparator
        SortOrder.DATE_TAKEN_OLDEST -> dateTakenOldestComparator
        SortOrder.TAG_NAME -> tagNameComparator
    }

    /**
     * Sorts a list of photos according to [order].
     * Produces a deterministic total order with no possible ties.
     */
    fun <T> sort(
        photos: List<T>,
        order: SortOrder,
        toSortable: (T) -> SortablePhoto
    ): List<T> {
        val comparator = getComparator(order)
        return photos.sortedWith { o1, o2 ->
            comparator.compare(toSortable(o1), toSortable(o2))
        }
    }
}
