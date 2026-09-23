package com.example.ui.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun StatusBarIcons(darkIcons: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            SideEffect {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = darkIcons
            }
        }
    }
}

private const val SCROLL_THRESHOLD = 8f

fun statusBarScrollConnection(
    onHide: () -> Unit,
    onShow: () -> Unit,
): NestedScrollConnection = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val dy = available.y
        when {
            dy < -SCROLL_THRESHOLD -> onHide() // Finger moving up -> content moving down -> hide
            dy > SCROLL_THRESHOLD -> onShow()  // Finger moving down -> content moving up -> show
        }
        return Offset.Zero
    }
}

@Composable
fun Modifier.hideStatusBarOnScroll(): Modifier {
    val view = LocalView.current
    if (view.isInEditMode) return this
    val activity = LocalContext.current as? Activity ?: return this
    val controller = remember(activity, view) {
        WindowCompat.getInsetsController(activity.window, view).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    return this.nestedScroll(
        remember(controller) {
            statusBarScrollConnection(
                onHide = { controller.hide(WindowInsetsCompat.Type.statusBars()) },
                onShow = { controller.show(WindowInsetsCompat.Type.statusBars()) }
            )
        }
    )
}
