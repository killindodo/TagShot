package com.tagshot.app.domain

import java.nio.charset.StandardCharsets
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

data class TagColorScheme(
    val backgroundArgb: Int,
    val textArgb: Int,
    val red: Int,
    val green: Int,
    val blue: Int,
    val hue: Int
)

object TagColor {

    const val BLACK_ARGB = 0xFF000000.toInt()
    const val WHITE_ARGB = 0xFFFFFFFF.toInt()

    const val SATURATION = 0.55f
    const val LIGHTNESS_LIGHT = 0.40f
    const val LIGHTNESS_DARK = 0.70f

    /**
     * 32-bit FNV-1a hash algorithm.
     * Offset basis: 0x811C9DC5
     * Prime: 0x01000193
     */
    fun fnv1a32(bytes: ByteArray): Int {
        var h = 0x811C9DC5.toInt()
        for (b in bytes) {
            h = h xor (b.toInt() and 0xFF)
            h *= 0x01000193
        }
        return h
    }

    /**
     * Derives a deterministic hue in [0, 359] from a tag's normalized name.
     */
    fun hue(normalizedName: String): Int {
        val bytes = normalizedName.toByteArray(StandardCharsets.UTF_8)
        val hash = fnv1a32(bytes).toLong() and 0xFFFFFFFFL
        return (hash % 360).toInt()
    }

    /**
     * Converts HSL to sRGB integers in [0, 255].
     * @param h Hue in [0, 360)
     * @param s Saturation in [0, 1]
     * @param l Lightness in [0, 1]
     */
    fun hslToSrgb(h: Int, s: Float, l: Float): Triple<Int, Int, Int> {
        val normalizedHue = ((h % 360) + 360) % 360
        val c = (1f - abs(2f * l - 1f)) * s
        val hPrime = normalizedHue / 60f
        val x = c * (1f - abs((hPrime % 2f) - 1f))
        val m = l - c / 2f

        val (r1, g1, b1) = when {
            normalizedHue < 60 -> Triple(c, x, 0f)
            normalizedHue < 120 -> Triple(x, c, 0f)
            normalizedHue < 180 -> Triple(0f, c, x)
            normalizedHue < 240 -> Triple(0f, x, c)
            normalizedHue < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        val r = ((r1 + m) * 255f).roundToInt().coerceIn(0, 255)
        val g = ((g1 + m) * 255f).roundToInt().coerceIn(0, 255)
        val b = ((b1 + m) * 255f).roundToInt().coerceIn(0, 255)

        return Triple(r, g, b)
    }

    /**
     * Converts an sRGB 8-bit channel to linear light value.
     * if c <= 0.04045 then c / 12.92 else ((c + 0.055) / 1.055) ^ 2.4
     */
    fun srgbToLinear(c255: Int): Double {
        val c = c255 / 255.0
        return if (c <= 0.04045) {
            c / 12.92
        } else {
            ((c + 0.055) / 1.055).pow(2.4)
        }
    }

    /**
     * Computes the relative luminance of an sRGB color per WCAG 2.1:
     * L = 0.2126 * R_lin + 0.7152 * G_lin + 0.0722 * B_lin
     */
    fun relativeLuminance(r: Int, g: Int, b: Int): Double {
        val rLin = srgbToLinear(r)
        val gLin = srgbToLinear(g)
        val bLin = srgbToLinear(b)
        return 0.2126 * rLin + 0.7152 * gLin + 0.0722 * bLin
    }

    /**
     * Computes the WCAG contrast ratio between two luminance values:
     * (max(L1, L2) + 0.05) / (min(L1, L2) + 0.05)
     */
    fun contrastRatio(l1: Double, l2: Double): Double {
        val high = max(l1, l2)
        val low = min(l1, l2)
        return (high + 0.05) / (low + 0.05)
    }

    /**
     * Chooses black or white text color based on which has the higher contrast ratio against the background.
     * Black luminance = 0.0, White luminance = 1.0.
     */
    fun textColorForBackground(r: Int, g: Int, b: Int): Int {
        val bgLuminance = relativeLuminance(r, g, b)
        val contrastWithBlack = contrastRatio(bgLuminance, 0.0)
        val contrastWithWhite = contrastRatio(bgLuminance, 1.0)
        return if (contrastWithBlack >= contrastWithWhite) BLACK_ARGB else WHITE_ARGB
    }

    /**
     * Packs (r, g, b) into an opaque 32-bit ARGB Int (0xFFRRGGBB).
     */
    fun packArgb(r: Int, g: Int, b: Int): Int {
        return (0xFF shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
    }

    /**
     * Derives the complete color scheme for a tag in either light or dark theme.
     */
    fun getColorScheme(normalizedName: String, isDarkTheme: Boolean): TagColorScheme {
        val h = hue(normalizedName)
        val l = if (isDarkTheme) LIGHTNESS_DARK else LIGHTNESS_LIGHT
        val (r, g, b) = hslToSrgb(h, SATURATION, l)
        val bgArgb = packArgb(r, g, b)
        val textArgb = textColorForBackground(r, g, b)
        return TagColorScheme(
            backgroundArgb = bgArgb,
            textArgb = textArgb,
            red = r,
            green = g,
            blue = b,
            hue = h
        )
    }
}
