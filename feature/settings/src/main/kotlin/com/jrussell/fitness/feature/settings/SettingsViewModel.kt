package com.jrussell.fitness.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jrussell.fitness.core.domain.UserPreferencesRepository
import com.jrussell.fitness.core.model.ColorPalette
import com.jrussell.fitness.core.model.ThemeMode
import com.jrussell.fitness.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** State and actions for the Settings screen's Appearance section. */
@HiltViewModel
class SettingsViewModel @Inject constructor(private val repository: UserPreferencesRepository) : ViewModel() {
    /** The saved theme mode and palette; changes apply app-wide as soon as they are written. */
    val uiState: StateFlow<UserPreferences> = repository.userPreferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UserPreferences.DEFAULT,
    )

    fun onThemeModeSelected(themeMode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(themeMode)
        }
    }

    fun onPaletteSelected(palette: ColorPalette) {
        viewModelScope.launch {
            repository.setPalette(palette)
        }
    }

    private companion object {
        /** Keeps the upstream alive across a configuration change (e.g. rotation). */
        const val STOP_TIMEOUT_MILLIS: Long = 5_000L
    }
}
