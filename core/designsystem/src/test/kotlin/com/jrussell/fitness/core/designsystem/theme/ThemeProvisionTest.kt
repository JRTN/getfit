package com.jrussell.fitness.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.jrussell.fitness.core.model.ColorPalette
import com.jrussell.fitness.core.model.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ThemeProvisionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun highContrastDark_providesMatchingSchemeAndDataColors() {
        var scheme: ColorScheme? = null
        var data: DataColors? = null
        rule.setContent {
            GetFitTheme(themeMode = ThemeMode.DARK, palette = ColorPalette.HIGH_CONTRAST) {
                scheme = MaterialTheme.colorScheme
                data = GetFitTheme.dataColors
            }
        }
        rule.waitForIdle()

        val expected: ColorScheme = colorSchemeFor(ColorPalette.HIGH_CONTRAST, dark = true)
        assertThat(scheme).isNotNull()
        assertThat(scheme?.background).isEqualTo(expected.background)
        assertThat(scheme?.primary).isEqualTo(expected.primary)
        assertThat(data).isEqualTo(dataColorsFor(ColorPalette.HIGH_CONTRAST, dark = true))
    }

    @Test
    @Config(qualifiers = "night")
    fun lightMode_ignoresSystemDark() {
        var background: Color? = null
        rule.setContent {
            GetFitTheme(themeMode = ThemeMode.LIGHT) {
                background = MaterialTheme.colorScheme.background
            }
        }
        rule.waitForIdle()

        assertThat(background).isEqualTo(Color(0xFFF7F7F8))
    }
}
