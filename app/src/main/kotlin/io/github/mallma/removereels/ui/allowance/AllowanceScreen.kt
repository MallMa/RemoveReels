package io.github.mallma.removereels.ui.allowance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.github.mallma.removereels.R
import io.github.mallma.removereels.budget.Allowance
import io.github.mallma.removereels.budget.Allowance.Companion.DAILY_MAXIMUM_MINUTES
import io.github.mallma.removereels.budget.Allowance.Companion.STEP_MINUTES
import io.github.mallma.removereels.budget.TargetApp
import io.github.mallma.removereels.ui.AppIcon
import io.github.mallma.removereels.ui.label
import io.github.mallma.removereels.ui.shortDate
import io.github.mallma.removereels.ui.surfaceLabel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AllowanceScreen(
    state: AllowanceEditorState,
    onBlock: (TargetApp) -> Unit,
    onAllow: (TargetApp, Int) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.allowance_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(R.string.allowance_today)
            TargetApp.entries.forEach { TodayRow(it, state.today[it]) }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionTitle(R.string.allowance_from_tomorrow)
            TargetApp.entries.forEach { app -> AppEditor(app, state, onBlock, onAllow) }
            SharedMaximum(state)
            Text(
                text = stringResource(R.string.allowance_starts, shortDate(state.startsOn)),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onSave, enabled = state.hasChanges, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.allowance_save))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: Int) {
    Text(stringResource(text), style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun appSurface(app: TargetApp): String =
    stringResource(R.string.allowance_app_surface, stringResource(app.label), stringResource(app.surfaceLabel))

@Composable
private fun minutesText(allowance: Allowance): String =
    if (allowance.isBlocked) {
        stringResource(R.string.allowance_blocked)
    } else {
        stringResource(R.string.allowance_minutes, allowance.minutes)
    }

@Composable
private fun TodayRow(
    app: TargetApp,
    allowance: Allowance,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(appSurface(app), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(minutesText(allowance), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AppEditor(
    app: TargetApp,
    state: AllowanceEditorState,
    onBlock: (TargetApp) -> Unit,
    onAllow: (TargetApp, Int) -> Unit,
) {
    val allowance = state.draft[app]
    val maximum = state.draft.maximumMinutesFor(app)
    Column(modifier = Modifier.selectableGroup()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app, size = 32.dp)
            Text(
                appSurface(app),
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Choice(R.string.allowance_block_completely, selected = allowance.isBlocked, enabled = true) { onBlock(app) }
        Choice(R.string.allowance_allow, selected = !allowance.isBlocked, enabled = state.canAllow(app)) {
            onAllow(app, STEP_MINUTES)
        }
        when {
            !state.canAllow(app) -> {
                Text(
                    text = stringResource(R.string.allowance_lower_other_first, stringResource(app.other.label)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 48.dp),
                )
            }

            !allowance.isBlocked -> {
                MinutesSlider(app, allowance, maximum, onAllow)
            }
        }
    }
}

@Composable
private fun Choice(
    text: Int,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(
            stringResource(text),
            modifier = Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun MinutesSlider(
    app: TargetApp,
    allowance: Allowance,
    maximum: Int,
    onAllow: (TargetApp, Int) -> Unit,
) {
    val description = stringResource(R.string.allowance_slider_description, appSurface(app))
    val stateText =
        pluralStringResource(R.plurals.allowance_slider_state, allowance.minutes, allowance.minutes, maximum)
    Row(modifier = Modifier.padding(start = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        if (maximum > STEP_MINUTES) {
            Slider(
                value = allowance.minutes.toFloat(),
                onValueChange = { onAllow(app, it.roundToInt()) },
                valueRange = STEP_MINUTES.toFloat()..maximum.toFloat(),
                steps = (maximum - STEP_MINUTES) / STEP_MINUTES - 1,
                modifier =
                    Modifier.weight(1f).semantics {
                        contentDescription = description
                        stateDescription = stateText
                    },
            )
        }
        Text(
            text = stringResource(R.string.allowance_minutes, allowance.minutes),
            modifier = Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun SharedMaximum(state: AllowanceEditorState) {
    val instagram = state.draft.instagram.minutes
    val youtube = state.draft.youtube.minutes
    val free = DAILY_MAXIMUM_MINUTES - instagram - youtube
    val colors = MaterialTheme.colorScheme
    val description = stringResource(R.string.allowance_bar_description, instagram, youtube, free)
    Text(
        text = stringResource(R.string.allowance_shared_maximum, DAILY_MAXIMUM_MINUTES),
        style = MaterialTheme.typography.bodyMedium,
    )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .clearAndSetSemantics { contentDescription = description },
    ) {
        Segment(
            instagram,
            stringResource(R.string.allowance_bar_instagram, instagram),
            colors.primary,
            colors.onPrimary,
        )
        Segment(youtube, stringResource(R.string.allowance_bar_youtube, youtube), colors.tertiary, colors.onTertiary)
        Segment(free, stringResource(R.string.allowance_bar_free, free), colors.surfaceVariant, colors.onSurfaceVariant)
    }
}

@Composable
private fun RowScope.Segment(
    minutes: Int,
    label: String,
    color: Color,
    contentColor: Color,
) {
    if (minutes > 0) {
        Box(
            modifier = Modifier.weight(minutes.toFloat()).height(36.dp).background(color),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, color = contentColor, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

private val TargetApp.other: TargetApp
    get() =
        when (this) {
            TargetApp.INSTAGRAM -> TargetApp.YOUTUBE
            TargetApp.YOUTUBE -> TargetApp.INSTAGRAM
        }
