package com.muneeb.coursemanager.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ExtraColors(
    val cardAccentContainer: Color,
    val cardAccentContent: Color
)

val LocalExtraColors = staticCompositionLocalOf {
    ExtraColors(cardAccentContainer = Color.Black, cardAccentContent = Color.White)
}

private fun monochromeLightScheme(): ColorScheme = lightColorScheme(
    primary = AppBlack,
    onPrimary = AppWhite,
    primaryContainer = AppWhite,
    onPrimaryContainer = AppBlack,
    secondary = AppBlack,
    onSecondary = AppWhite,
    secondaryContainer = AppWhite,
    onSecondaryContainer = AppBlack,
    tertiary = AppBlack,
    onTertiary = AppWhite,
    tertiaryContainer = AppWhite,
    onTertiaryContainer = AppBlack,
    background = AppWhite,
    onBackground = AppBlack,
    surface = AppWhite,
    onSurface = AppBlack,
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF4A4A4A),
    inverseSurface = AppBlack,
    inverseOnSurface = AppWhite,
    inversePrimary = AppWhite,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    outline = Color(0xFF7A7A7A),
    outlineVariant = Color(0xFFBDBDBD),
    scrim = Color(0xFF000000),
    surfaceTint = AppBlack
)

private fun monochromeDarkScheme(): ColorScheme = darkColorScheme(
    primary = AppWhite,
    onPrimary = AppBlack,
    primaryContainer = AppWhite,
    onPrimaryContainer = AppBlack,
    secondary = AppWhite,
    onSecondary = AppBlack,
    secondaryContainer = AppWhite,
    onSecondaryContainer = AppBlack,
    tertiary = AppWhite,
    onTertiary = AppBlack,
    tertiaryContainer = AppWhite,
    onTertiaryContainer = AppBlack,
    background = DarkBackground,
    onBackground = AppWhite,
    surface = DarkSurface,
    onSurface = AppWhite,
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFBDBDBD),
    inverseSurface = AppWhite,
    inverseOnSurface = AppBlack,
    inversePrimary = AppBlack,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    outline = Color(0xFF8A8A8A),
    outlineVariant = Color(0xFF3A3A3A),
    scrim = Color(0xFF000000),
    surfaceTint = AppWhite
)

private fun pinkLightScheme(): ColorScheme = lightColorScheme(
    primary = PinkLightPrimary,
    onPrimary = PinkLightOnPrimary,
    primaryContainer = PinkLightPrimaryContainer,
    onPrimaryContainer = PinkLightOnPrimaryContainer,
    secondary = PinkLightSecondary,
    onSecondary = PinkLightOnSecondary,
    secondaryContainer = PinkLightSecondaryContainer,
    onSecondaryContainer = PinkLightOnSecondaryContainer,
    tertiary = PinkLightTertiary,
    onTertiary = PinkLightOnTertiary,
    tertiaryContainer = PinkLightTertiaryContainer,
    onTertiaryContainer = PinkLightOnTertiaryContainer,
    background = PinkLightBackground,
    onBackground = PinkLightOnBackground,
    surface = PinkLightBackground,
    onSurface = PinkLightOnSurface,
    surfaceVariant = PinkLightSurfaceVariant,
    onSurfaceVariant = PinkLightOnSurfaceVariant,
    inverseSurface = PinkLightInverseSurface,
    inverseOnSurface = PinkLightInverseOnSurface,
    inversePrimary = PinkLightInversePrimary,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    outline = PinkLightOutline,
    outlineVariant = PinkLightOutlineVariant,
    scrim = Color(0xFF000000),
    surfaceTint = PinkLightPrimary
)

private fun pinkDarkScheme(): ColorScheme = darkColorScheme(
    primary = PinkDarkPrimary,
    onPrimary = PinkDarkOnPrimary,
    primaryContainer = PinkDarkPrimaryContainer,
    onPrimaryContainer = PinkDarkOnPrimaryContainer,
    secondary = PinkDarkSecondary,
    onSecondary = PinkDarkOnSecondary,
    secondaryContainer = PinkDarkSecondaryContainer,
    onSecondaryContainer = PinkDarkOnSecondaryContainer,
    tertiary = PinkDarkTertiary,
    onTertiary = PinkDarkOnTertiary,
    tertiaryContainer = PinkDarkTertiaryContainer,
    onTertiaryContainer = PinkDarkOnTertiaryContainer,
    background = PinkDarkBackground,
    onBackground = PinkDarkOnBackground,
    surface = PinkDarkSurface,
    onSurface = PinkDarkOnSurface,
    surfaceVariant = PinkDarkSurfaceVariant,
    onSurfaceVariant = PinkDarkOnSurfaceVariant,
    inverseSurface = PinkDarkInverseSurface,
    inverseOnSurface = PinkDarkInverseOnSurface,
    inversePrimary = PinkDarkInversePrimary,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    outline = PinkDarkOutline,
    outlineVariant = PinkDarkOutlineVariant,
    scrim = Color(0xFF000000),
    surfaceTint = PinkDarkPrimary
)

private fun slateLightScheme(): ColorScheme = lightColorScheme(
    primary = SlateLightPrimary,
    onPrimary = SlateLightOnPrimary,
    primaryContainer = SlateLightPrimaryContainer,
    onPrimaryContainer = SlateLightOnPrimaryContainer,
    secondary = SlateLightSecondary,
    onSecondary = SlateLightOnSecondary,
    secondaryContainer = SlateLightSecondaryContainer,
    onSecondaryContainer = SlateLightOnSecondaryContainer,
    tertiary = SlateLightTertiary,
    onTertiary = SlateLightOnTertiary,
    tertiaryContainer = SlateLightTertiaryContainer,
    onTertiaryContainer = SlateLightOnTertiaryContainer,
    background = SlateLightBackground,
    onBackground = SlateLightOnBackground,
    surface = SlateLightBackground,
    onSurface = SlateLightOnSurface,
    surfaceVariant = SlateLightSurfaceVariant,
    onSurfaceVariant = SlateLightOnSurfaceVariant,
    inverseSurface = SlateLightInverseSurface,
    inverseOnSurface = SlateLightInverseOnSurface,
    inversePrimary = SlateLightInversePrimary,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    outline = SlateLightOutline,
    outlineVariant = SlateLightOutlineVariant,
    scrim = Color(0xFF000000),
    surfaceTint = SlateLightPrimary
)

private fun slateDarkScheme(): ColorScheme = darkColorScheme(
    primary = SlateDarkPrimary,
    onPrimary = SlateDarkOnPrimary,
    primaryContainer = SlateDarkPrimaryContainer,
    onPrimaryContainer = SlateDarkOnPrimaryContainer,
    secondary = SlateDarkSecondary,
    onSecondary = SlateDarkOnSecondary,
    secondaryContainer = SlateDarkSecondaryContainer,
    onSecondaryContainer = SlateDarkOnSecondaryContainer,
    tertiary = SlateDarkTertiary,
    onTertiary = SlateDarkOnTertiary,
    tertiaryContainer = SlateDarkTertiaryContainer,
    onTertiaryContainer = SlateDarkOnTertiaryContainer,
    background = SlateDarkBackground,
    onBackground = SlateDarkOnBackground,
    surface = SlateDarkSurface,
    onSurface = SlateDarkOnSurface,
    surfaceVariant = SlateDarkSurfaceVariant,
    onSurfaceVariant = SlateDarkOnSurfaceVariant,
    inverseSurface = SlateDarkInverseSurface,
    inverseOnSurface = SlateDarkInverseOnSurface,
    inversePrimary = SlateDarkInversePrimary,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    outline = SlateDarkOutline,
    outlineVariant = SlateDarkOutlineVariant,
    scrim = Color(0xFF000000),
    surfaceTint = SlateDarkPrimary
)

@Composable
fun NotesManagerTheme(
    palette: ThemePalette,
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme = when (palette) {
        ThemePalette.MONOCHROME -> if (darkTheme) monochromeDarkScheme() else monochromeLightScheme()
        ThemePalette.PINK -> if (darkTheme) pinkDarkScheme() else pinkLightScheme()
        ThemePalette.SLATE -> if (darkTheme) slateDarkScheme() else slateLightScheme()
    }
    val extraColors: ExtraColors = when (palette) {
        ThemePalette.MONOCHROME -> ExtraColors(
            cardAccentContainer = if (darkTheme) MonochromeDarkAccentCard else AppBlack,
            cardAccentContent = AppWhite
        )
        ThemePalette.PINK -> ExtraColors(
            cardAccentContainer = if (darkTheme) PinkDarkAccentCard else PinkLightAccentCard,
            cardAccentContent = AppWhite
        )
        ThemePalette.SLATE -> ExtraColors(
            cardAccentContainer = if (darkTheme) SlateDarkAccentCard else SlateLightAccentCard,
            cardAccentContent = AppWhite
        )
    }
    CompositionLocalProvider(LocalExtraColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}