package com.jrussell.fitness.feature.body

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jrussell.fitness.core.designsystem.theme.GetFitTheme
import com.jrussell.fitness.core.domain.range.DateRangeSelection
import com.jrussell.fitness.core.model.MassUnit
import com.jrussell.fitness.core.model.ids.MeasurementId
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BodyScreenSemanticsTest {
    @get:Rule
    val rule: ComposeContentTestRule = createComposeRule()

    private val today: LocalDate = LocalDate(2026, 10, 17)
    private val row: WeightRow = WeightRow(MeasurementId(Uuid.random()), today, LocalTime(8, 0), 212.4)

    @Test
    fun logTrends_exposeSelection() {
        show(state())

        rule.onNodeWithText("Log").assertIsSelected()
        rule.onNodeWithText("Trends").assertIsNotSelected()

        rule.onNodeWithText("Trends").performClick()

        rule.onNodeWithText("Trends").assertIsSelected()
        rule.onNodeWithText("Log").assertIsNotSelected()
    }

    @Test
    fun addWeight_dialogFieldError_isAnnounced() {
        show(state(entry = WeightEntryState(error = WeightInputError.OUT_OF_RANGE)))

        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Enter a weight between 45 and 881 lb"))
            .assertIsDisplayed()
    }

    @Test
    fun historyRow_offersDeleteAsCustomAction() {
        show(state(history = listOf(row)))

        rule.onNode(
            SemanticsMatcher("has a Delete custom action") { node ->
                node.config.getOrElseNullable(SemanticsActions.CustomActions) { null }.orEmpty().any {
                    it.label ==
                        "Delete"
                }
            },
        ).assertIsDisplayed()
    }

    @Test
    fun latestCard_showsAndReadsTheWeeklyRate() {
        show(state(history = listOf(row)))

        rule.onNodeWithText("−1.2 lb/week since Oct 11", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithContentDescription("Latest weight 212.4 pounds, today, −1.2 pounds per week since Oct 11")
            .assertIsDisplayed()
    }

    @Test
    fun logView_hasTheRangeChips() {
        var picked: DateRangeSelection? = null
        show(state(history = listOf(row)), BodyActions(onRangeSelected = { picked = it }))

        rule.onNodeWithText("30 days").performClick()

        assertThat(picked).isEqualTo(DateRangeSelection.LastDays(30))
        rule.onNodeWithText("Today").assertDoesNotExist()
    }

    @Test
    fun emptyRange_saysSoInsteadOfAddYourFirst() {
        show(state(history = emptyList()).copy(latest = LatestWeight(212.4, today, 0.0, today)))

        rule.onNodeWithText("No weigh-ins in this range").assertIsDisplayed()
    }

    private fun state(entry: WeightEntryState? = null, history: List<WeightRow> = emptyList()): BodyUiState {
        return BodyUiState(
            today = today,
            unit = MassUnit.POUND,
            latest = history.firstOrNull()?.let {
                LatestWeight(it.value, it.date, perWeek = -1.2, since = LocalDate(2026, 10, 11))
            },
            history = history,
            selection = DateRangeSelection.All,
            chartRange = LocalDate(2026, 10, 11)..today,
            chartWeighIns = emptyList(),
            entry = entry,
        )
    }

    private fun show(state: BodyUiState, actions: BodyActions = BodyActions.None) {
        rule.setContent {
            GetFitTheme {
                BodyContent(state = state, actions = actions)
            }
        }
    }
}
