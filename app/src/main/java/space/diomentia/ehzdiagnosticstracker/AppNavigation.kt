package space.diomentia.ehzdiagnosticstracker

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import space.diomentia.ehzdiagnosticstracker.data.DiagnosticViewModel
import space.diomentia.ehzdiagnosticstracker.ui.screens.AuthorizationScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.DiagnosticMenuScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.DiagnosticObjectsScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.DiagnosticPointsScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.EditProfileScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.InitialScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.OutOfServiceScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.ReportHistoryScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.SettingsScreen
import java.time.LocalDate

sealed class Screen(val route: String) {
    object Initial : Screen("initial")
    object OutOfService : Screen("out_of_service")
    object Authorization : Screen("authorization")
    object ProfileEdit : Screen("profile_edit") {
        // TODO: don't depend on app state
        // fun createRoute(profileId: Long) = "profile_edit/$profileId"
    }

    object DiagnosticObjects : Screen("diagnostic_objects")
    object Settings : Screen("settings")

    object Diagnostics : Screen("diagnostics/{areaId}") {
        fun createRoute(areaId: String) =
            "diagnostics/$areaId"

        object Menu : Screen("diagnostics_menu")
        object Points : Screen("diagnostics_points")
        object History : Screen("diagnostics_history")
        object EditReport : Screen("edit_report/{pointId}/{reportId}") {
            fun createRoute(pointId: String, reportId: Long) = "edit_report/$pointId/$reportId"
        }

        object CreateReport : Screen("create_report/{pointId}") {
            fun createRoute(pointId: String) = "create_report/$pointId"
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController,
        startDestination = when {
            LocalDate.now() < LocalDate.of(2026, 1, 1) -> Screen.Initial.route
            else -> Screen.OutOfService.route
        },
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(600)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(600),
                targetOffset = { it }
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(600),
                initialOffset = { it }
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(700)
            )
        }
    ) {
        composable(Screen.Initial.route) {
            InitialScreen(
                onAuthorize = {
                    navController.popBackStack()
                    navController.navigate(Screen.Authorization.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.OutOfService.route) {
            OutOfServiceScreen()
        }

        composable(Screen.Authorization.route) {
            AuthorizationScreen(
                onEditProfile = {
                    navController.navigate(Screen.ProfileEdit.route)
                },
                onAuthorized = {
                    navController.navigate(Screen.DiagnosticObjects.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.ProfileEdit.route) {
            EditProfileScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.DiagnosticObjects.route) {
            DiagnosticObjectsScreen(
                onAuthorize = {
                    navController.navigate(Screen.Authorization.route) {
                        launchSingleTop = true
                    }
                },
                onAreaChosen = { area ->
                    navController.navigate(Screen.Diagnostics.createRoute(area.id)) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        navigation(
            route = Screen.Diagnostics.route,
            startDestination = Screen.Diagnostics.Menu.route,
            arguments = listOf(
                navArgument("areaId") { type = NavType.StringType }
            )
        ) {
            val diagnosticViewModel = @Composable { ->
                hiltViewModel<DiagnosticViewModel>(remember(navController.currentBackStackEntry) {
                    navController.getBackStackEntry(
                        Screen.Diagnostics.route
                    )
                })
            }

            composable(Screen.Diagnostics.Menu.route) {
                DiagnosticMenuScreen(
                    viewModel = diagnosticViewModel(),
                    onAuthorize = {
                        navController.navigate(Screen.Authorization.route) {
                            popUpTo(Screen.Authorization.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onChooseArea = {
                        navController.navigate(Screen.DiagnosticObjects.route) {
                            popUpTo(Screen.DiagnosticObjects.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onSettings = {
                        navController.navigate(Screen.Settings.route) {
                            launchSingleTop = true
                        }
                    },
                    onHistory = {
                        navController.navigate(Screen.Diagnostics.History.route) {
                            launchSingleTop = true
                        }
                    },
                    onPointList = {
                        navController.navigate(Screen.Diagnostics.Points.route) {
                            launchSingleTop = true
                        }
                    },
                    onCreateReport = { pointId ->
                        navController.navigate(
                            Screen.Diagnostics.CreateReport.createRoute(
                                pointId = pointId
                            )
                        )
                    },
                    onEditReport = { pointId, reportId ->
                        navController.navigate(
                            Screen.Diagnostics.EditReport.createRoute(
                                pointId = pointId,
                                reportId = reportId
                            )
                        )
                    }
                )
            }

            composable(Screen.Diagnostics.History.route) {
                ReportHistoryScreen(
                    viewModel = diagnosticViewModel(),
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Diagnostics.Points.route) {
                DiagnosticPointsScreen(
                    viewModel = diagnosticViewModel(),
                    onBack = { navController.popBackStack() },
                    onPointChosen = { pointId ->
                        navController.navigate(Screen.Diagnostics.CreateReport.createRoute(pointId)) {
                            popUpTo(Screen.Diagnostics.Menu.route) { inclusive = false }
                        }
                    }
                )
            }
        }
    }
}