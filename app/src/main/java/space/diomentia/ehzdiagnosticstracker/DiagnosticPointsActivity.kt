package space.diomentia.ehzdiagnosticstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.DiagnosticPoint
import space.diomentia.ehzdiagnosticstracker.data.LocalCoroutineScope
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.BorderedDialogContainer
import space.diomentia.ehzdiagnosticstracker.ui.FullscreenLazyColumn
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.setupEdgeToEdge
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme

private val LocalAreaProvider = compositionLocalOf<String> { error("no area provided") }

class DiagnosticPointsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge(this)
        setContent {
            val diagnosticArea = AppState.CurrentDiagnosticSession.diagnosticArea
            if (diagnosticArea == null) {
                finish()
                return@setContent
            }
            CompositionLocalProvider(
                LocalSnackbarHostState provides remember { SnackbarHostState() },
                LocalAreaProvider provides diagnosticArea
            ) {
                Content()
            }
        }
    }
}

@Composable
private fun Content() {
    val activity = LocalActivity.current
    var createPointRequest by remember { mutableStateOf(false) }

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
                            activity?.finish()
                        }
                    },
                    actions = if (AppState.adminSession)
                        { ->
                            ActionButton(
                                rememberVectorPainter(Icons.Outlined.Add),
                                label = stringResource(R.string.create_point)
                            ) { createPointRequest = true }
                        }
                    else null
                )
            }
        ) { innerPadding ->
            val diagnosticPoints by AppState.CurrentDiagnosticSession.diagnosticArea?.let { area ->
                AppState.reportDb
                    ?.diagnosticAreaDao()
                    ?.getAreaPoints(area)
                    ?.collectAsStateWithLifecycle(emptyList())
            } ?: remember { mutableStateOf(emptyList()) }

            if (createPointRequest) {
                AddPointDialog { createPointRequest = false }
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
            editPointRequest?.let { diagnosticPoint ->
                EditPointDialog(diagnosticPoint) { editPointRequest = null }
            }

            FullscreenLazyColumn(innerPadding) {
                items(diagnosticPoints) { diagnosticPoint ->
                    DiagnosticPointItem(
                        diagnosticPoint.point,
                        onClick = { editPointRequest = diagnosticPoint.point }
                    )
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
    onClick: (() -> Unit)
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(
                enabled = AppState.adminSession,
                onClick = onClick
            )
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
        AnimatedVisibility(AppState.adminSession) {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = null,
                modifier = Modifier
                    .clickable(onClick = onClick)
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .padding(end = 12.dp)
            )
        }
    }
}

@Composable
private fun AddPointDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = LocalCoroutineScope.current
    val snackbarHostState = LocalSnackbarHostState.current
    val area = LocalAreaProvider.current
    var idInput by remember { mutableStateOf("") }
    var inventoryNumberInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest) {
        BorderedDialogContainer {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    stringResource(R.string.create_point),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    "${stringResource(R.string.id)}:",
                    style = MaterialTheme.typography.titleSmall
                )
                TextField(
                    idInput,
                    onValueChange = { idInput = it },
                    placeholder = {
                        Text(
                            stringResource(R.string.input_id),
                            fontStyle = FontStyle.Italic,
                            maxLines = 1,
                            modifier = Modifier.alpha(.5f)
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .padding(start = 16.dp)
                )
                Text(
                    "${stringResource(R.string.inventory_number)}:",
                    style = MaterialTheme.typography.titleSmall
                )
                TextField(
                    inventoryNumberInput,
                    onValueChange = { inventoryNumberInput = it },
                    placeholder = {
                        Text(
                            stringResource(R.string.input_inventory_number),
                            fontStyle = FontStyle.Italic,
                            maxLines = 1,
                            modifier = Modifier.alpha(.5f)
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .padding(start = 16.dp)
                )
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PtmFilledButton(
                        onClick = {
                            val id = idInput
                            val inventoryNumber = inventoryNumberInput
                            if (id.isEmpty() || inventoryNumber.isEmpty()) {
                                coroutineScope?.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(context.getString(R.string.wrong_profile_input))
                                }
                                return@PtmFilledButton
                            }
                            AppState.reportDb?.diagnosticAreaDao()?.run {
                                insertDiagnosticPoint(
                                    DiagnosticPoint(
                                        id = id,
                                        inventoryId = inventoryNumber,
                                        registrationNumber = id,
                                        diagnosticArea = area
                                    )
                                )
                            }
                            onDismissRequest()
                        }
                    ) {
                        Text(stringResource(R.string.save))
                    }
                    PtmFilledButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}

@Composable
private fun EditPointDialog(
    diagnosticPoint: DiagnosticPoint,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = LocalCoroutineScope.current
    val snackbarHostState = LocalSnackbarHostState.current
    var inventoryNumberInput by remember { mutableStateOf(diagnosticPoint.inventoryId) }

    Dialog(onDismissRequest) {
        BorderedDialogContainer {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    stringResource(R.string.edit_point),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    "${stringResource(R.string.id)}:",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    diagnosticPoint.id,
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .padding(start = 16.dp)
                )
                Text(
                    "${stringResource(R.string.inventory_number)}:",
                    style = MaterialTheme.typography.titleSmall
                )
                TextField(
                    inventoryNumberInput,
                    onValueChange = { inventoryNumberInput = it },
                    placeholder = {
                        Text(
                            stringResource(R.string.input_inventory_number),
                            fontStyle = FontStyle.Italic,
                            maxLines = 1,
                            modifier = Modifier.alpha(.5f)
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .padding(start = 16.dp)
                )
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PtmFilledButton(
                        onClick = {
                            val inventoryNumber = inventoryNumberInput
                            if (inventoryNumber.isEmpty()) {
                                coroutineScope?.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(context.getString(R.string.wrong_profile_input))
                                }
                                return@PtmFilledButton
                            }
                            AppState.reportDb?.diagnosticAreaDao()?.run {
                                updateDiagnosticPoint(
                                    diagnosticPoint.copy(inventoryId = inventoryNumber)
                                )
                            }
                            onDismissRequest()
                        }
                    ) {
                        Text(stringResource(R.string.save))
                    }
                    PtmFilledButton(
                        onClick = {
                            AppState.reportDb?.diagnosticAreaDao()?.run {
                                deleteDiagnosticPoint(diagnosticPoint)
                            }
                        }
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                    PtmFilledButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}


@Preview
@Composable
private fun ContentPreview() = Content()