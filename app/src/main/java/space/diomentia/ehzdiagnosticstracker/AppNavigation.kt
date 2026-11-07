package space.diomentia.ehzdiagnosticstracker

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.LocalSessionManager
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.ReportDatabase
import space.diomentia.ehzdiagnosticstracker.data.SessionManager
import space.diomentia.ehzdiagnosticstracker.ui.screens.AuthorizationScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.EditProfileScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.HomeScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.InitialScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.LocationSiteEditorScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.OutOfServiceScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.ReportListScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.ReportScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.RouteSelectionScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.SessionHubScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.SettingsScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.SiteEditorScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.SyncScreen
import java.time.LocalDate

sealed class Screen(val route: String) {
    object Initial : Screen("initial")
    object OutOfService : Screen("out_of_service")
    object Authorization : Screen("authorization")
    object ProfileEdit : Screen("profile_edit")
    object Home : Screen("home")
    object RouteSelection : Screen("route_selection")
    object Settings : Screen("settings")
    object Sync : Screen("sync")
    object SessionHub : Screen("session_hub")

    object LocationSiteEditor : Screen("location_site_editor/{$ARG_SITE_ID}") {
        fun create() = "location_site_editor/_"
        fun edit(siteId: String) = "location_site_editor/$siteId"
    }

    object SiteEditor : Screen("site_editor/{$ARG_SITE_ID}?$ARG_EDIT={$ARG_EDIT}") {
        fun open(siteId: String, edit: Boolean = false) = "site_editor/$siteId?$ARG_EDIT=$edit"
    }

    object Report : Screen("report/{$ARG_REPORT_ID}") {
        fun open(reportId: String) = "report/$reportId"
    }

    object ReportList :
        Screen("report_list?$ARG_ROUTE_ID={$ARG_ROUTE_ID}&$ARG_SECTION_ID={$ARG_SECTION_ID}") {
        fun open(routeId: String? = null, sectionId: String? = null) =
            "report_list?$ARG_ROUTE_ID=${routeId ?: ""}&$ARG_SECTION_ID=${sectionId ?: ""}"
    }

    companion object {
        const val ARG_SITE_ID = "siteId"
        const val ARG_REPORT_ID = "reportId"
        const val ARG_ROUTE_ID = "routeId"
        const val ARG_SECTION_ID = "sectionId"
        const val ARG_EDIT = "edit"
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    val navController = rememberNavController()
    val sessionManager = remember {
        SessionManager(
            dao = ReportDatabase.getInstance(context).reportDao(),
            config = AppState.reportConfig.value
        )
    }

    CompositionLocalProvider(LocalSessionManager provides sessionManager) {
        NavHost(
            navController,
            startDestination = when {
                true -> Screen.Initial.route
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
                        sessionManager.profile = AppState.profileSession
                        val destination = if (sessionManager.activeReportId != null)
                            Screen.SessionHub.route
                        else
                            Screen.Home.route
                        navController.navigate(destination) {
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

            composable(Screen.Home.route) {
                if (AppState.profileSession == null) {
                    navController.navigate(Screen.Authorization.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                    return@composable
                }
                HomeScreen(
                    onNewReport = { navController.navigate(Screen.RouteSelection.route) },
                    onHistory = { navController.navigate(Screen.ReportList.open()) },
                    onExport = { navController.navigate(Screen.Sync.route) },
                    onSettings = { navController.navigate(Screen.Settings.route) },
                    onProfile = { navController.popBackStack() }
                )
            }

            composable(Screen.RouteSelection.route) {
                RouteSelectionScreen(
                    onBack = { navController.popBackStack() },
                    onSectionChosen = { routeId, sectionId ->
                        if (sessionManager.profile == null) {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            navController.navigate(Screen.Authorization.route) {
                                launchSingleTop = true
                            }
                            return@RouteSelectionScreen
                        }
                        val isNew = sessionManager.startOrResumeSession(routeId, sectionId)
                        val needsZeroKm = isNew || sessionManager.hasNoSites()
                        navController.navigate(
                            if (needsZeroKm) Screen.LocationSiteEditor.create()
                            else Screen.SessionHub.route
                        )
                    },
                    onHistory = { routeId, sectionId ->
                        navController.navigate(Screen.ReportList.open(routeId, sectionId))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Sync.route) {
                SyncScreen(
                    onBack = { navController.popBackStack() },
                    onSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.SessionHub.route) {
                SessionHubScreen(
                    onNewMeasurementSite = {
                        val site = sessionManager.addSite()
                        navController.navigate(Screen.SiteEditor.open(site.id, edit = true))
                    },
                    onNewLocationSite = {
                        navController.navigate(Screen.LocationSiteEditor.create())
                    },
                    onEditDraftSite = { siteId ->
                        navController.navigate(Screen.SiteEditor.open(siteId, edit = true))
                    },
                    onViewReport = { reportId ->
                        navController.navigate(Screen.Report.open(reportId))
                    },
                    onSessionCompleted = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                )
            }

            composable(
                Screen.LocationSiteEditor.route,
                arguments = listOf(
                    navArgument(Screen.ARG_SITE_ID) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val siteId = backStackEntry.arguments?.getString(Screen.ARG_SITE_ID)
                    ?.takeIf { it != "_" }
                val isZeroKmCreation = siteId == null &&
                        (navController.previousBackStackEntry?.destination?.route == Screen.RouteSelection.route)
                LocationSiteEditorScreen(
                    siteId = siteId,
                    onSaved = {
                        if (navController.previousBackStackEntry?.destination?.route
                            == Screen.SessionHub.route
                        ) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(Screen.SessionHub.route) {
                                popUpTo(Screen.RouteSelection.route) { inclusive = false }
                            }
                        }
                    },
                    onBack = {
                        if (isZeroKmCreation) {
                            sessionManager.cancelReport()
                        }
                        navController.popBackStack()
                    }
                )
            }

            composable(
                Screen.SiteEditor.route,
                arguments = listOf(
                    navArgument(Screen.ARG_SITE_ID) { type = NavType.StringType },
                    navArgument(Screen.ARG_EDIT) {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val siteId = backStackEntry.arguments?.getString(Screen.ARG_SITE_ID)
                    ?: return@composable
                val editMode = backStackEntry.arguments?.getBoolean(Screen.ARG_EDIT) ?: false
                SiteEditorScreen(
                    siteId = siteId,
                    initialEditMode = editMode,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                Screen.Report.route,
                arguments = listOf(
                    navArgument(Screen.ARG_REPORT_ID) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val reportId = backStackEntry.arguments?.getString(Screen.ARG_REPORT_ID)
                    ?: return@composable
                ReportScreen(
                    reportId = reportId,
                    onSiteClicked = { siteId, locationOnly ->
                        navController.navigate(
                            if (locationOnly) Screen.LocationSiteEditor.edit(siteId)
                            else Screen.SiteEditor.open(siteId)
                        )
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                Screen.ReportList.route,
                arguments = listOf(
                    navArgument(Screen.ARG_ROUTE_ID) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument(Screen.ARG_SECTION_ID) {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val routeId = backStackEntry.arguments?.getString(Screen.ARG_ROUTE_ID)
                    ?.takeIf { it.isNotEmpty() }
                val sectionId = backStackEntry.arguments?.getString(Screen.ARG_SECTION_ID)
                    ?.takeIf { it.isNotEmpty() }
                ReportListScreen(
                    routeId = routeId,
                    sectionId = sectionId,
                    onOpenReport = { reportId -> navController.navigate(Screen.Report.open(reportId)) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
