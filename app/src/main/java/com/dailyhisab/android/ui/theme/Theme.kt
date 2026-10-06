package com.dailyhisab.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ColorWhite = androidx.compose.ui.graphics.Color.White
private val ColorBlueLight = androidx.compose.ui.graphics.Color(0xFF9DB7FF)

private val LightColors = lightColorScheme(
    primary = DailyBlue,
    secondary = DailyOrange,
    background = AppBackground,
    surface = ColorWhite,
)

private val DarkColors = darkColorScheme(
    primary = ColorBlueLight,
    secondary = DailyOrange,
    background = DarkBackground,
)

@Composable
fun DailyHisabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = DailyHisabTypography,
        content = content,
    )
}
