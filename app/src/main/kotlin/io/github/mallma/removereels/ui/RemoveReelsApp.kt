package io.github.mallma.removereels.ui

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.mallma.removereels.container
import io.github.mallma.removereels.ui.allowance.AllowanceRoute
import io.github.mallma.removereels.ui.home.HomeRoute
import io.github.mallma.removereels.ui.keeprunning.KeepRunningRoute
import io.github.mallma.removereels.ui.keeprunning.PhoneMaker
import io.github.mallma.removereels.ui.onboarding.HowItWorksStep
import io.github.mallma.removereels.ui.onboarding.NotificationsRoute
import io.github.mallma.removereels.ui.onboarding.TurnOnRoute
import io.github.mallma.removereels.ui.onboarding.WelcomeStep
import io.github.mallma.removereels.ui.settings.AboutRoute
import io.github.mallma.removereels.ui.settings.SettingsRoute
import io.github.mallma.removereels.ui.tryit.TryItRoute

/**
 * The app's screens and how they lead to each other. The first start runs onboarding: Welcome, How it works,
 * Notifications, Turn on, the phone maker's steps where needed, and Try it. [openTurnOn] comes from the "Blocking is
 * off" notification and opens the Turn on screen; [onTurnOnOpened] reports that it did.
 */
@Composable
fun RemoveReelsApp(
    modifier: Modifier = Modifier,
    openTurnOn: Boolean = false,
    onTurnOnOpened: () -> Unit = {},
) {
    val context = LocalContext.current
    val viewModel = viewModel { AppViewModel(context.container.stateStore) }
    val onboardingDone by viewModel.onboardingDone.collectAsStateWithLifecycle()
    // Chosen once, when the stored state is known; finishing onboarding then navigates to Home.
    val start =
        remember(onboardingDone == null) {
            onboardingDone?.let { done ->
                if (done) Route.Home else Route.Welcome
            }
        }
    if (start != null) {
        val navController = rememberNavController()
        LaunchedEffect(openTurnOn) {
            if (openTurnOn) {
                if (start == Route.Home) navController.navigate(Route.TurnOn(inOnboarding = false))
                onTurnOnOpened()
            }
        }
        NavHost(navController = navController, startDestination = start, modifier = modifier) {
            onboarding(navController) {
                viewModel.finishOnboarding()
                navController.navigate(Route.Home) { popUpTo(navController.graph.id) { inclusive = true } }
            }
            composable<Route.Home> {
                HomeRoute(
                    onOpenAllowance = { navController.navigate(Route.Allowance) },
                    onOpenSettings = { navController.navigate(Route.Settings) },
                )
            }
            composable<Route.Allowance> { AllowanceRoute(onBack = navController::navigateUp) }
            composable<Route.Settings> {
                SettingsRoute(
                    onOpenAllowance = { navController.navigate(Route.Allowance) },
                    onTestBlocking = { navController.navigate(Route.TestBlocking(inOnboarding = false)) },
                    onKeepRunning = { navController.navigate(Route.KeepRunning(inOnboarding = false)) },
                    onAbout = { navController.navigate(Route.About) },
                    onBack = navController::navigateUp,
                )
            }
            composable<Route.About> { AboutRoute(onBack = navController::navigateUp) }
        }
    }
}

/** The onboarding steps. Turn on, Keep running and Test blocking are also reachable later and then return. */
private fun NavGraphBuilder.onboarding(
    navController: NavController,
    finish: () -> Unit,
) {
    val back: () -> Unit = { navController.navigateUp() }
    composable<Route.Welcome> { WelcomeStep(onGetStarted = { navController.navigate(Route.HowItWorks) }) }
    composable<Route.HowItWorks> { HowItWorksStep(onContinue = { navController.navigate(Route.Notifications) }) }
    composable<Route.Notifications> {
        NotificationsRoute(onContinue = { navController.navigate(Route.TurnOn(inOnboarding = true)) })
    }
    composable<Route.TurnOn> { entry ->
        val inOnboarding = entry.toRoute<Route.TurnOn>().inOnboarding
        val next =
            if (PhoneMaker
                    .of(
                        Build.MANUFACTURER,
                    ).stopsServices
            ) {
                Route.KeepRunning(true)
            } else {
                Route.TestBlocking(true)
            }
        TurnOnRoute(onContinue = if (inOnboarding) ({ navController.navigate(next) }) else back)
    }
    composable<Route.KeepRunning> { entry ->
        val inOnboarding = entry.toRoute<Route.KeepRunning>().inOnboarding
        val next: () -> Unit = { navController.navigate(Route.TestBlocking(inOnboarding = true)) }
        KeepRunningRoute(onContinue = next.takeIf { inOnboarding }, onBack = back)
    }
    composable<Route.TestBlocking> { entry ->
        val inOnboarding = entry.toRoute<Route.TestBlocking>().inOnboarding
        TryItRoute(onDone = if (inOnboarding) finish else back, onBack = back)
    }
}
