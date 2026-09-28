package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.prefs.AppTheme
import com.example.data.prefs.ThemePreferenceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(
    private val repository: ThemePreferenceRepository,
    initialDark: Boolean = false
) : ViewModel() {

    val isDark: StateFlow<Boolean> = repository.themeFlow
        .map { it == AppTheme.DARK }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = initialDark
        )

    fun toggle() {
        viewModelScope.launch {
            val nextTheme = if (isDark.value) AppTheme.LIGHT else AppTheme.DARK
            repository.setTheme(nextTheme)
        }
    }

    fun setDark(dark: Boolean) {
        viewModelScope.launch {
            repository.setTheme(if (dark) AppTheme.DARK else AppTheme.LIGHT)
        }
    }
}

class ThemeViewModelFactory(
    private val repository: ThemePreferenceRepository,
    private val initialDark: Boolean
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThemeViewModel::class.java)) {
            return ThemeViewModel(repository, initialDark) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
