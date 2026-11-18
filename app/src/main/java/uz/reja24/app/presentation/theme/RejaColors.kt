package uz.reja24.app.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Material 3 ranglaridan tashqari, 24/7Reja'ga xos qo'shimcha ranglar.
 * Ekranlarda `RejaTheme.colors.textMuted` kabi ishlatiladi.
 */
@Immutable
data class RejaColors(
    val background: Color,
    val surface: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentText: Color,
    val timelineEmpty: Color,
    /** Hijriy sana rangi — milodiydan ajralib tursin. */
    val hijri: Color,
    val isDark: Boolean
)

val LightRejaColors = RejaColors(
    background = LightBackground,
    surface = LightSurface,
    border = LightBorder,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textMuted = LightTextMuted,
    accent = LightAccent,
    accentText = LightAccentText,
    timelineEmpty = LightTimelineEmpty,
    hijri = LightHijri,
    isDark = false
)

val NavyRejaColors = RejaColors(
    background = NavyBackground,
    surface = NavySurface,
    border = NavyBorder,
    textPrimary = NavyTextPrimary,
    textSecondary = NavyTextSecondary,
    textMuted = NavyTextMuted,
    accent = NavyAccent,
    accentText = NavyAccentText,
    timelineEmpty = NavyTimelineEmpty,
    hijri = NavyHijri,
    isDark = true
)

val GreenRejaColors = RejaColors(
    background = GreenBackground,
    surface = GreenSurface,
    border = GreenBorder,
    textPrimary = GreenTextPrimary,
    textSecondary = GreenTextSecondary,
    textMuted = GreenTextMuted,
    accent = GreenAccent,
    accentText = GreenAccentText,
    timelineEmpty = GreenTimelineEmpty,
    hijri = GreenHijri,
    isDark = true
)

val GraceRejaColors = RejaColors(
    background = GraceBackground,
    surface = GraceSurface,
    border = GraceBorder,
    textPrimary = GraceTextPrimary,
    textSecondary = GraceTextSecondary,
    textMuted = GraceTextMuted,
    accent = GraceAccent,
    accentText = GraceAccentText,
    timelineEmpty = GraceTimelineEmpty,
    hijri = GraceHijri,
    isDark = true
)

val LocalRejaColors = staticCompositionLocalOf { LightRejaColors }
