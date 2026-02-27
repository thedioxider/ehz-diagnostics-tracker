package space.diomentia.ehzdiagnosticstracker.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.data.DiagnosticArea
import space.diomentia.ehzdiagnosticstracker.data.DiagnosticObject
import space.diomentia.ehzdiagnosticstracker.data.DiagnosticPoint
import space.diomentia.ehzdiagnosticstracker.data.DiagnosticSession
import space.diomentia.ehzdiagnosticstracker.data.LocalDiagnosticSession
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
fun DiagnosticPointsScreen(
    diagnosticSession: DiagnosticSession,
    onBack: () -> Unit,
    onPointChosen: (DiagnosticPoint) -> Unit
) {
    var createPointRequest by remember { mutableStateOf(false) }

    CompositionLocalProvider(
        LocalDiagnosticSession provides remember { mutableStateOf(diagnosticSession) }
    ) {
        PtmTheme {
            Scaffold(
                snackbarHost = { PtmSnackbarHost(LocalSnackbarHostState.current) },
                topBar = {
                    PtmTopBar(title = {
                        Text(stringResource(R.string.diagnostic_points))
                    })
                },
                bottomBar = {
                    PtmBottomBar(
                        navigation = {
                            ActionButton(
                                rememberVectorPainter(Icons.AutoMirrored.Outlined.ArrowBack),
                                label = stringResource(R.string.back)
                            ) {
                                onBack()
                            }
                        },
                        actions = {
                            ActionButton(
                                rememberVectorPainter(Icons.Outlined.Add),
                                label = stringResource(R.string.create_point)
                            ) { createPointRequest = true }
                        }
                    )
                }
            ) { innerPadding ->
                val diagnosticSession by LocalDiagnosticSession.current
                val diagnosticPoints by ReportDatabase.getInstance()
                    .diagnosticAreaDao()
                    .getAreaPoints(diagnosticSession.diagnosticArea.id)
                    .collectAsStateWithLifecycle(emptyList())

                if (createPointRequest) {
                    DiagnosticPointDialog(null) { createPointRequest = false }
                }

                if (diagnosticPoints.isEmpty()) {
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

                var editPointRequest by remember { mutableStateOf<DiagnosticPoint?>(null) }
                var deletePointRequest: DiagnosticPoint? by remember { mutableStateOf(null) }
                editPointRequest?.let {
                    DiagnosticPointDialog(
                        editPointRequest,
                        onDelete = { deletePointRequest = editPointRequest }
                    ) { editPointRequest = null }
                }

                deletePointRequest?.let { diagnosticPoint ->
                    DeletePointDialog(diagnosticPoint) { deletePointRequest = null }
                }

                FullscreenLazyColumn(innerPadding) {
                    items(diagnosticPoints) { diagnosticPoint ->
                        DiagnosticPointItem(
                            diagnosticPoint.point,
                            onClick = { onPointChosen(diagnosticPoint.point) },
                            onEdit = { editPointRequest = diagnosticPoint.point }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiagnosticPointItem(
    point: DiagnosticPoint,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(24.dp)
            .padding()
            .defaultMinSize(minHeight = 36.dp)
            .height(IntrinsicSize.Min)
            .then(modifier)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            Text(
                point.inventoryId,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                point.id,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.alpha(.75f)
            )
        }
        Icon(
            Icons.Outlined.Edit,
            contentDescription = null,
            modifier = Modifier
                .clickable(onClick = onEdit)
                .fillMaxHeight()
                .aspectRatio(1f)
                .padding(end = 12.dp)
        )
    }
}

@Composable
private fun DiagnosticPointDialog(
    diagnosticPoint: DiagnosticPoint?,
    onDelete: () -> Unit = {},
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val diagnosticSession by LocalDiagnosticSession.current

    EditFieldsDialog(
        fields = listOf(
            R.string.id to DialogField(
                stringResource(R.string.id),
                checker = { DiagnosticPoint.checkId(it) },
                supportingText = stringResource(R.string.only_letters_or_numbers),
                mutable = diagnosticPoint == null,
                initValue = diagnosticPoint?.id
            ),
            R.string.inventory_number to DialogField(
                stringResource(R.string.inventory_number),
                checker = { DiagnosticPoint.checkName(it) },
                supportingText = stringResource(R.string.only_letters_or_numbers),
                initValue = diagnosticPoint?.inventoryId
            )
        ),
        onAction = { v ->
            if (diagnosticPoint == null) {
                ReportDatabase.getInstance(context).diagnosticAreaDao()
                    .insertDiagnosticPoint(
                        DiagnosticPoint(
                            id = v[R.string.id]!!,
                            inventoryId = v[R.string.inventory_number]!!,
                            registrationNumber = v[R.string.id]!!,
                            diagnosticArea = diagnosticSession.diagnosticArea.id
                        )
                    )
            } else {
                ReportDatabase.getInstance(context).diagnosticAreaDao()
                    .updateDiagnosticPoint(
                        diagnosticPoint.copy(inventoryId = v[R.string.inventory_number]!!)
                    )
            }
            onDismissRequest()
        },
        actionLabel = { Text(stringResource(if (diagnosticPoint == null) R.string.create else R.string.save)) },
        onSecondaryAction = {
            onDelete()
            onDismissRequest()
        },
        secondaryActionLabel = if (diagnosticPoint != null) { -> Text(stringResource(R.string.delete)) } else null,
        label = { Text(stringResource(if (diagnosticPoint == null) R.string.create_point else R.string.edit_point)) },
        onCancel = onDismissRequest
    )
}

@Composable
private fun DeletePointDialog(
    diagnosticPoint: DiagnosticPoint,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val snacker = rememberSnacker()

    DeleteDialog(
        onDelete = {
            ReportDatabase.getInstance(context)
                .diagnosticAreaDao()
                .deleteDiagnosticPoint(diagnosticPoint)
            snacker.showMessage(context.getString(R.string.profile_deleted))
            onDismissRequest()
        },
        label = { Text("${stringResource(R.string.delete_point)}?") },
        onCancel = onDismissRequest,
        name = diagnosticPoint.inventoryId,
        details = diagnosticPoint.id
    )
}


@Preview
@Composable
private fun DiagnosticPointsScreenPreview() = DiagnosticPointsScreen(
    DiagnosticSession(
        diagnosticObject = DiagnosticObject("", "", "Test Object"),
        diagnosticArea = DiagnosticArea("", "Test Area", "", "")
    ), {}, {})