package com.jrussell.fitness.feature.home

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import assertk.assertThat
import assertk.assertions.isTrue
import com.jrussell.fitness.core.designsystem.theme.GetFitTheme
import com.jrussell.fitness.core.domain.metric.RangeChange
import com.jrussell.fitness.core.domain.range.DateRangeSelection
import com.jrussell.fitness.core.model.MassUnit
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeScreenSemanticsTest {
    @get:Rule
    val rule: ComposeContentTestRule = createComposeRule()

    private val today: LocalDate = LocalDate(2026, 10, 17)
    private val todayCard: BodyCard = BodyCard.Today(
        latest = 212.4,
        latestDate = today,
        perWeek = -1.2,
        since = LocalDate(2026, 10, 11),
        unit = MassUnit.POUND,
    )

    @Test
    fun rangeChips_present() {
        show(todayCard, onOpenBody = {})

        rule.onNodeWithText("Today").assertIsDisplayed()
        rule.onNodeWithText("30 days").assertIsDisplayed()
    }

    @Test
    fun bodyCard_click_callsOnOpenBody() {
        var opened: Boolean = false
        show(todayCard, onOpenBody = { opened = true })

        rule.onNodeWithContentDescription("Weight", substring = true).performClick()

        assertThat(opened).isTrue()
    }

    @Test
    fun bodyCard_mergesSemantics() {
        show(todayCard, onOpenBody = {})

        rule.onNodeWithContentDescription("Weight 212.4 pounds, −1.2 pounds per week since Oct 11", substring = true)
            .assertHasClickAction()
    }

    @Test
    fun rangeCard_showsChangeAndRate() {
        show(
            BodyCard.Range(RangeChange(216.0, 212.4, -3.6), since = LocalDate(2026, 9, 18), MassUnit.POUND),
            onOpenBody = {},
        )

        rule.onNodeWithText("−3.6 lb/week since Sep 18", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithContentDescription(
            "Weight 216.0 to 212.4 pounds, −3.6 pounds per week since Sep 18",
        ).assertIsDisplayed()
    }

    private fun show(body: BodyCard?, onOpenBody: () -> Unit) {
        rule.setContent {
            GetFitTheme {
                HomeContent(
                    state = HomeUiState(today = today, selection = DateRangeSelection.Today, body = body),
                    onRangeSelected = {},
                    onOpenBody = onOpenBody,
                )
            }
        }
    }
}
