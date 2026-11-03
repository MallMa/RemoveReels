package io.github.mallma.removereels.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.mallma.removereels.ui.allowance.AllowanceRoute
import io.github.mallma.removereels.ui.home.HomeRoute

/** The app's screens and how they lead to each other. */
@Composable
fun RemoveReelsApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Route.Home, modifier = modifier) {
        composable<Route.Home> { HomeRoute(onOpenAllowance = { navController.navigate(Route.Allowance) }) }
        composable<Route.Allowance> { AllowanceRoute(onBack = navController::navigateUp) }
    }
}
