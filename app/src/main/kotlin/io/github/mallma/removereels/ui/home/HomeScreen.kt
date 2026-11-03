package io.github.mallma.removereels.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mallma.removereels.R
import io.github.mallma.removereels.ui.AppIcon
import io.github.mallma.removereels.ui.label
import io.github.mallma.removereels.ui.surfaceLabel
import kotlin.time.Duration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    state: HomeState,
    onTurnOn: () -> Unit,
    onOpenDateSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusCard(state.blockingOn, onTurnOn)
            if (!state.automaticTimeOn) AutomaticTimeBanner(onOpenDateSettings)
            state.cards.forEach { AppCardView(it) }
            if (state.allowedToday.isPositive()) {
                Text(
                    text =
                        stringResource(
                            R.string.home_today_total,
                            state.usedToday.inWholeMinutes,
                            state.allowedToday.inWholeMinutes,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    blockingOn: Boolean,
    onTurnOn: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = if (blockingOn) colors.secondaryContainer else colors.errorContainer,
                contentColor = if (blockingOn) colors.onSecondaryContainer else colors.onErrorContainer,
            ),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(12.dp)
                    .background(if (blockingOn) colors.primary else colors.error, CircleShape),
            )
            Text(
                text = stringResource(if (blockingOn) R.string.home_blocking_on else R.string.home_blocking_off),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 12.dp).weight(1f),
            )
            if (!blockingOn) {
                Button(onClick = onTurnOn) { Text(stringResource(R.string.home_turn_on)) }
            }
        }
    }
}

@Composable
private fun AutomaticTimeBanner(onOpenDateSettings: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.block_automatic_time_off), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onOpenDateSettings, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.home_open_date_settings))
            }
        }
    }
}

@Composable
private fun AppCardView(card: AppCard) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            AppIcon(card.app)
            Column(modifier = Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(card.app.label), style = MaterialTheme.typography.titleMedium)
                Text(card.statusLine(), style = MaterialTheme.typography.bodyMedium)
                if (card.installed && !card.today.isBlocked) {
                    LinearProgressIndicator(
                        progress = { (card.used / card.today.duration).toFloat() },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    )
                }
                card.fromTomorrow?.let { allowance ->
                    Text(
                        text =
                            if (allowance.isBlocked) {
                                stringResource(R.string.home_from_tomorrow_blocked)
                            } else {
                                stringResource(R.string.home_from_tomorrow_minutes, allowance.minutes)
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (card.installed && card.rulesStale) {
                    Text(
                        text = stringResource(R.string.home_rules_stale),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppCard.statusLine(): String {
    val surface = stringResource(app.surfaceLabel)
    return when {
        !installed -> stringResource(R.string.home_not_installed)
        today.isBlocked -> stringResource(R.string.home_surface_blocked, surface)
        !left.isPositive() -> stringResource(R.string.home_surface_used_up, surface)
        else -> stringResource(R.string.home_surface_left, surface, left.minutesRoundedUp(), today.minutes)
    }
}

/** Whole minutes, rounded up, so that "1 min left" shows until the last second. */
private fun Duration.minutesRoundedUp(): Long = (inWholeSeconds + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE

private const val SECONDS_PER_MINUTE = 60
