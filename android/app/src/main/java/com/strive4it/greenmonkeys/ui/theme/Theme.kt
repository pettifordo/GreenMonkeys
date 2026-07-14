package com.strive4it.greenmonkeys.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand seeds from the iOS app: monkey green on a purple gradient.
private val MonkeyGreen = Color(0xFF4CAF50)
private val ShamePurple = Color(0xFF5E35B1)

private val DarkColors = darkColorScheme(
    primary = MonkeyGreen,
    secondary = ShamePurple,
)

private val LightColors = lightColorScheme(
    primary = MonkeyGreen,
    secondary = ShamePurple,
)

@Composable
fun GreenMonkeysTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
