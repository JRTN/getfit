package com.jrussell.fitness.core.designsystem.theme

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.jrussell.fitness.core.model.ThemeMode
import org.junit.Test

class ThemeModeResolutionTest {
    @Test
    fun system_followsSystem() {
        assertThat(ThemeMode.SYSTEM.resolveDark(systemInDarkTheme = true)).isTrue()
        assertThat(ThemeMode.SYSTEM.resolveDark(systemInDarkTheme = false)).isFalse()
    }

    @Test
    fun light_ignoresSystem() {
        assertThat(ThemeMode.LIGHT.resolveDark(systemInDarkTheme = true)).isFalse()
    }

    @Test
    fun dark_ignoresSystem() {
        assertThat(ThemeMode.DARK.resolveDark(systemInDarkTheme = false)).isTrue()
    }
}
