package com.jrussell.fitness

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Process-wide entry point; `@HiltAndroidApp` creates the application-level Hilt component. */
@HiltAndroidApp
class GetFitApplication : Application()
