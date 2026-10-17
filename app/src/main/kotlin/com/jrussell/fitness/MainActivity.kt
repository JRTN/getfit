package com.jrussell.fitness

import android.app.UiModeManager
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jrussell.fitness.core.designsystem.theme.GetFitTheme
import com.jrussell.fitness.core.designsystem.theme.resolveDark
import com.jrussell.fitness.core.model.ThemeMode
import com.jrussell.fitness.core.model.UserPreferences
import com.jrussell.fitness.ui.GetFitApp
import dagger.hilt.android.AndroidEntryPoint

/** The app's single activity; all screens are composables hosted here. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferences: UserPreferences? by viewModel.userPreferences.collectAsStateWithLifecycle()
            // Until the saved preferences load (a few ms), the window background shows.
            preferences?.let { loaded: UserPreferences ->
                val dark: Boolean = loaded.themeMode.resolveDark(isSystemInDarkTheme())
                LaunchedEffect(loaded.themeMode) {
                    applyApplicationNightMode(loaded.themeMode)
                }
                DisposableEffect(dark) {
                    // Status- and navigation-bar icons follow the app's theme, not the system's.
                    val style: SystemBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                    onDispose { }
                }
                GetFitTheme(themeMode = loaded.themeMode, palette = loaded.palette) {
                    GetFitApp(
                        navigation = viewModel.navigation,
                        onExit = { moveTaskToBack(true) },
                    )
                }
            }
        }
    }

    /**
     * Tells Android the app's own light/dark choice. The system remembers it, so the launch
     * window and system UI match the app on the next cold start too, with no flash.
     */
    private fun applyApplicationNightMode(themeMode: ThemeMode) {
        val mode: Int = when (themeMode) {
            ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
            ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
            ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
        }
        getSystemService(UiModeManager::class.java).setApplicationNightMode(mode)
    }
}
