package com.dailyhisab.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

enum class AppThemeStyle { Default, Aurora }

private val LightColors = lightColorScheme(
    primary = DailyBlue,
    onPrimary = Color.White,
    secondary = DailyOrange,
    tertiary = DailyGreen,
    error = DailyRed,
    background = AppBackground,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = SurfaceSoft,
    onSurfaceVariant = InkMuted,
    outlineVariant = DividerLight,
)

private val AuroraColors = LightColors.copy(
    background = AuroraBackground,
    surface = AuroraSurface,
    surfaceVariant = Color(0xFFEFF8FF),
    tertiary = AuroraLavender,
)

private val DarkColors = darkColorScheme(
    primary = DailyBlueLight,
    onPrimary = DailyBlueDark,
    secondary = DailyOrange,
    tertiary = AuroraCyan,
    error = Color(0xFFFFB4AB),
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceSoft,
)

@Composable
fun DailyHisabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    style: AppThemeStyle = AppThemeStyle.Aurora,
    content: @Composable () -> Unit,
) {
    val colors = when {
        darkTheme -> DarkColors
        style == AppThemeStyle.Aurora -> AuroraColors
        else -> LightColors
    }

    CompositionLocalProvider(LocalDailyHisabSpacing provides DailyHisabSpacing()) {
        MaterialTheme(
            colorScheme = colors,
            typography = DailyHisabTypography,
            shapes = DailyHisabShapes,
            content = content,
        )
    }
}

