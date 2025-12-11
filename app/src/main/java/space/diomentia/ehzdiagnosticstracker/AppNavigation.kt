package space.diomentia.ehzdiagnosticstracker

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import space.diomentia.ehzdiagnosticstracker.data.DiagnosticSession
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.ReportSession
import space.diomentia.ehzdiagnosticstracker.ui.screens.AuthorizationScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.DiagnosticMenuScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.DiagnosticObjectsScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.DiagnosticPointsScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.EditProfileScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.InitialScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.OutOfServiceScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.ReportEditorScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.ReportHistoryScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.SettingsScreen
import java.time.LocalDate

sealed class Screen(val route: String) {
    object Initial : Screen("initial")
    object OutOfService : Screen("out_of_service")
    object Authorization : Screen("authorization")

    // TODO: don't depend on app state
    object ProfileEdit : Screen("profile_edit")
    object DiagnosticObjects : Screen("diagnostic_objects")
    object Settings : Screen("settings")
    object Diagnostics : Screen("diagnostics") {
        object Menu : Screen("diagnostics_menu")
        object Points : Screen("diagnostics_points")
        object History : Screen("diagnostics_history")
        object ReportEditor : Screen("report_editor")
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    val navController = rememberNavController()
    var diagnosticSession: DiagnosticSession? by remember { mutableStateOf(null) }
    var reportSession: ReportSession? by remember { mutableStateOf(null) }

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
                targetOffset = { it / 2 }
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(600),
                initialOffset = { it / 2 }
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
                    navController.navigate(
                        if (diagnosticSession == null)
                            Screen.DiagnosticObjects.route
                        else
                            Screen.Diagnostics.Menu.route
                    ) {
                        launchSingleTop = true
                    }
                    snackbarHostState.currentSnackbarData?.dismiss()
                }
            )
        }

        composable(Screen.ProfileEdit.route) {
            EditProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DiagnosticObjects.route) {
            DiagnosticObjectsScreen(
                onAuthorize = {
                    navController.navigate(Screen.Authorization.route) {
                        launchSingleTop = true
                    }
                },
                onAreaChosen = { diagnosticObject, diagnosticArea ->
                    diagnosticSession = DiagnosticSession(diagnosticObject, diagnosticArea)
                    navController.navigate(Screen.Diagnostics.route) {
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
            startDestination = Screen.Diagnostics.Menu.route
        ) {
            composable(Screen.Diagnostics.Menu.route) {
                val mDiagnosticSession = remember { diagnosticSession }
                if (mDiagnosticSession == null) {
                    navController.navigate(Screen.Initial.route) {
                        popUpTo(0) { inclusive = true }
                    }
                    return@composable
                }
                DiagnosticMenuScreen(
                    diagnosticSession = mDiagnosticSession,
                    onAuthorize = {
                        navController.popBackStack()
                        navController.navigate(Screen.Authorization.route) {
                            launchSingleTop = true
                        }
                    },
                    onChooseArea = {
                        navController.popBackStack()
                        navController.navigate(Screen.DiagnosticObjects.route) {
                            launchSingleTop = true
                        }
                        diagnosticSession = null
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
                    onEditReport = { mReportSession ->
                        reportSession = mReportSession
                        navController.navigate(Screen.Diagnostics.ReportEditor.route)
                    },
                    reportSession = remember { reportSession }
                )
            }

            composable(Screen.Diagnostics.History.route) {
                val mDiagnosticSession = remember { diagnosticSession }
                if (mDiagnosticSession == null) {
                    navController.navigate(Screen.Initial.route) {
                        popUpTo(0) { inclusive = true }
                    }
                    return@composable
                }
                ReportHistoryScreen(
                    diagnosticSession = mDiagnosticSession,
                    onEditReport = { report ->
                        reportSession = ReportSession.fromExisting(
                            context = context,
                            diagnosticSession = mDiagnosticSession,
                            reportId = report.id
                        )
                        navController.navigate(Screen.Diagnostics.ReportEditor.route) {
                            popUpTo(Screen.Diagnostics.Menu.route) { inclusive = false }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Diagnostics.Points.route) {
                val mDiagnosticSession = remember { diagnosticSession }
                if (mDiagnosticSession == null) {
                    navController.navigate(Screen.Initial.route) {
                        popUpTo(0) { inclusive = true }
                    }
                    return@composable
                }
                DiagnosticPointsScreen(
                    diagnosticSession = mDiagnosticSession,
                    onBack = { navController.popBackStack() },
                    onPointChosen = { diagnosticPoint ->
                        reportSession = ReportSession(
                            diagnosticSession = mDiagnosticSession,
                            diagnosticPoint = diagnosticPoint
                        )
                        navController.navigate(Screen.Diagnostics.ReportEditor.route) {
                            popUpTo(Screen.Diagnostics.Menu.route) { inclusive = false }
                        }
                    }
                )
            }

            composable(Screen.Diagnostics.ReportEditor.route) {
                val mReportSession = remember { reportSession }
                if (mReportSession == null) {
                    navController.navigate(Screen.Initial.route) {
                        popUpTo(0) { inclusive = true }
                    }
                    return@composable
                }
                ReportEditorScreen(
                    mReportSession,
                    onSaved = { reportSession = null },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}