package io.github.mallma.removereels.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.mallma.removereels.R

/** The layout every onboarding step shares: a title, the step's content, and its buttons at the bottom. */
@Composable
internal fun OnboardingStep(
    title: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding().padding(24.dp)) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.padding(top = 24.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                content()
            }
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth()) { Text(primaryLabel) }
                secondaryLabel?.let { TextButton(onClick = onSecondary) { Text(it) } }
            }
        }
    }
}

@Composable
private fun BodyText(text: Int) {
    Text(stringResource(text), style = MaterialTheme.typography.bodyLarge)
}

@Composable
internal fun WelcomeStep(onGetStarted: () -> Unit) {
    OnboardingStep(
        title = stringResource(R.string.welcome_title),
        primaryLabel = stringResource(R.string.welcome_get_started),
        onPrimary = onGetStarted,
    ) {
        BodyText(R.string.welcome_works)
        BodyText(R.string.welcome_blocked)
        BodyText(R.string.welcome_allowance)
        Text(stringResource(R.string.welcome_no_off_switch), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
internal fun HowItWorksStep(onContinue: () -> Unit) {
    OnboardingStep(
        title = stringResource(R.string.how_it_works_title),
        primaryLabel = stringResource(R.string.onboarding_continue),
        onPrimary = onContinue,
    ) {
        BodyText(R.string.how_it_works_body)
        BodyText(R.string.how_it_works_two_apps)
        BodyText(R.string.how_it_works_saves_nothing)
        BodyText(R.string.how_it_works_no_internet)
    }
}

@Composable
internal fun NotificationsStep(
    granted: Boolean,
    onAllow: () -> Unit,
    onContinue: () -> Unit,
) {
    OnboardingStep(
        title = stringResource(R.string.notifications_title),
        primaryLabel = stringResource(if (granted) R.string.onboarding_continue else R.string.notifications_allow),
        onPrimary = if (granted) onContinue else onAllow,
        secondaryLabel = if (granted) null else stringResource(R.string.notifications_not_now),
        onSecondary = onContinue,
    ) {
        BodyText(R.string.notifications_body)
        if (granted) BodyText(R.string.notifications_granted)
    }
}

/**
 * Sends the user to Android's settings to switch the service on. Apps installed from a downloaded file need an extra
 * step first, because Android 13 and later treat the accessibility switch as a restricted setting for them.
 */
@Composable
internal fun TurnOnStep(
    blockingOn: Boolean,
    installedFromFile: Boolean,
    onOpenSettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onContinue: () -> Unit,
) {
    if (blockingOn) {
        OnboardingStep(
            title = stringResource(R.string.turn_on_done_title),
            primaryLabel = stringResource(R.string.onboarding_continue),
            onPrimary = onContinue,
        ) {
            BodyText(R.string.turn_on_done_body)
        }
    } else {
        OnboardingStep(
            title = stringResource(R.string.turn_on_title),
            primaryLabel = stringResource(R.string.turn_on_open_settings),
            onPrimary = onOpenSettings,
            secondaryLabel = if (installedFromFile) stringResource(R.string.turn_on_open_app_info) else null,
            onSecondary = onOpenAppInfo,
        ) {
            if (installedFromFile) {
                BodyText(R.string.turn_on_restricted)
                BodyText(R.string.turn_on_restricted_steps)
            }
            BodyText(R.string.turn_on_steps)
        }
    }
}
