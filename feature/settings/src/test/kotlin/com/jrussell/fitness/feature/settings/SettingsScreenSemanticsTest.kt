package com.jrussell.fitness.feature.settings

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.jrussell.fitness.core.designsystem.theme.GetFitTheme
import com.jrussell.fitness.core.model.ColorPalette
import com.jrussell.fitness.core.model.MassUnit
import com.jrussell.fitness.core.model.ThemeMode
import com.jrussell.fitness.core.model.UserPreferences
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** TalkBack must announce each Appearance option as a radio button with its selected state. */
@RunWith(RobolectricTestRunner::class)
class SettingsScreenSemanticsTest {
    @get:Rule
    val rule: ComposeContentTestRule = createComposeRule()

    @Test
    fun appearanceOptions_exposeRadioRoleAndSelection() {
        rule.setContent {
            GetFitTheme {
                AppearanceSettings(
                    state = UserPreferences(ThemeMode.DARK, ColorPalette.HIGH_CONTRAST),
                    onThemeModeSelected = {},
                    onPaletteSelected = {},
                )
            }
        }

        val isRadioButton: SemanticsMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        val selected: List<String> = listOf("Dark", "High contrast")
        val notSelected: List<String> = listOf("System", "Light", "Default", "Color-vision-safe")
        selected.forEach { label: String ->
            rule.onNodeWithText(label).assert(isRadioButton).assertIsSelected()
        }
        notSelected.forEach { label: String ->
            rule.onNodeWithText(label).assert(isRadioButton).assertIsNotSelected()
        }
    }

    @Test
    fun units_exposeRadioRoleAndSelection() {
        rule.setContent {
            GetFitTheme {
                UnitsSettings(state = UserPreferences.DEFAULT.copy(massUnit = MassUnit.POUND), onMassUnitSelected = {})
            }
        }

        val isRadioButton: SemanticsMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        rule.onNodeWithText("Pounds").assert(isRadioButton).assertIsSelected()
        rule.onNodeWithText("Kilograms").assert(isRadioButton).assertIsNotSelected()
    }

    @Test
    fun chartsRow_opensTheChartsPage() {
        var opened: Boolean = false
        rule.setContent {
            GetFitTheme {
                ChartsRow(onClick = { opened = true })
            }
        }

        rule.onNodeWithText("Charts").assertHasClickAction().performClick()

        assertThat(opened).isTrue()
    }

    @Test
    fun trendLinesSwitch_exposesSwitchRoleAndState() {
        var changedTo: Boolean? = null
        rule.setContent {
            GetFitTheme {
                ChartSettings(
                    state = UserPreferences.DEFAULT.copy(showTrendLines = true),
                    onShowTrendLinesChanged = { show: Boolean -> changedTo = show },
                )
            }
        }

        val isSwitch: SemanticsMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)
        rule.onNodeWithText("Trend lines").assert(isSwitch).assertIsOn().performClick()

        assertThat(changedTo).isEqualTo(false)
    }
}
