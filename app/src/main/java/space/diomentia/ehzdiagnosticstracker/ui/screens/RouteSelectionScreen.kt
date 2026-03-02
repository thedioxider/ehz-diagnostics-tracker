package space.diomentia.ehzdiagnosticstracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.ReportDatabase
import space.diomentia.ehzdiagnosticstracker.data.Route
import space.diomentia.ehzdiagnosticstracker.data.Section
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.ActionCancelDialog
import space.diomentia.ehzdiagnosticstracker.ui.DeleteDialog
import space.diomentia.ehzdiagnosticstracker.ui.DialogField
import space.diomentia.ehzdiagnosticstracker.ui.EditFieldsDialog
import space.diomentia.ehzdiagnosticstracker.ui.FullscreenLazyColumn
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.rememberSnacker
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme
import java.util.UUID

@Composable
fun RouteSelectionScreen(
    onAuthorize: () -> Unit,
    onSectionChosen: (Route, Section) -> Unit,
    onHistory: (routeId: String?, sectionId: String?) -> Unit
) {
    var selectedRoute: Route? by remember { mutableStateOf(null) }

    // Admin CRUD state
    var createRouteRequest by remember { mutableStateOf(false) }
    var editRouteRequest by remember { mutableStateOf<Route?>(null) }
    var deleteRouteRequest by remember { mutableStateOf<Route?>(null) }
    var addSectionRequest by remember { mutableStateOf<Route?>(null) }
    var editSectionRequest by remember { mutableStateOf<Section?>(null) }
    var deleteSectionRequest by remember { mutableStateOf<Section?>(null) }

    // Section action dialog state
    var sectionActionTarget by remember { mutableStateOf<Section?>(null) }

    // Dialogs
    if (createRouteRequest) {
        RouteDialog(null) { createRouteRequest = false }
    }
    editRouteRequest?.let {
        RouteDialog(
            it,
            onDelete = { deleteRouteRequest = it }
        ) { editRouteRequest = null }
    }
    deleteRouteRequest?.let { route ->
        DeleteRouteDialog(
            route,
            onSuccess = { selectedRoute = null }
        ) { deleteRouteRequest = null }
    }
    addSectionRequest?.let { route ->
        SectionDialog(route.id, null) { addSectionRequest = null }
    }
    editSectionRequest?.let { section ->
        SectionDialog(
            section.routeId,
            section,
            onDelete = { deleteSectionRequest = section },
            onDismissRequest = { editSectionRequest = null }
        )
    }
    deleteSectionRequest?.let { section ->
        DeleteSectionDialog(section) { deleteSectionRequest = null }
    }

    sectionActionTarget?.let { section ->
        ActionCancelDialog(
            onAction = { selectedRoute?.let { route -> onSectionChosen(route, section) } },
            actionLabel = { Text(stringResource(R.string.start_diagnostics)) },
            onCancel = { sectionActionTarget = null },
            onSecondaryAction = {
                sectionActionTarget = null
                onHistory(null, section.id)
            },
            secondaryActionLabel = { Text(stringResource(R.string.report_list)) },
            label = { Text("${stringResource(R.string.area)} ${section.displayName()}") }
        ) { }
    }

    Scaffold(
        snackbarHost = { PtmSnackbarHost(LocalSnackbarHostState.current) },
        topBar = {
            PtmTopBar(title = {
                Text(stringResource(R.string.diagnostic_object))
            })
        },
        bottomBar = {
            PtmBottomBar(
                navigation = {
                    ActionButton(
                        painterResource(R.drawable.ic_person),
                        label = stringResource(R.string.authorization)
                    ) {
                        onAuthorize()
                    }
                }
            )
        }
    ) { innerPadding ->
        val diagnosticObjects by ReportDatabase.getInstance()
            .routeDao()
            .getAllRoutes()
            .collectAsStateWithLifecycle(emptyList())

        if (diagnosticObjects.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    stringResource(R.string.entries_not_found),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.alpha(.3f)
                )
            }
            return@Scaffold
        }

        val selectedRouteSections by selectedRoute?.let {
            ReportDatabase.getInstance()
                .routeDao()
                .getRouteSections(it.id)
                .collectAsStateWithLifecycle(emptyList())
        } ?: remember { mutableStateOf(emptyList()) }

        // TODO: maybe there is a better solution than just AnimatedVisibility slop
        FullscreenLazyColumn(innerPadding) {
            items(diagnosticObjects) { routeWithSections ->
                AnimatedVisibility(
                    selectedRoute == null || selectedRoute?.id == routeWithSections.route.id
                ) {
                    RouteItem(
                        routeWithSections.route,
                        onBack = if (selectedRoute?.id == routeWithSections.route.id) { ->
                            selectedRoute = null
                        } else null,
                        onClick = if (selectedRoute == null) { ->
                            selectedRoute = routeWithSections.route
                        } else null,
                        onLongClick = if (AppState.adminSession) { ->
                            editRouteRequest = routeWithSections.route
                        } else null,
                        onHistory = if (selectedRoute?.id == routeWithSections.route.id) { ->
                            onHistory(routeWithSections.route.id, null)
                        } else null
                    )
                }
            }
            items(selectedRouteSections) { section ->
                AnimatedVisibility(selectedRoute != null) {
                    SectionItem(
                        section,
                        modifier = Modifier.padding(start = 32.dp),
                        onClick = { sectionActionTarget = section },
                        onLongClick = if (AppState.adminSession) { ->
                            editSectionRequest = section
                        } else null
                    )
                }
            }
            if (selectedRoute != null && AppState.adminSession) {
                item {
                    AddItemButton(
                        label = stringResource(R.string.add_section),
                        modifier = Modifier.padding(start = 32.dp),
                        onClick = { addSectionRequest = selectedRoute }
                    )
                }
            }
            if (selectedRoute == null && AppState.adminSession) {
                item {
                    AddItemButton(
                        label = stringResource(R.string.add_object),
                        onClick = { createRouteRequest = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddItemButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 24.dp)
            .then(modifier)
    ) {
        Icon(
            Icons.Outlined.Add,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 12.dp)
                .alpha(.5f)
        )
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.alpha(.5f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SectionItem(
    section: Section,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .defaultMinSize(minHeight = 24.dp)
            .height(IntrinsicSize.Min)
            .then(modifier)
    ) {
        // TODO: indicate when there's an incomplete report present
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            Text(
                "${stringResource(R.string.area)} ${section.displayName()}",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RouteItem(
    route: Route,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onHistory: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = onBack != null || onClick != null,
                onClick = onBack ?: onClick ?: {},
                onLongClick = onLongClick
            )
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .defaultMinSize(minHeight = 36.dp)
            .height(IntrinsicSize.Min)
            .then(modifier)
    ) {
        AnimatedVisibility(onBack != null) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .padding(end = 12.dp)
            )
        }
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            Text(
                route.name,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "${route.length} ${stringResource(R.string.km)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.alpha(.5f)
            )
        }
        AnimatedVisibility(onHistory != null) {
            // TODO: better clickable area and indication
            Icon(
                painterResource(R.drawable.ic_history),
                contentDescription = stringResource(R.string.report_list),
                modifier = Modifier
                    .clickable(onClick = onHistory ?: {})
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .padding(end = 12.dp)
            )
        }
    }
}

@Composable
private fun RouteDialog(
    route: Route?,
    onDelete: () -> Unit = {},
    onDismissRequest: () -> Unit
) {
    LocalContext.current
    val routeDao = ReportDatabase.getInstance().routeDao()

    EditFieldsDialog(
        fields = listOf(
            R.string.registration_number to DialogField(
                stringResource(R.string.registration_number),
                mutable = false,
                initValue = route?.id ?: UUID.randomUUID().toString()
            ),
            R.string.name to DialogField(
                stringResource(R.string.name),
                checker = { Route.checkName(it) },
                supportingText = stringResource(R.string.only_letters_or_numbers),
                initValue = route?.name
            ),
            R.string.route_length_km to DialogField(
                stringResource(R.string.route_length_km),
                checker = { it.isDigitsOnly() },
                allowUnchecked = false,
                nullable = false,
                initValue = route?.length?.toString()
            )
        ),
        onAction = { v ->
            if (route == null) {
                routeDao.insertRoute(
                    Route(
                        id = v[R.string.registration_number]!!,
                        name = v[R.string.name]!!,
                        length = v[R.string.route_length_km]!!.toInt()
                    )
                )
            } else {
                routeDao.updateRoute(
                    route.copy(
                        name = v[R.string.name]!!,
                        length = v[R.string.route_length_km]!!.toInt()
                    )
                )
            }
            onDismissRequest()
        },
        actionLabel = {
            Text(
                stringResource(
                    if (route == null) R.string.create
                    else R.string.save
                )
            )
        },
        onSecondaryAction = {
            onDelete()
            onDismissRequest()
        },
        secondaryActionLabel = if (route != null) { -> Text(stringResource(R.string.delete)) } else null,
        label = {
            Text(
                stringResource(
                    if (route == null) R.string.create_object
                    else R.string.edit_object
                )
            )
        },
        onCancel = onDismissRequest
    )
}

@Composable
private fun SectionDialog(
    routeId: String,
    section: Section?,
    onDelete: () -> Unit = {},
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current

    EditFieldsDialog(
        fields = listOf(
            R.string.registration_number to DialogField(
                stringResource(R.string.registration_number),
                mutable = false,
                initValue = section?.id ?: UUID.randomUUID().toString()
            ),
            // TODO: edit fields accordingly
            R.string.section_begin_km to DialogField(
                stringResource(R.string.section_begin_km),
                checker = { it.isDigitsOnly() },
                keyboardType = KeyboardType.Decimal,
                initValue = section?.beginKm?.toString(),
                allowUnchecked = false
            ),
            R.string.section_end_km to DialogField(
                stringResource(R.string.section_end_km),
                checker = { it.isDigitsOnly() },
                keyboardType = KeyboardType.Decimal,
                initValue = section?.endKm?.toString(),
                allowUnchecked = false
            )
        ),
        onAction = { v ->
            if (section == null) {
                ReportDatabase.getInstance(context).routeDao()
                    .insertSection(
                        Section(
                            id = v[R.string.registration_number]!!,
                            beginKm = v[R.string.section_begin_km]!!.toInt(),
                            endKm = v[R.string.section_end_km]!!.toInt(),
                            routeId = routeId
                        )
                    )
            } else {
                ReportDatabase.getInstance(context).routeDao()
                    .updateSection(
                        section.copy(
                            beginKm = v[R.string.section_begin_km]!!.toInt(),
                            endKm = v[R.string.section_end_km]!!.toInt()
                        )
                    )
            }
            onDismissRequest()
        },
        actionLabel = { Text(stringResource(if (section == null) R.string.create else R.string.save)) },
        onSecondaryAction = {
            onDelete()
            onDismissRequest()
        },
        secondaryActionLabel = if (section != null) { -> Text(stringResource(R.string.delete)) } else null,
        label = { Text(stringResource(if (section == null) R.string.create_area else R.string.edit_area)) },
        onCancel = onDismissRequest
    )
}

@Composable
private fun DeleteRouteDialog(
    route: Route,
    onSuccess: () -> Unit = {},
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val snacker = rememberSnacker()

    DeleteDialog(
        onDelete = {
            ReportDatabase.getInstance(context)
                .routeDao()
                .deleteRoute(route)
            snacker.showMessage(context.getString(R.string.route_deleted))
            onSuccess()
            onDismissRequest()
        },
        label = { Text("${stringResource(R.string.delete_object)}?") },
        onCancel = onDismissRequest,
        name = route.name,
        details = route.id
    )
}

@Composable
private fun DeleteSectionDialog(
    section: Section,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val snacker = rememberSnacker()

    DeleteDialog(
        onDelete = {
            ReportDatabase.getInstance(context)
                .routeDao()
                .deleteSection(section)
            snacker.showMessage(context.getString(R.string.section_deleted))
            onDismissRequest()
        },
        label = { Text("${stringResource(R.string.delete_area)}?") },
        onCancel = onDismissRequest,
        name = section.displayName(),
        details = section.id
    )
}

// --- Previews ---

@Preview
@Composable
private fun RouteSelectionScreenPreview() = PtmTheme {
    RouteSelectionScreen({}, { _, _ -> }, { _, _ -> })
}

@Preview
@Composable
private fun AddRouteDialogPreview() = PtmTheme {
    RouteDialog(null) {}
}

@Preview
@Composable
private fun EditRouteDialogPreview() = PtmTheme {
    RouteDialog(Route(UUID.randomUUID().toString(), "Test", 42), {}, {})
}

@Preview
@Composable
private fun AddSectionDialogPreview() = PtmTheme {
    SectionDialog("", null) {}
}

@Preview
@Composable
private fun DeleteRouteDialogPreview() = PtmTheme {
    DeleteRouteDialog(Route(UUID.randomUUID().toString(), "Test", 42)) {}
}