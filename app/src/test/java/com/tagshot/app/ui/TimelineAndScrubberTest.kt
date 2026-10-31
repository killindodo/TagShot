package com.tagshot.app.ui

import androidx.compose.ui.unit.dp
import com.tagshot.app.data.PhotoEntity
import com.tagshot.app.data.PhotoWithTags
import com.tagshot.app.ui.components.formatMonthYear
import com.tagshot.app.ui.components.formatTimelineHeader
import com.tagshot.app.ui.components.groupPhotosByTimeline
import com.tagshot.app.ui.theme.Alpha
import com.tagshot.app.ui.theme.Borders
import com.tagshot.app.ui.theme.CornerRadius
import com.tagshot.app.ui.theme.Elevation
import com.tagshot.app.ui.theme.Spacing
import com.tagshot.app.ui.theme.TagShotThemeTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TimelineAndScrubberTest {

    @Test
    fun testThemeTokensSpecification() {
        // Spacing: xxs = 2.dp, xs = 4.dp, sm = 8.dp, md = 12.dp, lg = 16.dp, xl = 24.dp, xxl = 32.dp
        assertEquals(2.dp, TagShotThemeTokens.Spacing.xxs)
        assertEquals(4.dp, TagShotThemeTokens.Spacing.xs)
        assertEquals(8.dp, TagShotThemeTokens.Spacing.sm)
        assertEquals(12.dp, TagShotThemeTokens.Spacing.md)
        assertEquals(16.dp, TagShotThemeTokens.Spacing.lg)
        assertEquals(24.dp, TagShotThemeTokens.Spacing.xl)
        assertEquals(32.dp, TagShotThemeTokens.Spacing.xxl)

        assertEquals(2.dp, Spacing.xxs)
        assertEquals(32.dp, Spacing.xxl)

        // CornerRadius: xs = 4.dp, sm = 8.dp, md = 12.dp, lg = 18.dp, full = 999.dp
        assertEquals(4.dp, TagShotThemeTokens.CornerRadius.xs)
        assertEquals(8.dp, TagShotThemeTokens.CornerRadius.sm)
        assertEquals(12.dp, TagShotThemeTokens.CornerRadius.md)
        assertEquals(18.dp, TagShotThemeTokens.CornerRadius.lg)
        assertEquals(999.dp, TagShotThemeTokens.CornerRadius.full)

        assertEquals(4.dp, CornerRadius.xs)
        assertEquals(999.dp, CornerRadius.full)

        // Borders: hairline = 0.5.dp, thin = 0.75.dp, regular = 1.dp, thick = 2.dp
        assertEquals(0.5.dp, TagShotThemeTokens.Borders.hairline)
        assertEquals(0.75.dp, TagShotThemeTokens.Borders.thin)
        assertEquals(1.dp, TagShotThemeTokens.Borders.regular)
        assertEquals(2.dp, TagShotThemeTokens.Borders.thick)

        assertEquals(0.5.dp, Borders.hairline)
        assertEquals(2.dp, Borders.thick)

        // Elevation: none = 0.dp, subtle = 2.dp, floating = 8.dp
        assertEquals(0.dp, TagShotThemeTokens.Elevation.none)
        assertEquals(2.dp, TagShotThemeTokens.Elevation.subtle)
        assertEquals(8.dp, TagShotThemeTokens.Elevation.floating)

        assertEquals(0.dp, Elevation.none)
        assertEquals(8.dp, Elevation.floating)

        // Alpha: glass = 0.65f, highlight = 0.15f, scrim = 0.85f
        assertEquals(0.65f, TagShotThemeTokens.Alpha.glass, 0.001f)
        assertEquals(0.15f, TagShotThemeTokens.Alpha.highlight, 0.001f)
        assertEquals(0.85f, TagShotThemeTokens.Alpha.scrim, 0.001f)

        assertEquals(0.65f, Alpha.glass, 0.001f)
        assertEquals(0.85f, Alpha.scrim, 0.001f)
    }

    @Test
    fun testTimelineDateHeaders() {
        val zone = ZoneId.systemDefault()
        val now = LocalDate.of(2026, 10, 31).atStartOfDay(zone).toInstant().toEpochMilli()

        // Today
        val todayMillis = LocalDate.of(2026, 10, 31).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals("Today", formatTimelineHeader(todayMillis, now))

        // Yesterday
        val yesterdayMillis = LocalDate.of(2026, 10, 30).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals("Yesterday", formatTimelineHeader(yesterdayMillis, now))

        // October 2026 (earlier in the month)
        val octoberEarlier = LocalDate.of(2026, 10, 15).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals("October 2026", formatTimelineHeader(octoberEarlier, now))

        // Prior month
        val september = LocalDate.of(2026, 9, 20).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals("September 2026", formatTimelineHeader(september, now))

        // Prior year
        val priorYear = LocalDate.of(2025, 12, 25).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals("December 2025", formatTimelineHeader(priorYear, now))

        // Undated
        assertEquals("Undated", formatTimelineHeader(0L, now))
    }

    @Test
    fun testGroupPhotosByTimeline() {
        val zone = ZoneId.systemDefault()
        val tToday = LocalDate.of(2026, 10, 31).atStartOfDay(zone).toInstant().toEpochMilli()
        val tYesterday = LocalDate.of(2026, 10, 30).atStartOfDay(zone).toInstant().toEpochMilli()
        val tOctober = LocalDate.of(2026, 10, 5).atStartOfDay(zone).toInstant().toEpochMilli()

        val p1 = PhotoWithTags(
            photo = PhotoEntity(id = 1L, originalPath = "1", thumbPath = "1", sha256 = "1", mimeType = "image/jpeg", width = 10, height = 10, sizeBytes = 100, addedAt = tToday, takenAt = null),
            tags = emptyList()
        )
        val p2 = PhotoWithTags(
            photo = PhotoEntity(id = 2L, originalPath = "2", thumbPath = "2", sha256 = "2", mimeType = "image/jpeg", width = 10, height = 10, sizeBytes = 100, addedAt = tToday, takenAt = null),
            tags = emptyList()
        )
        val p3 = PhotoWithTags(
            photo = PhotoEntity(id = 3L, originalPath = "3", thumbPath = "3", sha256 = "3", mimeType = "image/jpeg", width = 10, height = 10, sizeBytes = 100, addedAt = tYesterday, takenAt = null),
            tags = emptyList()
        )
        val p4 = PhotoWithTags(
            photo = PhotoEntity(id = 4L, originalPath = "4", thumbPath = "4", sha256 = "4", mimeType = "image/jpeg", width = 10, height = 10, sizeBytes = 100, addedAt = tOctober, takenAt = null),
            tags = emptyList()
        )

        val groups = groupPhotosByTimeline(listOf(p1, p2, p3, p4))
        assertEquals(3, groups.size)
        assertEquals(listOf(p1, p2), groups[0].photos)
        assertEquals(listOf(p3), groups[1].photos)
        assertEquals(listOf(p4), groups[2].photos)
    }

    @Test
    fun testFastScrollScrubberMonthYearFormatter() {
        val zone = ZoneId.systemDefault()
        val oct2026 = LocalDate.of(2026, 10, 15).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals("October 2026", formatMonthYear(oct2026))

        val jan2025 = LocalDate.of(2025, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals("January 2025", formatMonthYear(jan2025))

        assertEquals("", formatMonthYear(0L))
    }
}
