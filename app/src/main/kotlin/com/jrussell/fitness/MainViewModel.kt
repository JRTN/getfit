package com.jrussell.fitness

import androidx.lifecycle.ViewModel
import androidx.navigation3.runtime.NavKey
import com.jrussell.fitness.feature.today.TodayKey
import com.jrussell.fitness.navigation.TopLevelBackStack
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Activity-scoped state. Holding the tab back stack here keeps it across configuration changes
 * such as rotation. After process death the app starts again on Today (acceptable for M0).
 */
@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {
    /** Navigation state shared by the bottom bar, the top bar and `NavDisplay`. */
    val navigation: TopLevelBackStack<NavKey> = TopLevelBackStack(TodayKey)
}
