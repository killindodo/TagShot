package com.tagshot.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Visual styling variations for the FastScroll timeline scrubber.
 */
enum class ScrubberStyle(val displayName: String, val description: String) {
    CUPERTINO_CAPSULE("Cupertino Capsule", "Minimal translucent floating pill bar"),
    CYBER_NEON_BAR("Cyber Neon Bar", "Luminous laser beam with glowing slider head"),
    RETRO_WINDOWS("Retro Windows 95", "Classic 3D beveled scrollbar with tactile knurled grip"),
    MINIMAL_NOTCH("Minimal Edge Notch", "Discreet screen bezel tab expanding on touch");

    companion object {
        fun fromNameOrDefault(name: String?): ScrubberStyle {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: CUPERTINO_CAPSULE
        }
    }
}

/**
 * Physical width/thickness of the scrubber scroll track.
 */
enum class ScrubberWidth(val displayName: String, val widthDp: Dp) {
    ULTRA_THIN("Ultra Thin (3.5dp)", 3.5.dp),
    STANDARD("Standard (6dp)", 6.dp),
    ACCESSIBLE_BOLD("Accessible Bold (10dp)", 10.dp);

    companion object {
        fun fromNameOrDefault(name: String?): ScrubberWidth {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: STANDARD
        }
    }
}

/**
 * Aesthetics for the floating date/time magnifier bubble.
 */
enum class ScrubberBubbleStyle(val displayName: String, val description: String) {
    FROSTED_GLASS_LENS("Frosted Acrylic Lens", "Translucent glass bubble with specular halo"),
    WINDOWS_95_TOOLTIP("Windows 95 Tooltip", "Classic pale yellow box with drop shadow"),
    CYBER_HUD("Cyber Monospace HUD", "Emerald/cyan brackets [ OCT 2026 ]"),
    MATERIAL_PILL("Material 3 Pill", "Modern elevated pill badge with calendar icon");

    companion object {
        fun fromNameOrDefault(name: String?): ScrubberBubbleStyle {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: FROSTED_GLASS_LENS
        }
    }
}
