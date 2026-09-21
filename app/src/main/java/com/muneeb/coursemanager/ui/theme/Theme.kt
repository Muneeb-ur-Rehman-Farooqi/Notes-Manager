package com.muneeb.coursemanager.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppStyle(
    val fabContainerColor: Color,
    val fabContentColor: Color,
    val primaryButtonContainerColor: Color,
    val primaryButtonContentColor: Color,
    val bannerContainerColor: Color,
    val bannerContentColor: Color,
    val cardContainerColor: Color,
    val cardContentColor: Color,
    val cardTitleColor: Color,
    val cardSubtitleColor: Color,
    val countdownColor: Color,
    val errorTextColor: Color
)

val LocalAppStyle = staticCompositionLocalOf {
    AppStyle(
        fabContainerColor = Color.Black,
        fabContentColor = Color.White,
        primaryButtonContainerColor = Color.Black,
        primaryButtonContentColor = Color.White,
        bannerContainerColor = Color.Black,
        bannerContentColor = Color.White,
        cardContainerColor = Color(0xFFE0E0E0),
        cardContentColor = Color.Black,
        cardTitleColor = Color.Black,
        cardSubtitleColor = Color(0xFF4A4A4A),
        countdownColor = Color(0xFFE53935),
        errorTextColor = Color(0xFFB3261E)
    )
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
    surfaceVariant = Color(0xFF2A2A2A),
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

private fun monochromeStyleLight(): AppStyle = AppStyle(
    fabContainerColor = AppBlack,
    fabContentColor = AppWhite,
    primaryButtonContainerColor = AppBlack,
    primaryButtonContentColor = AppWhite,
    bannerContainerColor = AppBlack,
    bannerContentColor = AppWhite,
    cardContainerColor = Color(0xFFE0E0E0),
    cardContentColor = AppBlack,
    cardTitleColor = AppBlack,
    cardSubtitleColor = Color(0xFF4A4A4A),
    countdownColor = MonochromeCountdownRed,
    errorTextColor = ErrorLight
)

private fun monochromeStyleDark(): AppStyle = AppStyle(
    fabContainerColor = AppWhite,
    fabContentColor = AppBlack,
    primaryButtonContainerColor = AppWhite,
    primaryButtonContentColor = AppBlack,
    bannerContainerColor = AppWhite,
    bannerContentColor = AppBlack,
    cardContainerColor = Color(0xFF2A2A2A),
    cardContentColor = AppWhite,
    cardTitleColor = AppWhite,
    cardSubtitleColor = Color(0xFFBDBDBD),
    countdownColor = MonochromeCountdownRed,
    errorTextColor = ErrorDark
)

private fun pinkStyleLight(): AppStyle = AppStyle(
    fabContainerColor = PinkLightPrimary,
    fabContentColor = PinkLightOnPrimary,
    primaryButtonContainerColor = PinkLightPrimary,
    primaryButtonContentColor = PinkLightOnPrimary,
    bannerContainerColor = PinkLightAccentCard,
    bannerContentColor = AppWhite,
    cardContainerColor = PinkLightSurfaceVariant,
    cardContentColor = PinkLightOnSurface,
    cardTitleColor = PinkLightOnSurface,
    cardSubtitleColor = PinkLightOnSurfaceVariant,
    countdownColor = PinkLightCountdown,
    errorTextColor = ErrorLight
)

private fun pinkStyleDark(): AppStyle = AppStyle(
    fabContainerColor = PinkDarkPrimary,
    fabContentColor = PinkDarkOnPrimary,
    primaryButtonContainerColor = PinkDarkPrimary,
    primaryButtonContentColor = PinkDarkOnPrimary,
    bannerContainerColor = PinkDarkAccentCard,
    bannerContentColor = AppWhite,
    cardContainerColor = PinkDarkSurfaceVariant,
    cardContentColor = PinkDarkOnSurface,
    cardTitleColor = PinkDarkOnSurface,
    cardSubtitleColor = PinkDarkOnSurfaceVariant,
    countdownColor = PinkDarkCountdown,
    errorTextColor = ErrorDark
)

private fun slateStyleLight(): AppStyle = AppStyle(
    fabContainerColor = SlateLightPrimary,
    fabContentColor = SlateLightOnPrimary,
    primaryButtonContainerColor = SlateLightPrimary,
    primaryButtonContentColor = SlateLightOnPrimary,
    bannerContainerColor = SlateLightAccentCard,
    bannerContentColor = AppWhite,
    cardContainerColor = SlateLightSurfaceVariant,
    cardContentColor = SlateLightOnSurface,
    cardTitleColor = SlateLightOnSurface,
    cardSubtitleColor = SlateLightOnSurfaceVariant,
    countdownColor = SlateCountdownRed,
    errorTextColor = ErrorLight
)

private fun slateStyleDark(): AppStyle = AppStyle(
    fabContainerColor = SlateDarkPrimary,
    fabContentColor = SlateDarkOnPrimary,
    primaryButtonContainerColor = SlateDarkPrimary,
    primaryButtonContentColor = SlateDarkOnPrimary,
    bannerContainerColor = SlateDarkAccentCard,
    bannerContentColor = AppWhite,
    cardContainerColor = SlateDarkSurfaceVariant,
    cardContentColor = SlateDarkOnSurface,
    cardTitleColor = SlateDarkOnSurface,
    cardSubtitleColor = SlateDarkOnSurfaceVariant,
    countdownColor = SlateCountdownRed,
    errorTextColor = ErrorDark
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
    val appStyle: AppStyle = when (palette) {
        ThemePalette.MONOCHROME -> if (darkTheme) monochromeStyleDark() else monochromeStyleLight()
        ThemePalette.PINK -> if (darkTheme) pinkStyleDark() else pinkStyleLight()
        ThemePalette.SLATE -> if (darkTheme) slateStyleDark() else slateStyleLight()
    }
    CompositionLocalProvider(LocalAppStyle provides appStyle) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}