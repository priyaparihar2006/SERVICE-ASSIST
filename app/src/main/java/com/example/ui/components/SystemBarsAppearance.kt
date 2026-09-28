package com.example.ui.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.ui.theme.ServoraTheme

@Composable
fun StatusBarIcons(darkIcons: Boolean) {
    val isDarkTheme = ServoraTheme.colors.isDark
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            SideEffect {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = if (isDarkTheme) false else darkIcons
                insetsController.isAppearanceLightNavigationBars = !isDarkTheme
            }
        }
    }
}

/**
 * Status bar is intentionally always visible (attached to the header).
 * Kept as a no-op so existing call sites compile; safe to remove later.
 */
@Suppress("UnusedReceiverParameter")
@Composable
fun Modifier.hideStatusBarOnScroll(): Modifier = this
