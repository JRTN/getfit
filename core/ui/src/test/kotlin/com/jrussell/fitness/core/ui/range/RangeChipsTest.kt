package com.jrussell.fitness.core.ui.range

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jrussell.fitness.core.designsystem.theme.GetFitTheme
import com.jrussell.fitness.core.domain.range.DateRangeSelection
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RangeChipsTest {
    @get:Rule
    val rule: ComposeContentTestRule = createComposeRule()

    @Test
    fun chips_exposeRadioRoleAndSelection() {
        show(onSelect = {})

        rule.onNodeWithText("Today").assertIsSelected().assert(isRadioButton())
        rule.onNodeWithText("7 days").assertIsNotSelected().assert(isRadioButton())
        rule.onNodeWithText("30 days").assertIsNotSelected()
        rule.onNodeWithText("From…").assertIsNotSelected()
    }

    @Test
    fun clickingSevenDays_selectsLastSevenDays() {
        var selected: DateRangeSelection = DateRangeSelection.Today
        show(onSelect = { selection: DateRangeSelection -> selected = selection })

        rule.onNodeWithText("7 days").performClick()

        assertThat(selected).isEqualTo(DateRangeSelection.LastDays(7))
    }

    private fun show(onSelect: (DateRangeSelection) -> Unit) {
        rule.setContent {
            GetFitTheme {
                RangeChips(selection = DateRangeSelection.Today, onSelect = onSelect, today = LocalDate(2026, 10, 17))
            }
        }
    }

    private fun isRadioButton(): SemanticsMatcher {
        return SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
    }
}
