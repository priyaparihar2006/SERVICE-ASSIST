package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.prefs.AppTheme
import com.example.data.prefs.RainbowColor
import com.example.data.prefs.ThemePreferenceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(
    private val repository: ThemePreferenceRepository,
    initialTheme: AppTheme = AppTheme.LIGHT,
    initialColor: RainbowColor = RainbowColor.GREEN
) : ViewModel() {

    val appTheme: StateFlow<AppTheme> = repository.themeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = initialTheme
        )

    val isDark: StateFlow<Boolean> = repository.themeFlow
        .map { it == AppTheme.DARK }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = initialTheme == AppTheme.DARK
        )

    val selectedColor: StateFlow<RainbowColor> = repository.colorFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = initialColor
        )

    fun toggle() {
        viewModelScope.launch {
            val nextTheme = if (appTheme.value == AppTheme.DARK) AppTheme.LIGHT else AppTheme.DARK
            repository.setTheme(nextTheme)
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            repository.setTheme(theme)
        }
    }

    fun setDark(dark: Boolean) {
        viewModelScope.launch {
            repository.setTheme(if (dark) AppTheme.DARK else AppTheme.LIGHT)
        }
    }

    fun setColor(color: RainbowColor) {
        viewModelScope.launch {
            repository.setColor(color)
        }
    }
}

class ThemeViewModelFactory(
    private val repository: ThemePreferenceRepository,
    private val initialTheme: AppTheme = AppTheme.LIGHT,
    private val initialColor: RainbowColor = RainbowColor.GREEN
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThemeViewModel::class.java)) {
            return ThemeViewModel(repository, initialTheme, initialColor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
