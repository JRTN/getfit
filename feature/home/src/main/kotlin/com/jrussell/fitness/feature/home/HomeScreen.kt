package com.jrussell.fitness.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jrussell.fitness.core.designsystem.theme.GetFitTheme
import com.jrussell.fitness.core.domain.range.DateRangeSelection
import com.jrussell.fitness.core.model.MassUnit
import com.jrussell.fitness.core.ui.format.formatOneDecimal
import com.jrussell.fitness.core.ui.format.formatShortDate
import com.jrussell.fitness.core.ui.format.formatSignedOneDecimal
import com.jrussell.fitness.core.ui.format.massUnitSpoken
import com.jrussell.fitness.core.ui.format.massUnitSymbol
import com.jrussell.fitness.core.ui.range.RangeChips
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate

private val SCREEN_PADDING: Dp = 16.dp
private val CARD_PADDING: Dp = 16.dp
private val HAIRLINE: Dp = 1.dp

/**
 * Home: where you stand, at a glance (decision #38). Read-only; tapping a card opens the tab that
 * owns its data. Nothing is drawn until the first state loads.
 *
 * @param onOpenBody switches to the Body tab (supplied by `:app`; features never navigate to each other).
 */
@Composable
fun HomeScreen(onOpenBody: () -> Unit, modifier: Modifier = Modifier, viewModel: HomeViewModel = hiltViewModel()) {
    val state: HomeUiState? by viewModel.uiState.collectAsStateWithLifecycle()
    state?.let { loaded: HomeUiState ->
        HomeContent(
            state = loaded,
            onRangeSelected = viewModel::onRangeSelected,
            onOpenBody = onOpenBody,
            modifier = modifier,
        )
    }
}

/** Home's stateless content: range chips, then one card per domain with data. */
@Composable
fun HomeContent(
    state: HomeUiState,
    onRangeSelected: (DateRangeSelection) -> Unit,
    onOpenBody: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(SCREEN_PADDING),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RangeChips(selection = state.selection, onSelect = onRangeSelected, today = state.today)
        val body: BodyCard? = state.body
        if (body == null) {
            Text(
                text = stringResource(R.string.home_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            BodyCardView(card = body, today = state.today, onClick = onOpenBody)
        }
    }
}

/** The Body card: one tappable target, announced as a single sentence by TalkBack. */
@Composable
private fun BodyCardView(card: BodyCard, today: LocalDate, onClick: () -> Unit) {
    val spoken: String = when (card) {
        is BodyCard.Today -> todaySpoken(card, today)
        is BodyCard.Range -> rangeSpoken(card, today)
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(HAIRLINE, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = spoken },
    ) {
        Column(modifier = Modifier.padding(CARD_PADDING), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when (card) {
                is BodyCard.Today -> TodayBody(card, today)
                is BodyCard.Range -> RangeBody(card, today)
            }
        }
    }
}

@Composable
private fun TodayBody(card: BodyCard.Today, today: LocalDate) {
    val symbol: String = massUnitSymbol(card.unit)
    CardLabel(stringResource(R.string.home_body_weight))
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "${formatOneDecimal(card.latest)} $symbol", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = dateLabel(card.latestDate, today),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    RateText(card.perWeek, symbol, formatShortDate(card.since, today))
}

@Composable
private fun RangeBody(card: BodyCard.Range, today: LocalDate) {
    val symbol: String = massUnitSymbol(card.unit)
    CardLabel(stringResource(R.string.home_body_weight))
    Text(
        text = stringResource(
            R.string.home_body_change,
            formatOneDecimal(card.change.start),
            formatOneDecimal(card.change.end),
            symbol,
        ),
        style = MaterialTheme.typography.headlineSmall,
    )
    RateText(card.change.perWeek, symbol, formatShortDate(card.since, today))
}

/** The weight trend and where it is measured from, "−1.2 lb/week since Oct 11". */
@Composable
private fun RateText(perWeek: Double, symbol: String, since: String) {
    Text(
        text = stringResource(R.string.home_body_rate, formatSignedOneDecimal(perWeek), symbol, since),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun CardLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun todaySpoken(card: BodyCard.Today, today: LocalDate): String {
    val sentence: String = stringResource(
        R.string.home_body_spoken_today,
        formatOneDecimal(card.latest),
        massUnitSpoken(card.unit),
        formatSignedOneDecimal(card.perWeek),
        formatShortDate(card.since, today),
    )
    return if (card.latestDate == today) sentence else "$sentence, ${dateLabel(card.latestDate, today)}"
}

@Composable
private fun rangeSpoken(card: BodyCard.Range, today: LocalDate): String {
    return stringResource(
        R.string.home_body_spoken_range,
        formatOneDecimal(card.change.start),
        formatOneDecimal(card.change.end),
        massUnitSpoken(card.unit),
        formatSignedOneDecimal(card.change.perWeek),
        formatShortDate(card.since, today),
    )
}

/** "today", or the date in the locale's medium format when the reading is older. */
@Composable
private fun dateLabel(date: LocalDate, today: LocalDate): String {
    return if (date == today) {
        stringResource(R.string.home_body_today)
    } else {
        date.toJavaLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun HomeContentPreview() {
    val today: LocalDate = LocalDate(2026, 10, 17)
    GetFitTheme {
        HomeContent(
            state = HomeUiState(
                today = today,
                selection = DateRangeSelection.Today,
                body = BodyCard.Today(
                    212.4,
                    today,
                    perWeek = -1.2,
                    since = LocalDate(2026, 10, 11),
                    unit = MassUnit.POUND,
                ),
            ),
            onRangeSelected = {},
            onOpenBody = {},
        )
    }
}
