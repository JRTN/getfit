package com.jrussell.fitness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.jrussell.fitness.core.domain.UserPreferencesRepository
import com.jrussell.fitness.core.model.UserPreferences
import com.jrussell.fitness.feature.home.HomeKey
import com.jrussell.fitness.navigation.TopLevelBackStack
import com.jrussell.fitness.navigation.TopLevelDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Activity-scoped state. Holding the tab back stack here keeps it across configuration changes
 * such as rotation. After process death the app starts again on Home.
 */
@HiltViewModel
class MainViewModel @Inject constructor(repository: UserPreferencesRepository) : ViewModel() {
    /** Navigation state shared by the bottom bar, the top bar and `NavDisplay`. */
    val navigation: TopLevelBackStack<NavKey> =
        TopLevelBackStack(
            HomeKey,
            tabs = TopLevelDestination.entries.map { destination: TopLevelDestination ->
                destination.key
            },
        )

    /** The saved theme and palette; `null` until first read, so the UI never flashes the wrong theme. */
    val userPreferences: StateFlow<UserPreferences?> = repository.userPreferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )
}
