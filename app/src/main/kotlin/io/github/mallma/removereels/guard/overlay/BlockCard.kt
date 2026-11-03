package io.github.mallma.removereels.guard.overlay

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.mallma.removereels.R
import io.github.mallma.removereels.budget.BlockReason
import io.github.mallma.removereels.budget.TargetApp

/** Covers the blocked screen while the app backs out of it underneath, and says what still works. */
@Composable
internal fun BlockCard(
    notice: BlockNotice,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    LaunchedEffect(notice) { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
    Surface(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(32.dp).semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_pause),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(notice.app.blockedTitle),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            notice.lines().forEach { line ->
                Spacer(Modifier.height(16.dp))
                Text(text = line, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(32.dp))
            Button(onClick = onContinue) {
                Text(stringResource(notice.app.continueLabel))
            }
        }
    }
}

@Composable
private fun BlockNotice.lines(): List<String> =
    listOfNotNull(
        when (reason) {
            is BlockReason.UsedUp -> stringResource(app.usedUp, reason.allowance.minutes)
            BlockReason.AutomaticTimeOff -> stringResource(R.string.block_automatic_time_off)
            BlockReason.NoAllowance -> null
        },
        stringResource(app.stillWorks),
        if (openedFromMessages) stringResource(R.string.block_from_messages) else null,
    )

private val TargetApp.blockedTitle: Int
    get() =
        when (this) {
            TargetApp.INSTAGRAM -> R.string.block_title_instagram
            TargetApp.YOUTUBE -> R.string.block_title_youtube
        }

private val TargetApp.usedUp: Int
    get() =
        when (this) {
            TargetApp.INSTAGRAM -> R.string.block_used_up_instagram
            TargetApp.YOUTUBE -> R.string.block_used_up_youtube
        }

private val TargetApp.stillWorks: Int
    get() =
        when (this) {
            TargetApp.INSTAGRAM -> R.string.block_still_works_instagram
            TargetApp.YOUTUBE -> R.string.block_still_works_youtube
        }

private val TargetApp.continueLabel: Int
    get() =
        when (this) {
            TargetApp.INSTAGRAM -> R.string.block_continue_instagram
            TargetApp.YOUTUBE -> R.string.block_continue_youtube
        }
