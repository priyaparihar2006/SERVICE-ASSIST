package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.prefs.RainbowColor

// ============================================================================
// MATERIAL 3 COLOR SCHEMES BUILDER
// ============================================================================

fun buildServoraLightColorScheme(rainbow: RainbowColor = RainbowColor.GREEN): ColorScheme {
    val primaryColor = Color(rainbow.primaryLightHex)
    val containerColor = Color(rainbow.containerLightHex)
    val onContainerColor = Color(rainbow.onContainerLightHex)

    return lightColorScheme(
        primary = primaryColor,
        onPrimary = Color.White,
        primaryContainer = containerColor,
        onPrimaryContainer = onContainerColor,
        inversePrimary = Color(rainbow.primaryDarkHex),
        secondary = ServoraHoney,
        onSecondary = ServoraCharcoal,
        secondaryContainer = containerColor,
        onSecondaryContainer = onContainerColor,
        tertiary = ServoraBlue,
        onTertiary = Color.White,
        tertiaryContainer = ServoraBlueLight,
        onTertiaryContainer = Color(0xFF1E40AF),
        background = ServoraCanvas,
        onBackground = ServoraCharcoal,
        surface = ServoraSurface,
        onSurface = ServoraCharcoal,
        surfaceVariant = ServoraSurfaceSubtle,
        onSurfaceVariant = ServoraSubtext,
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF9FAFB),
        surfaceContainer = Color(0xFFF3F4F6),
        surfaceContainerHigh = Color(0xFFE5E7EB),
        surfaceContainerHighest = Color(0xFFD1D5DB),
        inverseSurface = ServoraCharcoal,
        inverseOnSurface = Color.White,
        outline = ServoraBorder,
        outlineVariant = Color(0xFFE2E8F0),
        error = Color(0xFFE53935),
        onError = Color.White,
        errorContainer = Color(0xFFFEE2E2),
        onErrorContainer = Color(0xFF991B1B),
        scrim = Color(0x99000000)
    )
}

fun buildServoraDarkColorScheme(rainbow: RainbowColor = RainbowColor.GREEN): ColorScheme {
    val primaryDark = Color(rainbow.primaryDarkHex)
    val containerDark = Color(rainbow.containerDarkHex)
    val onContainerDark = Color(rainbow.onContainerDarkHex)

    return darkColorScheme(
        primary = primaryDark,
        onPrimary = Color(0xFF0F1412),
        primaryContainer = containerDark,
        onPrimaryContainer = onContainerDark,
        inversePrimary = Color(rainbow.primaryLightHex),
        secondary = ServoraDarkWarning,
        onSecondary = Color(0xFF261900),
        secondaryContainer = ServoraDarkWarningContainer,
        onSecondaryContainer = ServoraDarkOnWarningContainer,
        tertiary = ServoraDarkInfo,
        onTertiary = Color(0xFF002244),
        tertiaryContainer = ServoraDarkInfoContainer,
        onTertiaryContainer = ServoraDarkOnInfoContainer,
        background = ServoraDarkBackground,
        onBackground = ServoraDarkTextPrimary,
        surface = ServoraDarkSurface,
        onSurface = ServoraDarkTextPrimary,
        surfaceVariant = ServoraDarkSurfaceVariant,
        onSurfaceVariant = ServoraDarkTextSecondary,
        surfaceContainerLowest = Color(0xFF0A0E0C),
        surfaceContainerLow = Color(0xFF131916),
        surfaceContainer = Color(0xFF171D1A),
        surfaceContainerHigh = Color(0xFF222B27),
        surfaceContainerHighest = Color(0xFF2B3732),
        inverseSurface = ServoraDarkTextPrimary,
        inverseOnSurface = ServoraDarkBackground,
        outline = ServoraDarkOutline,
        outlineVariant = ServoraDarkDivider,
        error = ServoraDarkDanger,
        onError = Color(0xFF3B0000),
        errorContainer = ServoraDarkDangerContainer,
        onErrorContainer = ServoraDarkOnDangerContainer,
        scrim = Color(0xCC000000)
    )
}

val ServoraLightColorScheme: ColorScheme = buildServoraLightColorScheme(RainbowColor.GREEN)
val ServoraDarkColorScheme: ColorScheme = buildServoraDarkColorScheme(RainbowColor.GREEN)

// ============================================================================
// EXTENDED SEMANTIC PALETTE (ServoraColors)
// ============================================================================

@Immutable
data class ServoraColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val danger: Color,
    val onDanger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textOnBrand: Color,
    val cardBackground: Color,
    val cardBackgroundSubtle: Color,
    val cardBorder: Color,
    val divider: Color,
    val brandGradientStart: Color,
    val brandGradientEnd: Color,
    val headerBackgroundStart: Color,
    val headerBackgroundEnd: Color,
    val chipBackground: Color,
    val chipText: Color,
    val ratingStar: Color,
    val badgeRed: Color,
    val navBarBackground: Color,
    val navBarSelected: Color,
    val navBarSelectedIndicator: Color,
    val navBarUnselected: Color,
    val chatBubbleMine: Color,
    val onChatBubbleMine: Color,
    val chatBubbleTheirs: Color,
    val onChatBubbleTheirs: Color,
    val chatBubbleTheirsBorder: Color = Color(0xFFE3E8E5),
    val chatBubbleTheirsMeta: Color = Color(0xFF6B7280),
    val chatWallpaper: Color = Color(0xFFEEF3EF),
    val chatSystemBubble: Color,
    val onChatSystemBubble: Color,
    val chatReadTick: Color,
    val scrimOnImage: Color,
    val shimmer: Color,
    val isDark: Boolean,
    val subtext: Color = textSecondary,
    val surfaceVariant: Color = cardBackgroundSubtle,
    val primary: Color = success,
    val onPrimary: Color = onSuccess,
    val primaryContainer: Color = successContainer,
    val onPrimaryContainer: Color = onSuccessContainer,
    val error: Color = danger,
    val onError: Color = onDanger,
    val errorContainer: Color = dangerContainer,
    val onErrorContainer: Color = onDangerContainer,
    val surface: Color = cardBackground
)

fun buildServoraColors(rainbow: RainbowColor = RainbowColor.GREEN, isDark: Boolean): ServoraColors {
    val primaryColor = if (isDark) Color(rainbow.primaryDarkHex) else Color(rainbow.primaryLightHex)
    val onPrimaryColor = if (isDark) Color(0xFF0F1412) else Color.White
    val containerColor = if (isDark) Color(rainbow.containerDarkHex) else Color(rainbow.containerLightHex)
    val onContainerColor = if (isDark) Color(rainbow.onContainerDarkHex) else Color(rainbow.onContainerLightHex)
    val gradStart = if (isDark) Color(rainbow.containerDarkHex) else Color(rainbow.primaryLightHex)
    val gradEnd = if (isDark) Color(rainbow.gradientDarkEndHex) else Color(rainbow.gradientLightEndHex)

    return if (!isDark) {
        ServoraColors(
            success = primaryColor,
            onSuccess = onPrimaryColor,
            successContainer = containerColor,
            onSuccessContainer = onContainerColor,
            warning = Color(0xFFF59E0B),
            onWarning = Color.White,
            warningContainer = Color(0xFFFEF3C7),
            onWarningContainer = Color(0xFFB45309),
            info = Color(0xFF2563EB),
            onInfo = Color.White,
            infoContainer = Color(0xFFEFF6FF),
            onInfoContainer = Color(0xFF1E40AF),
            danger = Color(0xFFE53935),
            onDanger = Color.White,
            dangerContainer = Color(0xFFFEE2E2),
            onDangerContainer = Color(0xFF991B1B),
            textPrimary = Color(0xFF111827),
            textSecondary = Color(0xFF6B7280),
            textMuted = Color(0xFF6B7280),
            textOnBrand = Color.White,
            cardBackground = Color(0xFFFFFFFF),
            cardBackgroundSubtle = Color(0xFFF9FAFB),
            cardBorder = Color(0xFFE5E7EB),
            divider = Color(0xFFE5E7EB),
            brandGradientStart = gradStart,
            brandGradientEnd = gradEnd,
            headerBackgroundStart = gradStart,
            headerBackgroundEnd = gradEnd,
            chipBackground = containerColor,
            chipText = onContainerColor,
            ratingStar = Color(0xFFF59E0B),
            badgeRed = Color(0xFFE53935),
            navBarBackground = Color(0xE6FFFFFF),
            navBarSelected = onContainerColor,
            navBarSelectedIndicator = containerColor,
            navBarUnselected = primaryColor.copy(alpha = 0.75f),
            chatBubbleMine = primaryColor,
            onChatBubbleMine = Color.White,
            chatBubbleTheirs = Color(0xFFFFFFFF),
            onChatBubbleTheirs = Color(0xFF111827),
            chatBubbleTheirsBorder = Color(0xFFE3E8E5),
            chatBubbleTheirsMeta = Color(0xFF6B7280),
            chatWallpaper = containerColor.copy(alpha = 0.25f),
            chatSystemBubble = Color(0xFFF3F4F6),
            onChatSystemBubble = Color(0xFF6B7280),
            chatReadTick = Color(0xFF2563EB),
            scrimOnImage = Color(0x99000000),
            shimmer = Color(0xFFE5E7EB),
            isDark = false
        )
    } else {
        ServoraColors(
            success = primaryColor,
            onSuccess = onPrimaryColor,
            successContainer = containerColor,
            onSuccessContainer = onContainerColor,
            warning = Color(0xFFFBBF24),
            onWarning = Color(0xFF261900),
            warningContainer = Color(0xFF3A2E0B),
            onWarningContainer = Color(0xFFFDE68A),
            info = Color(0xFF7AB8FF),
            onInfo = Color(0xFF002244),
            infoContainer = Color(0xFF0E2742),
            onInfoContainer = Color(0xFFBFDBFE),
            danger = Color(0xFFFF6B6B),
            onDanger = Color(0xFF3B0000),
            dangerContainer = Color(0xFF3B1414),
            onDangerContainer = Color(0xFFFECACA),
            textPrimary = Color(0xFFE8EEEA),
            textSecondary = Color(0xFFA9B7B0),
            textMuted = Color(0xFF8E9C95),
            textOnBrand = Color.White,
            cardBackground = Color(0xFF171D1A),
            cardBackgroundSubtle = Color(0xFF1F2723),
            cardBorder = Color(0xFF33403A),
            divider = Color(0xFF2A3430),
            brandGradientStart = gradStart,
            brandGradientEnd = gradEnd,
            headerBackgroundStart = gradStart,
            headerBackgroundEnd = gradEnd,
            chipBackground = containerColor,
            chipText = onContainerColor,
            ratingStar = Color(0xFFFBBF24),
            badgeRed = Color(0xFFE53935),
            navBarBackground = Color(0xE6171D1A),
            navBarSelected = primaryColor,
            navBarSelectedIndicator = containerColor,
            navBarUnselected = Color(0xFF8FA098),
            chatBubbleMine = gradEnd,
            onChatBubbleMine = Color(0xFFE8EEEA),
            chatBubbleTheirs = Color(0xFF26302B),
            onChatBubbleTheirs = Color(0xFFE8EEEA),
            chatBubbleTheirsBorder = Color(0xFF34403A),
            chatBubbleTheirsMeta = Color(0xFFA9B7B0),
            chatWallpaper = Color(0xFF0E1411),
            chatSystemBubble = Color(0xFF1F2723),
            onChatSystemBubble = Color(0xFFA9B7B0),
            chatReadTick = Color(0xFF7AB8FF),
            scrimOnImage = Color(0xCC000000),
            shimmer = Color(0xFF2A3430),
            isDark = true
        )
    }
}

val LightServoraColors = buildServoraColors(RainbowColor.GREEN, isDark = false)
val DarkServoraColors = buildServoraColors(RainbowColor.GREEN, isDark = true)

val LocalServoraColors = staticCompositionLocalOf { LightServoraColors }

object ServoraTheme {
    val colors: ServoraColors
        @Composable
        get() = LocalServoraColors.current
}

@Composable
fun SetDynamicStatusBar(
    isDarkIcons: Boolean = true
) {
    val isDarkTheme = ServoraTheme.colors.isDark
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = if (isDarkTheme) false else isDarkIcons
                insetsController.isAppearanceLightNavigationBars = !isDarkTheme
            }
        }
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    rainbowColor: RainbowColor = RainbowColor.GREEN,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) buildServoraDarkColorScheme(rainbowColor) else buildServoraLightColorScheme(rainbowColor)
    val customColors = buildServoraColors(rainbowColor, darkTheme)
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

    CompositionLocalProvider(LocalServoraColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalTextStyle provides MaterialTheme.typography.bodyMedium
                ) {
                    content()
                }
            }
        )
    }
}
