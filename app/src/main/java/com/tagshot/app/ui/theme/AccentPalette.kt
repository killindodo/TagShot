package com.tagshot.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Data class providing light and dark color mappings for TagShot Pro accent themes.
 */
data class PaletteColorScheme(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val accentGlow: Color
)

/**
 * 6-palette Accent Theme Engine for TagShot Pro.
 * Provides curated aerospace, minimalist, and luminous colorways.
 */
enum class AccentPalette(
    val title: String,
    val subtitle: String,
    val shortName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val containerColor: Color,
    val darkScheme: PaletteColorScheme,
    val lightScheme: PaletteColorScheme
) {
    TITANIUM_SAPPHIRE(
        title = "Titanium Sapphire",
        subtitle = "Electric blue aerospace precision",
        shortName = "Sapphire",
        primaryColor = Color(0xFF38BDF8),
        secondaryColor = Color(0xFF0284C7),
        containerColor = Color(0xFF0C4A6E),
        darkScheme = PaletteColorScheme(
            primary = Color(0xFF38BDF8),
            onPrimary = ObsidianBg,
            primaryContainer = Color(0xFF0C4A6E),
            onPrimaryContainer = Color(0xFFE0F2FE),
            secondary = Color(0xFF0284C7),
            onSecondary = ObsidianBg,
            secondaryContainer = Color(0xFF0369A1).copy(alpha = 0.45f),
            onSecondaryContainer = Color(0xFFBAE6FD),
            tertiary = Color(0xFF34D399),
            onTertiary = ObsidianBg,
            accentGlow = Color(0xFF38BDF8)
        ),
        lightScheme = PaletteColorScheme(
            primary = Color(0xFF0284C7),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE0F2FE),
            onPrimaryContainer = Color(0xFF0C4A6E),
            secondary = Color(0xFF0369A1),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFBAE6FD),
            onSecondaryContainer = Color(0xFF075985),
            tertiary = Color(0xFF059669),
            onTertiary = Color.White,
            accentGlow = Color(0xFF0284C7)
        )
    ),

    CUPERTINO_AMBER(
        title = "Cupertino Amber",
        subtitle = "Warm luminous California sun",
        shortName = "Amber",
        primaryColor = Color(0xFFF59E0B),
        secondaryColor = Color(0xFFD97706),
        containerColor = Color(0xFF78350F),
        darkScheme = PaletteColorScheme(
            primary = Color(0xFFF59E0B),
            onPrimary = ObsidianBg,
            primaryContainer = Color(0xFF78350F),
            onPrimaryContainer = Color(0xFFFEF3C7),
            secondary = Color(0xFFD97706),
            onSecondary = ObsidianBg,
            secondaryContainer = Color(0xFF92400E).copy(alpha = 0.45f),
            onSecondaryContainer = Color(0xFFFDE68A),
            tertiary = Color(0xFFFB923C),
            onTertiary = ObsidianBg,
            accentGlow = Color(0xFFF59E0B)
        ),
        lightScheme = PaletteColorScheme(
            primary = Color(0xFFD97706),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFEF3C7),
            onPrimaryContainer = Color(0xFF78350F),
            secondary = Color(0xFFB45309),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFFDE68A),
            onSecondaryContainer = Color(0xFF78350F),
            tertiary = Color(0xFFEA580C),
            onTertiary = Color.White,
            accentGlow = Color(0xFFD97706)
        )
    ),

    CYBER_EMERALD(
        title = "Cyber Emerald",
        subtitle = "High-energy futuristic jade",
        shortName = "Emerald",
        primaryColor = Color(0xFF10B981),
        secondaryColor = Color(0xFF059669),
        containerColor = Color(0xFF064E3B),
        darkScheme = PaletteColorScheme(
            primary = Color(0xFF10B981),
            onPrimary = ObsidianBg,
            primaryContainer = Color(0xFF064E3B),
            onPrimaryContainer = Color(0xFFD1FAE5),
            secondary = Color(0xFF059669),
            onSecondary = ObsidianBg,
            secondaryContainer = Color(0xFF047857).copy(alpha = 0.45f),
            onSecondaryContainer = Color(0xFFA7F3D0),
            tertiary = Color(0xFF34D399),
            onTertiary = ObsidianBg,
            accentGlow = Color(0xFF10B981)
        ),
        lightScheme = PaletteColorScheme(
            primary = Color(0xFF059669),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD1FAE5),
            onPrimaryContainer = Color(0xFF064E3B),
            secondary = Color(0xFF047857),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFA7F3D0),
            onSecondaryContainer = Color(0xFF065F46),
            tertiary = Color(0xFF0D9488),
            onTertiary = Color.White,
            accentGlow = Color(0xFF059669)
        )
    ),

    MIDNIGHT_AMETHYST(
        title = "Midnight Amethyst",
        subtitle = "Deep cosmic purple nebula",
        shortName = "Amethyst",
        primaryColor = Color(0xFFA855F7),
        secondaryColor = Color(0xFF9333EA),
        containerColor = Color(0xFF581C87),
        darkScheme = PaletteColorScheme(
            primary = Color(0xFFA855F7),
            onPrimary = ObsidianBg,
            primaryContainer = Color(0xFF581C87),
            onPrimaryContainer = Color(0xFFF3E8FF),
            secondary = Color(0xFF9333EA),
            onSecondary = ObsidianBg,
            secondaryContainer = Color(0xFF7E22CE).copy(alpha = 0.45f),
            onSecondaryContainer = Color(0xFFE9D5FF),
            tertiary = Color(0xFFC084FC),
            onTertiary = ObsidianBg,
            accentGlow = Color(0xFFA855F7)
        ),
        lightScheme = PaletteColorScheme(
            primary = Color(0xFF7E22CE),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFF3E8FF),
            onPrimaryContainer = Color(0xFF581C87),
            secondary = Color(0xFF9333EA),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE9D5FF),
            onSecondaryContainer = Color(0xFF6B21A8),
            tertiary = Color(0xFF6366F1),
            onTertiary = Color.White,
            accentGlow = Color(0xFF7E22CE)
        )
    ),

    CRIMSON_DUSK(
        title = "Crimson Dusk",
        subtitle = "Vibrant twilight ruby scarlet",
        shortName = "Crimson",
        primaryColor = Color(0xFFF43F5E),
        secondaryColor = Color(0xFFE11D48),
        containerColor = Color(0xFF881337),
        darkScheme = PaletteColorScheme(
            primary = Color(0xFFF43F5E),
            onPrimary = ObsidianBg,
            primaryContainer = Color(0xFF881337),
            onPrimaryContainer = Color(0xFFFFE4E6),
            secondary = Color(0xFFE11D48),
            onSecondary = ObsidianBg,
            secondaryContainer = Color(0xFFBE123C).copy(alpha = 0.45f),
            onSecondaryContainer = Color(0xFFFECDD3),
            tertiary = Color(0xFFFB7185),
            onTertiary = ObsidianBg,
            accentGlow = Color(0xFFF43F5E)
        ),
        lightScheme = PaletteColorScheme(
            primary = Color(0xFFE11D48),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFE4E6),
            onPrimaryContainer = Color(0xFF881337),
            secondary = Color(0xFFBE123C),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFFECDD3),
            onSecondaryContainer = Color(0xFF9F1239),
            tertiary = Color(0xFFD946EF),
            onTertiary = Color.White,
            accentGlow = Color(0xFFE11D48)
        )
    ),

    OBSIDIAN_MONO(
        title = "Obsidian Monochrome",
        subtitle = "Minimalist stealth chrome slate",
        shortName = "Obsidian",
        primaryColor = Color(0xFFE2E8F0),
        secondaryColor = Color(0xFF94A3B8),
        containerColor = Color(0xFF334155),
        darkScheme = PaletteColorScheme(
            primary = Color(0xFFE2E8F0),
            onPrimary = ObsidianBg,
            primaryContainer = Color(0xFF334155),
            onPrimaryContainer = Color(0xFFF8FAFC),
            secondary = Color(0xFF94A3B8),
            onSecondary = ObsidianBg,
            secondaryContainer = Color(0xFF475569).copy(alpha = 0.45f),
            onSecondaryContainer = Color(0xFFCBD5E1),
            tertiary = Color(0xFF64748B),
            onTertiary = ObsidianBg,
            accentGlow = Color(0xFFE2E8F0)
        ),
        lightScheme = PaletteColorScheme(
            primary = Color(0xFF334155),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFF1F5F9),
            onPrimaryContainer = Color(0xFF0F172A),
            secondary = Color(0xFF475569),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE2E8F0),
            onSecondaryContainer = Color(0xFF1E293B),
            tertiary = Color(0xFF64748B),
            onTertiary = Color.White,
            accentGlow = Color(0xFF334155)
        )
    );

    companion object {
        fun fromNameOrDefault(name: String?): AccentPalette {
            if (name.isNullOrBlank()) return TITANIUM_SAPPHIRE
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: TITANIUM_SAPPHIRE
        }
    }
}
