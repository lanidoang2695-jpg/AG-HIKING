package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        primary = PineGreenPrimaryDark,
        onPrimary = PineGreenOnPrimaryDark,
        primaryContainer = PineGreenContainerDark,
        onPrimaryContainer = PineGreenOnContainerDark,
        secondary = TrailAmberSecondaryDark,
        onSecondary = TrailAmberOnSecondaryDark,
        secondaryContainer = TrailAmberContainerDark,
        onSecondaryContainer = TrailAmberOnContainerDark,
        tertiary = EarthSlateTertiaryDark,
        onTertiary = EarthSlateOnTertiaryDark,
        tertiaryContainer = EarthSlateContainerDark,
        onTertiaryContainer = EarthSlateOnContainerDark,
        background = OutdoorDarkBg,
        onBackground = OutdoorDarkOnBg,
        surface = OutdoorDarkSurface,
        onSurface = OutdoorDarkOnSurface,
        surfaceVariant = OutdoorDarkSurfaceVariant,
        onSurfaceVariant = OutdoorDarkOnSurfaceVariant,
        outline = OutdoorDarkOutline
    )

private val LightColorScheme =
    lightColorScheme(
        primary = PineGreenPrimary,
        onPrimary = PineGreenOnPrimary,
        primaryContainer = PineGreenContainer,
        onPrimaryContainer = PineGreenOnContainer,
        secondary = TrailAmberSecondary,
        onSecondary = TrailAmberOnSecondary,
        secondaryContainer = TrailAmberContainer,
        onSecondaryContainer = TrailAmberOnContainer,
        tertiary = EarthSlateTertiary,
        onTertiary = EarthSlateOnTertiary,
        tertiaryContainer = EarthSlateContainer,
        onTertiaryContainer = EarthSlateOnContainer,
        background = OutdoorLightBg,
        onBackground = OutdoorLightOnBg,
        surface = OutdoorLightSurface,
        onSurface = OutdoorLightOnSurface,
        surfaceVariant = OutdoorLightSurfaceVariant,
        onSurfaceVariant = OutdoorLightOnSurfaceVariant,
        outline = OutdoorLightOutline
    )

@Composable
fun AgHikingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent outdoor branding by default
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
