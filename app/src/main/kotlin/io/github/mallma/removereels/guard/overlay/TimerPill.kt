package io.github.mallma.removereels.guard.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mallma.removereels.R
import io.github.mallma.removereels.ui.theme.warningColors
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** From this much time left, the pill turns amber. */
private val LOW_TIME = 1.minutes

/** The time left today while watching within an allowance; tapping it hides it until the screen closes. */
@Composable
internal fun TimerPill(
    remaining: Duration,
    onHide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val low = remaining <= LOW_TIME
    val warning = warningColors()
    Surface(
        modifier = modifier.statusBarsPadding().padding(top = 8.dp),
        shape = CircleShape,
        color = if (low) warning.container else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (low) warning.content else MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier =
                Modifier
                    .clickable(onClickLabel = stringResource(R.string.time_left_hide), onClick = onHide)
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_timer), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.time_left, formatTimeLeft(remaining)),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/** Minutes and seconds, such as `4:32`, rounded up so that the pill never shows `0:00` while time is left. */
internal fun formatTimeLeft(remaining: Duration): String {
    val seconds = (remaining.inWholeMilliseconds + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND
    return "${seconds / SECONDS_PER_MINUTE}:${(seconds % SECONDS_PER_MINUTE).toString().padStart(2, '0')}"
}

private const val MILLIS_PER_SECOND = 1000
private const val SECONDS_PER_MINUTE = 60
