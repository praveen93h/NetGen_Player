package com.nextgen.player.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode {
    DARK, PURE_BLACK, MIDNIGHT_BLUE, LIGHT, SYSTEM
}

private val DarkColorScheme = darkColorScheme(
    primary = Orange500,
    onPrimary = Color.White,
    primaryContainer = Orange700,
    onPrimaryContainer = Color.White,
    secondary = Blue400,
    onSecondary = Color.White,
    secondaryContainer = Blue500,
    onSecondaryContainer = Color.White,
    tertiary = Orange400,
    background = Dark900,
    onBackground = TextPrimary,
    surface = Dark800,
    onSurface = TextPrimary,
    surfaceVariant = Dark600,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHighest = Dark500,
    surfaceContainerHigh = Dark600,
    surfaceContainer = Dark700,
    surfaceContainerLow = Dark800,
    surfaceContainerLowest = Dark850,
    outline = Dark400,
    outlineVariant = Dark300,
    error = ErrorRed,
    onError = Color.White
)

private val PureBlackColorScheme = darkColorScheme(
    primary = Orange500,
    onPrimary = Color.White,
    primaryContainer = Orange700,
    onPrimaryContainer = Color.White,
    secondary = Blue400,
    onSecondary = Color.White,
    secondaryContainer = Blue500,
    onSecondaryContainer = Color.White,
    tertiary = Orange400,
    background = PureBlack,
    onBackground = TextPrimary,
    surface = PureBlackSurface,
    onSurface = TextPrimary,
    surfaceVariant = PureBlackCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHighest = Color(0xFF1A1A1A),
    surfaceContainerHigh = Color(0xFF141414),
    surfaceContainer = Color(0xFF0A0A0A),
    surfaceContainerLow = PureBlackSurface,
    surfaceContainerLowest = PureBlack,
    outline = Color(0xFF222222),
    outlineVariant = Color(0xFF1A1A1A),
    error = ErrorRed,
    onError = Color.White
)

private val MidnightBlueColorScheme = darkColorScheme(
    primary = Orange500,
    onPrimary = Color.White,
    primaryContainer = Orange700,
    onPrimaryContainer = Color.White,
    secondary = Blue400,
    onSecondary = Color.White,
    secondaryContainer = Blue500,
    onSecondaryContainer = Color.White,
    tertiary = Orange400,
    background = MidnightBlueBg,
    onBackground = TextPrimary,
    surface = MidnightBlueSurface,
    onSurface = TextPrimary,
    surfaceVariant = MidnightBlueCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHighest = Color(0xFF1E2A42),
    surfaceContainerHigh = Color(0xFF182236),
    surfaceContainer = MidnightBlueCard,
    surfaceContainerLow = MidnightBlueSurface,
    surfaceContainerLowest = MidnightBlueBg,
    outline = MidnightBlueOutline,
    outlineVariant = Color(0xFF162033),
    error = ErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Orange500,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = Orange700,
    secondary = Blue500,
    onSecondary = Color.White,
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF49454F)
)

@Composable
fun NextGenPlayerTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = false,
    accentColor: Color? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        else -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            val baseScheme = when (themeMode) {
                ThemeMode.PURE_BLACK -> PureBlackColorScheme
                ThemeMode.MIDNIGHT_BLUE -> MidnightBlueColorScheme
                ThemeMode.LIGHT -> LightColorScheme
                ThemeMode.SYSTEM -> if (isDark) DarkColorScheme else LightColorScheme
                ThemeMode.DARK -> DarkColorScheme
            }
            if (accentColor != null) {
                baseScheme.copy(
                    primary = accentColor,
                    primaryContainer = accentColor.copy(alpha = 0.7f)
                )
            } else baseScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
