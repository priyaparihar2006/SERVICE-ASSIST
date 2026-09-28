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
    DARK
}

class ThemePreferenceRepository(private val context: Context) {

    private val themeKey = stringPreferencesKey("app_theme")

    val themeFlow: Flow<AppTheme> = context.themeDataStore.data.map { preferences ->
        val themeName = preferences[themeKey] ?: AppTheme.LIGHT.name
        try {
            AppTheme.valueOf(themeName)
        } catch (_: Exception) {
            AppTheme.LIGHT
        }
    }

    suspend fun setTheme(theme: AppTheme) {
        context.themeDataStore.edit { preferences ->
            preferences[themeKey] = theme.name
        }
    }
}
