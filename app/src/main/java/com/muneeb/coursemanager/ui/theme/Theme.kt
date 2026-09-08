package com.muneeb.coursemanager.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme

@Composable
fun NotesManagerTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = AppWhite,
            onPrimary = AppBlack,
            primaryContainer = DarkSurface,
            onPrimaryContainer = AppWhite,
            secondary = AppWhite,
            onSecondary = AppBlack,
            secondaryContainer = DarkSurface,
            onSecondaryContainer = AppWhite,
            tertiary = AppWhite,
            onTertiary = AppBlack,
            tertiaryContainer = DarkSurface,
            onTertiaryContainer = AppWhite,
            background = DarkBackground,
            surface = DarkSurface,
            onBackground = AppWhite,
            onSurface = AppWhite
        )
    } else {
        lightColorScheme(
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
            surface = AppWhite,
            onBackground = AppBlack,
            onSurface = AppBlack
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}