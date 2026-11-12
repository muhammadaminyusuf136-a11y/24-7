package uz.reja24.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import uz.reja24.app.domain.model.BackgroundStyle
import uz.reja24.app.domain.model.ThemeMode

/** RejaColors'ni Material 3 ColorScheme'ga o'giradi (standart komponentlar ham to'g'ri rangda bo'lsin). */
private fun RejaColors.toColorScheme(): ColorScheme {
    val onAccent = Color.White
    return if (isDark) {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            secondary = textSecondary,
            onSecondary = background,
            background = background,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surface,
            onSurfaceVariant = textSecondary,
            surfaceContainer = surface,
            surfaceContainerHigh = surface,
            surfaceContainerLow = surface,
            outline = border,
            outlineVariant = border
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            secondary = textSecondary,
            onSecondary = surface,
            background = background,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = background,
            onSurfaceVariant = textSecondary,
            surfaceContainer = surface,
            surfaceContainerHigh = surface,
            surfaceContainerLow = surface,
            outline = border,
            outlineVariant = border
        )
    }
}

/**
 * 24/7Reja mavzusi. Barcha ekranlar shu funksiya ichida chiziladi.
 * themeMode — foydalanuvchi sozlamalarda tanlagan mavzu.
 */
@Composable
fun RejaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    backgroundStyle: BackgroundStyle = BackgroundStyle.PURE,
    content: @Composable () -> Unit
) {
    val rejaColors = resolveRejaColors(themeMode, backgroundStyle, isSystemInDarkTheme())

    // Joriy til arabchami — shriftni shunga qarab tanlaymiz
    val locales = LocalConfiguration.current.locales
    val isArabic = !locales.isEmpty && locales[0].language == "ar"
    val typography = remember(isArabic) { rejaTypography(isArabic) }

    CompositionLocalProvider(LocalRejaColors provides rejaColors) {
        MaterialTheme(
            colorScheme = rejaColors.toColorScheme(),
            typography = typography,
            shapes = RejaShapes,
            content = content
        )
    }
}

/**
 * Ranglar to'plamini tanlaydi.
 * Orqa fon uslubi o'z ranglariga ega: "Kuch" — to'q (qorong'i), "Nafosat" — yumshoq pushti.
 * "Sof" uslubida foydalanuvchi tanlagan mavzu (Oq / Qora / Kulrang / Tizim) ishlatiladi.
 */
fun resolveRejaColors(themeMode: ThemeMode, backgroundStyle: BackgroundStyle, systemDark: Boolean): RejaColors =
    when (backgroundStyle) {
        BackgroundStyle.POWER -> DarkRejaColors
        BackgroundStyle.GRACE -> GraceRejaColors
        BackgroundStyle.PURE -> when (themeMode) {
            ThemeMode.LIGHT -> LightRejaColors
            ThemeMode.DARK -> DarkRejaColors
            ThemeMode.GRAY -> GrayRejaColors
            ThemeMode.SYSTEM -> if (systemDark) DarkRejaColors else LightRejaColors
        }
    }

/** Qisqa murojaat: RejaTheme.colors.accent */
object RejaTheme {
    val colors: RejaColors
        @Composable
        @ReadOnlyComposable
        get() = LocalRejaColors.current
}
