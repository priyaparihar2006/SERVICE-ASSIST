package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "servora_settings")

enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM
}

enum class RainbowColor(
    val id: String,
    val displayName: String,
    val primaryLightHex: Long,
    val primaryDarkHex: Long,
    val containerLightHex: Long,
    val containerDarkHex: Long,
    val onContainerLightHex: Long,
    val onContainerDarkHex: Long,
    val gradientLightEndHex: Long,
    val gradientDarkEndHex: Long
) {
    RED(
        id = "red",
        displayName = "Ruby Red",
        primaryLightHex = 0xFFDC2626,
        primaryDarkHex = 0xFFF87171,
        containerLightHex = 0xFFFEE2E2,
        containerDarkHex = 0xFF450A0A,
        onContainerLightHex = 0xFF991B1B,
        onContainerDarkHex = 0xFFFECACA,
        gradientLightEndHex = 0xFFB91C1C,
        gradientDarkEndHex = 0xFF991B1B
    ),
    ORANGE(
        id = "orange",
        displayName = "Sunset Orange",
        primaryLightHex = 0xFFEA580C,
        primaryDarkHex = 0xFFFB923C,
        containerLightHex = 0xFFFFEDD5,
        containerDarkHex = 0xFF431407,
        onContainerLightHex = 0xFF9A3412,
        onContainerDarkHex = 0xFFFED7AA,
        gradientLightEndHex = 0xFFC2410C,
        gradientDarkEndHex = 0xFF9A3412
    ),
    YELLOW(
        id = "yellow",
        displayName = "Amber Gold",
        primaryLightHex = 0xFFD97706,
        primaryDarkHex = 0xFFFBBF24,
        containerLightHex = 0xFFFEF3C7,
        containerDarkHex = 0xFF451A03,
        onContainerLightHex = 0xFF78350F,
        onContainerDarkHex = 0xFFFDE68A,
        gradientLightEndHex = 0xFFB45309,
        gradientDarkEndHex = 0xFF92400E
    ),
    GREEN(
        id = "green",
        displayName = "Emerald Green",
        primaryLightHex = 0xFF009051,
        primaryDarkHex = 0xFF34D399,
        containerLightHex = 0xFFE6F5EE,
        containerDarkHex = 0xFF064E3B,
        onContainerLightHex = 0xFF0B5433,
        onContainerDarkHex = 0xFFA7F3D0,
        gradientLightEndHex = 0xFF007542,
        gradientDarkEndHex = 0xFF064E3B
    ),
    BLUE(
        id = "blue",
        displayName = "Ocean Blue",
        primaryLightHex = 0xFF2563EB,
        primaryDarkHex = 0xFF60A5FA,
        containerLightHex = 0xFFEFF6FF,
        containerDarkHex = 0xFF172554,
        onContainerLightHex = 0xFF1E40AF,
        onContainerDarkHex = 0xFFBFDBFE,
        gradientLightEndHex = 0xFF1D4ED8,
        gradientDarkEndHex = 0xFF1E3A8A
    ),
    INDIGO(
        id = "indigo",
        displayName = "Royal Indigo",
        primaryLightHex = 0xFF4F46E5,
        primaryDarkHex = 0xFF818CF8,
        containerLightHex = 0xFFEEF2FF,
        containerDarkHex = 0xFF1E1B4B,
        onContainerLightHex = 0xFF3730A3,
        onContainerDarkHex = 0xFFC7D2FE,
        gradientLightEndHex = 0xFF4338CA,
        gradientDarkEndHex = 0xFF312E81
    ),
    VIOLET(
        id = "violet",
        displayName = "Amethyst Violet",
        primaryLightHex = 0xFF7C3AED,
        primaryDarkHex = 0xFFA78BFA,
        containerLightHex = 0xFFF5F3FF,
        containerDarkHex = 0xFF2E1065,
        onContainerLightHex = 0xFF5B21B6,
        onContainerDarkHex = 0xFFDDD6FE,
        gradientLightEndHex = 0xFF6D28D9,
        gradientDarkEndHex = 0xFF4C1D95
    ),
    ROSE(
        id = "rose",
        displayName = "Rose Pink",
        primaryLightHex = 0xFFE11D48,
        primaryDarkHex = 0xFFF472B6,
        containerLightHex = 0xFFFFE4E6,
        containerDarkHex = 0xFF4C0519,
        onContainerLightHex = 0xFF9F1239,
        onContainerDarkHex = 0xFFFECDD3,
        gradientLightEndHex = 0xFFBE123C,
        gradientDarkEndHex = 0xFF881337
    )
}

class ThemePreferenceRepository(private val context: Context) {

    private val themeKey = stringPreferencesKey("app_theme")
    private val colorKey = stringPreferencesKey("app_color_scheme")

    val themeFlow: Flow<AppTheme> = context.themeDataStore.data.map { preferences ->
        val themeName = preferences[themeKey] ?: AppTheme.LIGHT.name
        try {
            AppTheme.valueOf(themeName)
        } catch (_: Exception) {
            AppTheme.LIGHT
        }
    }

    val colorFlow: Flow<RainbowColor> = context.themeDataStore.data.map { preferences ->
        val colorName = preferences[colorKey] ?: RainbowColor.GREEN.name
        try {
            RainbowColor.valueOf(colorName)
        } catch (_: Exception) {
            RainbowColor.GREEN
        }
    }

    suspend fun setTheme(theme: AppTheme) {
        context.themeDataStore.edit { preferences ->
            preferences[themeKey] = theme.name
        }
    }

    suspend fun setColor(color: RainbowColor) {
        context.themeDataStore.edit { preferences ->
            preferences[colorKey] = color.name
        }
    }
}
