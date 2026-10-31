package com.tagshot.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Geometric shape variations for tag chips and photo badges.
 */
enum class TagShape(val displayName: String, val description: String) {
    PILL("Capsule Pill", "Fully rounded ergonomic stadium shape"),
    SQUIRCLE("Apple Squircle", "Smooth continuous curvature container"),
    SHARP_RECT("Architectural Sharp", "Clean minimalist Bauhaus edges"),
    CHAMFERED_CYBER("Cyber Cut-Corner", "Futuristic Sci-Fi angled chamfers"),
    LEAF_ASYMMETRIC("Asymmetric Leaf", "Organic diagonal rounded leaf silhouette"),
    MINIMAL_DOT("Status Dot", "Compact jewel dot indicator with micro text");

    fun toComposeShape(): Shape = when (this) {
        PILL -> CircleShape
        SQUIRCLE -> RoundedCornerShape(8.dp)
        SHARP_RECT -> RoundedCornerShape(2.dp)
        CHAMFERED_CYBER -> CutCornerShape(6.dp)
        LEAF_ASYMMETRIC -> RoundedCornerShape(
            topStart = 10.dp,
            bottomEnd = 10.dp,
            topEnd = 2.dp,
            bottomStart = 2.dp
        )
        MINIMAL_DOT -> RoundedCornerShape(5.dp)
    }

    companion object {
        fun fromNameOrDefault(name: String?): TagShape {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: SQUIRCLE
        }
    }
}

/**
 * Anchor position of tag badges overlaying photos in the gallery.
 */
enum class TagPosition(val displayName: String) {
    BOTTOM_LEFT("Bottom Left"),
    BOTTOM_RIGHT("Bottom Right"),
    TOP_LEFT("Top Left"),
    TOP_RIGHT("Top Right");

    companion object {
        fun fromNameOrDefault(name: String?): TagPosition {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: BOTTOM_LEFT
        }
    }
}

/**
 * Curated color palette presets for tags.
 */
enum class TagPalettePreset(
    val displayName: String,
    val description: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val backgroundColor: Color,
    val textColor: Color
) {
    DYNAMIC_AUTO(
        displayName = "Dynamic TagColor",
        description = "Intelligent hash-based per-tag palette",
        primaryColor = Color(0xFF38BDF8),
        secondaryColor = Color(0xFF818CF8),
        backgroundColor = Color(0x3338BDF8),
        textColor = Color(0xFFE0F2FE)
    ),
    NEON_CYBER(
        displayName = "Neon Cyberpunk",
        description = "Laser cyan, hot magenta, and electric glow",
        primaryColor = Color(0xFF00F0FF),
        secondaryColor = Color(0xFFFF007F),
        backgroundColor = Color(0x3D00F0FF),
        textColor = Color(0xFFE0FFFF)
    ),
    PASTEL_DREAM(
        displayName = "Pastel Dream",
        description = "Calming lavender, peach puff, and mint",
        primaryColor = Color(0xFFE0B0FF),
        secondaryColor = Color(0xFFFFD1DC),
        backgroundColor = Color(0x38E0B0FF),
        textColor = Color(0xFFFDF4FF)
    ),
    MONOCHROME_SLATE(
        displayName = "Monochrome Slate",
        description = "Carbon black, titanium slate, and stark silver",
        primaryColor = Color(0xFFE2E8F0),
        secondaryColor = Color(0xFF94A3B8),
        backgroundColor = Color(0x40334155),
        textColor = Color(0xFFF8FAFC)
    ),
    GOLD_LUXURY(
        displayName = "Gold Luxury",
        description = "Warm champagne, metallic gold, and obsidian",
        primaryColor = Color(0xFFF59E0B),
        secondaryColor = Color(0xFFD4AF37),
        backgroundColor = Color(0x4078350F),
        textColor = Color(0xFFFEF3C7)
    ),
    CUPERTINO_FROSTED(
        displayName = "Cupertino Frosted",
        description = "Translucent liquid acrylic with specular highlights",
        primaryColor = Color(0xFFFFFFFF),
        secondaryColor = Color(0xFFCBD5E1),
        backgroundColor = Color(0x33FFFFFF),
        textColor = Color(0xFFFFFFFF)
    ),
    RETRO_AMBER(
        displayName = "Retro Amber CRT",
        description = "Classic monochrome phosphor cathode ray glow",
        primaryColor = Color(0xFFFFB000),
        secondaryColor = Color(0xFFFF8C00),
        backgroundColor = Color(0x38451A03),
        textColor = Color(0xFFFFFBEB)
    ),
    HOLOGRAPHIC_SPECTRUM(
        displayName = "Holographic Spectrum",
        description = "Iridescent multi-color reflective gradient",
        primaryColor = Color(0xFF38BDF8),
        secondaryColor = Color(0xFFEC4899),
        backgroundColor = Color(0x3D8B5CF6),
        textColor = Color(0xFFFDF4FF)
    );

    companion object {
        fun fromNameOrDefault(name: String?): TagPalettePreset {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DYNAMIC_AUTO
        }
    }
}

/**
 * Dynamic moving animations for tag badges and chips.
 */
enum class TagAnimation(val displayName: String, val description: String) {
    NONE("Static", "Clean and battery efficient without motion"),
    PULSE_GLOW("Breathing Glow", "Organic rhythmic specular border radiance"),
    SHIMMER_SWEEP("Shimmer Ribbon", "Diagonal specular light beam gliding across the chip"),
    FLOAT_DRIFT("Floating Levitation", "Gentle vertical micro-drift wave motion");

    companion object {
        fun fromNameOrDefault(name: String?): TagAnimation {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: NONE
        }
    }
}
