package com.example.schedule.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = DeepGreen,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8F3DF),
    onPrimaryContainer = Color(0xFF062100),
    secondary = Color(0xFF54634D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E8CC),
    onSecondaryContainer = Color(0xFF121F0E),
    tertiary = Color(0xFF386667),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBCECED),
    onTertiaryContainer = Color(0xFF002021),
    background = AppBackground,
    onBackground = Color(0xFF191D17),
    surface = AppBackground,
    onSurface = Color(0xFF191D17),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9CD675),
    onPrimary = Color(0xFF113600),
    primaryContainer = Color(0xFF214F0B),
    onPrimaryContainer = Color(0xFFB7F38F),
    secondary = Color(0xFFBBCBB1),
    onSecondary = Color(0xFF263422),
    secondaryContainer = Color(0xFF3C4B37),
    onSecondaryContainer = Color(0xFFD7E8CC),
    tertiary = Color(0xFFA0D0D1),
    onTertiary = Color(0xFF003738),
    tertiaryContainer = Color(0xFF1E4E4F),
    onTertiaryContainer = Color(0xFFBCECED),
    background = Color(0xFF191D17),
    onBackground = Color(0xFFE1E4DB),
    surface = Color(0xFF191D17),
    onSurface = Color(0xFFE1E4DB),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun ScheduleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Android 12+ 动态取色
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        // 低版本回退自定义颜色
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
