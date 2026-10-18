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
        is BodyCard.Range -> rangeSpoken(card)
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
                is BodyCard.Range -> RangeBody(card)
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
    Text(
        text = stringResource(R.string.home_body_trend, formatOneDecimal(card.trend), symbol),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun RangeBody(card: BodyCard.Range) {
    val symbol: String = massUnitSymbol(card.unit)
    CardLabel(stringResource(R.string.home_body_weight_trend))
    val change = card.change
    if (change == null) {
        Text(text = stringResource(R.string.home_body_not_enough), style = MaterialTheme.typography.bodyMedium)
    } else {
        Text(
            text = stringResource(
                R.string.home_body_change,
                formatOneDecimal(change.start),
                formatOneDecimal(change.end),
                symbol,
            ),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.home_body_rate, formatSignedOneDecimal(change.perWeek), symbol),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun CardLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun todaySpoken(card: BodyCard.Today, today: LocalDate): String {
    val spokenUnit: String = massUnitSpoken(card.unit)
    val sentence: String = stringResource(
        R.string.home_body_spoken_today,
        formatOneDecimal(card.latest),
        spokenUnit,
        formatOneDecimal(card.trend),
        spokenUnit,
    )
    return if (card.latestDate == today) sentence else "$sentence, ${dateLabel(card.latestDate, today)}"
}

@Composable
private fun rangeSpoken(card: BodyCard.Range): String {
    val change = card.change ?: return stringResource(R.string.home_body_spoken_not_enough)
    val spokenUnit: String = massUnitSpoken(card.unit)
    return stringResource(
        R.string.home_body_spoken_range,
        formatOneDecimal(change.start),
        formatOneDecimal(change.end),
        spokenUnit,
        formatSignedOneDecimal(change.perWeek),
        spokenUnit,
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
                body = BodyCard.Today(latest = 212.4, latestDate = today, trend = 213.1, unit = MassUnit.POUND),
            ),
            onRangeSelected = {},
            onOpenBody = {},
        )
    }
}
