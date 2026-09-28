package com.example.ui.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/** Status-bar height that does NOT change when the bar is hidden/shown. */
@OptIn(ExperimentalLayoutApi::class)
val WindowInsets.Companion.stableStatusBars: WindowInsets
    @Composable get() = WindowInsets.statusBarsIgnoringVisibility

/** Drop-in replacement for statusBarsPadding() that never jumps. */
@OptIn(ExperimentalLayoutApi::class)
fun Modifier.stableStatusBarsPadding(): Modifier = composed {
    windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
}
