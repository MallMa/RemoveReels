package io.github.mallma.removereels.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.mallma.removereels.ui.allowance.AllowanceRoute
import io.github.mallma.removereels.ui.home.HomeRoute
import io.github.mallma.removereels.ui.keeprunning.KeepRunningRoute
import io.github.mallma.removereels.ui.settings.AboutRoute
import io.github.mallma.removereels.ui.settings.SettingsRoute
import io.github.mallma.removereels.ui.tryit.TryItRoute

/** The app's screens and how they lead to each other. */
@Composable
fun RemoveReelsApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Route.Home, modifier = modifier) {
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
                onTestBlocking = { navController.navigate(Route.TestBlocking) },
                onKeepRunning = { navController.navigate(Route.KeepRunning) },
                onAbout = { navController.navigate(Route.About) },
                onBack = navController::navigateUp,
            )
        }
        composable<Route.TestBlocking> {
            TryItRoute(onDone = navController::navigateUp, onBack = navController::navigateUp)
        }
        composable<Route.KeepRunning> { KeepRunningRoute(onBack = navController::navigateUp) }
        composable<Route.About> { AboutRoute(onBack = navController::navigateUp) }
    }
}
