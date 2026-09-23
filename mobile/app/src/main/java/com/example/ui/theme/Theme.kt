package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val ServoraLightColorScheme = lightColorScheme(
    primary = ServoraCoral,
    onPrimary = Color.White,
    primaryContainer = ServoraPeach,
    onPrimaryContainer = ServoraCoralDark,
    secondary = ServoraHoney,
    onSecondary = ServoraCharcoal,
    secondaryContainer = ServoraPeachLight,
    onSecondaryContainer = ServoraCharcoal,
    tertiary = ServoraCharcoal,
    onTertiary = Color.White,
    background = ServoraCanvas,
    onBackground = ServoraCharcoal,
    surface = ServoraSurface,
    onSurface = ServoraCharcoal,
    surfaceVariant = ServoraSurfaceSubtle,
    onSurfaceVariant = ServoraSubtext,
    outline = ServoraBorder,
    outlineVariant = Color(0xFFDED8CE)
)

private val ServoraDarkColorScheme = darkColorScheme(
    primary = ServoraCoral,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF381B12),
    onPrimaryContainer = ServoraPeach,
    secondary = ServoraHoney,
    onSecondary = Color.Black,
    background = Color(0xFF141312),
    onBackground = Color(0xFFEDE8E1),
    surface = Color(0xFF1E1C1A),
    onSurface = Color(0xFFEDE8E1),
    surfaceVariant = Color(0xFF282522),
    onSurfaceVariant = Color(0xFFB0A99F),
    outline = Color(0xFF3D3833)
)

@Composable
fun SetDynamicStatusBar(
    isDarkIcons: Boolean = true
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = isDarkIcons
                insetsController.isAppearanceLightNavigationBars = true
            }
        }
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ServoraDarkColorScheme else ServoraLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
