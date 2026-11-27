package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.shared.resources.Res
import com.example.shared.resources.inter
import com.example.shared.resources.jetbrains_mono
import com.example.shared.resources.space_grotesk
import com.example.ui.model.AppThemeMode
import org.jetbrains.compose.resources.Font

val LocalInstrumentColors = staticCompositionLocalOf { LightInstrumentPalette }

val MaterialTheme.instrumentColors: InstrumentPalette
    @Composable get() = LocalInstrumentColors.current

@Composable
fun resolveIsDarkTheme(themeMode: AppThemeMode): Boolean {
    val systemDark = isSystemInDarkTheme()
    return when (themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
}

private val FieldInstrumentLightScheme = lightColorScheme(
    primary = LightInstrumentPalette.cobalt,
    onPrimary = Color.White,
    primaryContainer = LightInstrumentPalette.cobaltContainer,
    onPrimaryContainer = LightInstrumentPalette.cobaltOnContainer,
    secondary = LightInstrumentPalette.rxEmerald,
    onSecondary = Color.White,
    secondaryContainer = LightInstrumentPalette.rxEmeraldBg,
    onSecondaryContainer = LightInstrumentPalette.rxEmerald,
    tertiary = LightInstrumentPalette.txAmber,
    onTertiary = Color.White,
    tertiaryContainer = LightInstrumentPalette.txAmberBg,
    onTertiaryContainer = LightInstrumentPalette.txAmber,
    error = LightInstrumentPalette.alertCrimson,
    onError = Color.White,
    errorContainer = LightInstrumentPalette.alertCrimsonBg,
    onErrorContainer = LightInstrumentPalette.alertCrimson,
    background = LightInstrumentPalette.background,
    onBackground = LightInstrumentPalette.ink,
    surface = LightInstrumentPalette.surface,
    onSurface = LightInstrumentPalette.ink,
    surfaceVariant = LightInstrumentPalette.surfaceSubtle,
    onSurfaceVariant = LightInstrumentPalette.inkSecondary,
    outline = LightInstrumentPalette.border,
    outlineVariant = LightInstrumentPalette.borderStrong,
)

private val FieldInstrumentDarkScheme = darkColorScheme(
    primary = DarkInstrumentPalette.cobalt,
    onPrimary = Color(0xFF06162E),
    primaryContainer = DarkInstrumentPalette.cobaltContainer,
    onPrimaryContainer = DarkInstrumentPalette.cobaltOnContainer,
    secondary = DarkInstrumentPalette.rxEmerald,
    onSecondary = Color(0xFF05241A),
    secondaryContainer = DarkInstrumentPalette.rxEmeraldBg,
    onSecondaryContainer = DarkInstrumentPalette.rxEmerald,
    tertiary = DarkInstrumentPalette.txAmber,
    onTertiary = Color(0xFF281904),
    tertiaryContainer = DarkInstrumentPalette.txAmberBg,
    onTertiaryContainer = DarkInstrumentPalette.txAmber,
    error = DarkInstrumentPalette.alertCrimson,
    onError = Color(0xFF2F080A),
    errorContainer = DarkInstrumentPalette.alertCrimsonBg,
    onErrorContainer = DarkInstrumentPalette.alertCrimson,
    background = DarkInstrumentPalette.background,
    onBackground = DarkInstrumentPalette.ink,
    surface = DarkInstrumentPalette.surface,
    onSurface = DarkInstrumentPalette.ink,
    surfaceVariant = DarkInstrumentPalette.surfaceSubtle,
    onSurfaceVariant = DarkInstrumentPalette.inkSecondary,
    outline = DarkInstrumentPalette.border,
    outlineVariant = DarkInstrumentPalette.borderStrong,
)

val InstrumentShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(18.dp),
)

@Composable
fun SerialScopeTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val isDark = resolveIsDarkTheme(themeMode)
    val palette = if (isDark) DarkInstrumentPalette else LightInstrumentPalette
    val colorScheme = if (isDark) FieldInstrumentDarkScheme else FieldInstrumentLightScheme

    val spaceGrotesk = FontFamily(
        Font(Res.font.space_grotesk, FontWeight.Normal),
        Font(Res.font.space_grotesk, FontWeight.Medium),
        Font(Res.font.space_grotesk, FontWeight.SemiBold),
        Font(Res.font.space_grotesk, FontWeight.Bold),
    )
    val inter = FontFamily(
        Font(Res.font.inter, FontWeight.Normal),
        Font(Res.font.inter, FontWeight.Medium),
        Font(Res.font.inter, FontWeight.SemiBold),
        Font(Res.font.inter, FontWeight.Bold),
    )
    val jetBrainsMono = FontFamily(
        Font(Res.font.jetbrains_mono, FontWeight.Normal),
        Font(Res.font.jetbrains_mono, FontWeight.Medium),
        Font(Res.font.jetbrains_mono, FontWeight.Bold),
    )
    val typography = remember(spaceGrotesk, inter, jetBrainsMono) {
        buildSerialScopeTypography(spaceGrotesk, inter, jetBrainsMono)
    }

    CompositionLocalProvider(
        LocalSpaceGroteskFontFamily provides spaceGrotesk,
        LocalInterFontFamily provides inter,
        LocalJetBrainsMonoFontFamily provides jetBrainsMono,
        LocalInstrumentColors provides palette,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = InstrumentShapes,
            content = content,
        )
    }
}
