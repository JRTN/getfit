package com.jrussell.fitness.feature.settings

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jrussell.fitness.core.model.ColorPalette
import com.jrussell.fitness.core.model.ThemeMode
import com.jrussell.fitness.core.model.UserPreferences
import com.jrussell.fitness.core.testing.FakeUserPreferencesRepository
import com.jrussell.fitness.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule: MainDispatcherRule = MainDispatcherRule()

    private val repository: FakeUserPreferencesRepository =
        FakeUserPreferencesRepository(UserPreferences(ThemeMode.DARK, ColorPalette.COLOR_VISION_SAFE))

    @Test
    fun uiState_reflectsRepository() = runTest {
        val viewModel: SettingsViewModel = SettingsViewModel(repository)

        viewModel.uiState.test {
            assertThat(expectMostRecentItem())
                .isEqualTo(UserPreferences(ThemeMode.DARK, ColorPalette.COLOR_VISION_SAFE))
        }
    }

    @Test
    fun onPaletteSelected_writesRepository() = runTest {
        val viewModel: SettingsViewModel = SettingsViewModel(repository)

        viewModel.onPaletteSelected(ColorPalette.HIGH_CONTRAST)

        assertThat(repository.current.palette).isEqualTo(ColorPalette.HIGH_CONTRAST)
    }

    @Test
    fun onThemeModeSelected_writesRepository() = runTest {
        val viewModel: SettingsViewModel = SettingsViewModel(repository)

        viewModel.onThemeModeSelected(ThemeMode.LIGHT)

        assertThat(repository.current.themeMode).isEqualTo(ThemeMode.LIGHT)
    }
}
