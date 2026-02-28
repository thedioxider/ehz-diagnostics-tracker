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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.Section
import space.diomentia.ehzdiagnosticstracker.data.Route
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.ReportDatabase
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.DeleteDialog
import space.diomentia.ehzdiagnosticstracker.ui.DialogField
import space.diomentia.ehzdiagnosticstracker.ui.EditFieldsDialog
import space.diomentia.ehzdiagnosticstracker.ui.FullscreenLazyColumn
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.rememberSnacker
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme

@Composable
fun DiagnosticObjectsScreen(
    onAuthorize: () -> Unit,
    onAreaChosen: (Route, Section) -> Unit
) {
    var createObjectRequest by remember { mutableStateOf(false) }
    var selectedRoute: Route? by remember { mutableStateOf(null) }

    PtmTheme {
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
                    },
                    actions = if (AppState.adminSession)
                        { ->
                            ActionButton(
                                rememberVectorPainter(Icons.Outlined.Add),
                                label = stringResource(R.string.create_object)
                            ) { createObjectRequest = true }
                        }
                    else null
                )
            }
        ) { innerPadding ->
            val diagnosticObjects by ReportDatabase.Companion.getInstance()
                .routeDao()
                .getAllRoutes()
                .collectAsStateWithLifecycle(emptyList())

            if (createObjectRequest) {
                DiagnosticObjectDialog(null) { createObjectRequest = false }
            }

            if (diagnosticObjects.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Companion.Center,
                    modifier = Modifier.Companion.fillMaxSize()
                ) {
                    Text(
                        stringResource(R.string.entries_not_found),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.Companion.alpha(.3f)
                    )
                }
                return@Scaffold
            }

            var addAreaRequest by remember { mutableStateOf<Route?>(null) }
            addAreaRequest?.let { diagnosticObject ->
                DiagnosticAreaDialog(diagnosticObject.id, null) { addAreaRequest = null }
            }

            var editObjectRequest by remember { mutableStateOf<Route?>(null) }
            var deleteObjectRequest: Route? by remember { mutableStateOf(null) }
            editObjectRequest?.let {
                DiagnosticObjectDialog(
                    editObjectRequest,
                    onDelete = { deleteObjectRequest = editObjectRequest }
                ) { editObjectRequest = null }
            }

            var editAreaRequest by remember { mutableStateOf<Section?>(null) }
            var deleteAreaRequest: Section? by remember { mutableStateOf(null) }
            editAreaRequest?.let { diagnosticArea ->
                DiagnosticAreaDialog(
                    diagnosticArea.routeId,
                    diagnosticArea,
                    onDelete = { deleteAreaRequest = diagnosticArea },
                    onDismissRequest = { editAreaRequest = null }
                )
            }

            deleteObjectRequest?.let { diagnosticObject ->
                DeleteObjectDialog(diagnosticObject) { deleteObjectRequest = null }
            }

            deleteAreaRequest?.let { diagnosticArea ->
                DeleteAreaDialog(diagnosticArea) { deleteAreaRequest = null }
            }

            val selectedObjectAreas by selectedRoute?.let {
                ReportDatabase.getInstance()
                    .routeDao()
                    .getRouteSections(it.id)
                    .collectAsStateWithLifecycle(emptyList())
            } ?: remember { mutableStateOf(emptyList()) }
            FullscreenLazyColumn(innerPadding) {
                items(diagnosticObjects) { diagnosticObject ->
                    AnimatedVisibility(
                        selectedRoute == null || selectedRoute?.id == diagnosticObject.route.id
                    ) {
                        DiagnosticObjectItem(
                            diagnosticObject.route.name,
                            onBack = if (selectedRoute?.id == diagnosticObject.route.id) { ->
                                selectedRoute = null
                            } else null,
                            onClick = if (selectedRoute == null) { ->
                                selectedRoute = diagnosticObject.route
                            } else null,
                            onLongClick = {
                                if (AppState.adminSession) {
                                    editObjectRequest = diagnosticObject.route
                                }
                            },
                            onNew = if (selectedRoute?.id == diagnosticObject.route.id
                                && AppState.adminSession
                            ) { ->
                                addAreaRequest = diagnosticObject.route
                            } else null
                        )
                    }
                }
                items(selectedObjectAreas) { section ->
                    AnimatedVisibility(selectedRoute != null) {
                        DiagnosticAreaItem(
                            section.name,
                            modifier = Modifier.Companion.padding(start = 64.dp),
                            onLongClick = {
                                if (AppState.adminSession) {
                                    editAreaRequest = section
                                }
                            }
                        ) {
                            selectedRoute?.let { route ->
                                onAreaChosen(route, section)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiagnosticAreaItem(
    name: String,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.Companion.CenterVertically,
        modifier = Modifier.Companion
            .combinedClickable(
                enabled = onClick != null,
                onClick = onClick ?: {},
                onLongClick = onLongClick ?: {}
            )
            .padding(16.dp)
            .padding()
            .defaultMinSize(minHeight = 24.dp)
            .height(IntrinsicSize.Min)
            .then(modifier)
    ) {
        Column(
            horizontalAlignment = Alignment.Companion.Start,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.Companion
                .fillMaxHeight()
                .weight(1f)
        ) {
            Text(
                "${stringResource(R.string.area)} $name",
                style = MaterialTheme.typography.titleMedium
            )
        }
        AnimatedVisibility(onClick != null && AppState.adminSession) {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.edit_area),
                modifier = Modifier.Companion
                    .clickable(onClick = onLongClick ?: {})
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .padding(end = 12.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiagnosticObjectItem(
    name: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onNew: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .combinedClickable(
                enabled = onClick != null,
                onClick = onClick ?: {},
                onLongClick = onLongClick ?: {}
            )
            .padding(24.dp)
            .padding()
            .defaultMinSize(minHeight = 36.dp)
            .height(IntrinsicSize.Min)
            .then(modifier)
    ) {
        AnimatedVisibility(onBack != null) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = null,
                modifier = Modifier
                    .clickable(onClick = onBack ?: {})
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
                name,
                style = MaterialTheme.typography.titleLarge
            )
        }
        AnimatedVisibility(onClick != null && AppState.adminSession) {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.edit_object),
                modifier = Modifier
                    .clickable(onClick = onLongClick ?: {})
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .padding(end = 12.dp)
            )
        }
        AnimatedVisibility(onNew != null) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = stringResource(R.string.create),
                modifier = Modifier
                    .clickable(onClick = onNew ?: {})
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .padding(end = 12.dp)
            )
        }
    }
}

@Composable
private fun DiagnosticObjectDialog(
    route: Route?,
    onDelete: () -> Unit = {},
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current

    EditFieldsDialog(
        fields = listOf(
            R.string.registration_number to DialogField(
                stringResource(R.string.registration_number),
                checker = { Route.checkId(it) },
                supportingText = stringResource(R.string.only_letters_or_numbers),
                mutable = route == null,
                initValue = route?.id,
                keyboardType = KeyboardType.Ascii
            ),
            R.string.name to DialogField(
                stringResource(R.string.name),
                checker = { Route.checkName(it) },
                supportingText = stringResource(R.string.only_letters_or_numbers),
                initValue = route?.name
            )
        ),
        onAction = { v ->
            if (route == null) {
                ReportDatabase.getInstance(context).routeDao()
                    .insertRoute(
                        Route(
                            id = v[R.string.registration_number]!!,
                            name = v[R.string.name]!!
                        )
                    )
            } else {
                ReportDatabase.getInstance(context).routeDao()
                    .updateRoute(
                        route.copy(name = v[R.string.name]!!)
                    )
            }
            onDismissRequest()
        },
        actionLabel = {
            Text(
                stringResource(
                    if (route == null)
                        R.string.create
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
                    if (route == null)
                        R.string.create_object
                    else R.string.edit_object
                )
            )
        },
        onCancel = onDismissRequest
    )
}

@Composable
private fun DiagnosticAreaDialog(
    diagnosticObjectId: String,
    section: Section?,
    onDelete: () -> Unit = {},
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current

    EditFieldsDialog(
        fields = listOf(
            R.string.registration_number to DialogField(
                stringResource(R.string.registration_number),
                checker = { Section.checkId(it) },
                supportingText = stringResource(R.string.only_letters_or_numbers),
                mutable = section == null,
                initValue = section?.id,
                keyboardType = KeyboardType.Ascii
            ),
            R.string.name to DialogField(
                stringResource(R.string.name),
                checker = { Section.checkName(it) },
                supportingText = stringResource(R.string.only_letters_or_numbers),
                initValue = section?.name
            )
        ),
        onAction = { v ->
            if (section == null) {
                ReportDatabase.getInstance(context).routeDao()
                    .insertSection(
                        Section(
                            id = v[R.string.registration_number]!!,
                            name = v[R.string.name]!!,
                            routeId = diagnosticObjectId
                        )
                    )
            } else {
                ReportDatabase.getInstance(context).routeDao()
                    .updateSection(
                        section.copy(name = v[R.string.name]!!)
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
private fun DeleteObjectDialog(
    route: Route,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val snacker = rememberSnacker()

    DeleteDialog(
        onDelete = {
            ReportDatabase.getInstance(context)
                .routeDao()
                .deleteRoute(route)
            snacker.showMessage(context.getString(R.string.profile_deleted))
            onDismissRequest()
        },
        label = { Text("${stringResource(R.string.delete_object)}?") },
        onCancel = onDismissRequest,
        name = route.name,
        details = route.id
    )
}

@Composable
private fun DeleteAreaDialog(
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
            snacker.showMessage(context.getString(R.string.profile_deleted))
            onDismissRequest()
        },
        label = { Text("${stringResource(R.string.delete_area)}?") },
        onCancel = onDismissRequest,
        name = section.name,
        details = section.id
    )
}


@Preview
@Composable
private fun DiagnosticObjectChooseScreenPreview() = DiagnosticObjectsScreen({}, { _, _ -> })

@Preview
@Composable
private fun AddObjectDialogPreview() = PtmTheme {
    DiagnosticObjectChooseScreenPreview()
    DiagnosticObjectDialog(null) { }
}

@Preview
@Composable
private fun EditObjectDialogPreview() = PtmTheme {
    DiagnosticObjectChooseScreenPreview()
    DiagnosticObjectDialog(Route("testes", "Test"), {}, {})
}

@Preview
@Composable
private fun AddAreaDialogPreview() = PtmTheme {
    DiagnosticObjectChooseScreenPreview()
    DiagnosticAreaDialog("", null) {}
}

@Preview
@Composable
private fun DeleteObjectDialogPreview() = PtmTheme {
    DiagnosticObjectChooseScreenPreview()
    DeleteObjectDialog(Route("tsetsetset", "Test"), {})
}