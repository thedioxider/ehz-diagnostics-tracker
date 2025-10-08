package space.diomentia.ehzdiagnosticstracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.DiagnosticArea
import space.diomentia.ehzdiagnosticstracker.data.DiagnosticObject
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

class DiagnosticObjectChooseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge(this)
        setContent {
            CompositionLocalProvider(
                LocalSnackbarHostState provides remember { SnackbarHostState() }
            ) {
                Content()
            }
        }
    }
}

@Composable
private fun Content() {
    val activity = LocalActivity.current
    var createObjectRequest by remember { mutableStateOf(false) }

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
                            label = stringResource(R.string.back)
                        ) {
                            activity?.run {
                                startActivity(Intent(this, AuthorizationActivity::class.java))
                                finish()
                            }
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
            val diagnosticObjects by AppState.reportDb
                ?.diagnosticAreaDao()
                ?.getAllObjects()
                ?.collectAsStateWithLifecycle(emptyList())
                ?: remember { mutableStateOf(emptyList()) }

            if (createObjectRequest) {
                AddObjectDialog { createObjectRequest = false }
            }

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

            var addAreaRequest by remember { mutableStateOf<DiagnosticObject?>(null) }
            addAreaRequest?.let { diagnosticObject ->
                AddAreaDialog(diagnosticObject) { addAreaRequest = null }
            }

            val selectedObjectAreas by AppState.CurrentDiagnosticSession.diagnosticObject?.let {
                AppState.reportDb
                    ?.diagnosticAreaDao()
                    ?.getObjectAreas(it)
                    ?.collectAsStateWithLifecycle(emptyList())
            } ?: remember { mutableStateOf(emptyList()) }
            FullscreenLazyColumn(innerPadding) {
                items(diagnosticObjects) { diagnosticObject ->
                    AnimatedVisibility(
                        AppState.CurrentDiagnosticSession.diagnosticObject == null
                                || AppState.CurrentDiagnosticSession.diagnosticObject == diagnosticObject.diagnosticObject.id
                    ) {
                        DiagnosticObjectItem(
                            diagnosticObject.diagnosticObject.name,
                            onBack = if (AppState.CurrentDiagnosticSession.diagnosticObject == diagnosticObject.diagnosticObject.id) { ->
                                AppState.CurrentDiagnosticSession.diagnosticObject = null
                            } else null,
                            onClick = if (AppState.CurrentDiagnosticSession.diagnosticObject == null) { ->
                                AppState.CurrentDiagnosticSession.diagnosticObject =
                                    diagnosticObject.diagnosticObject.id
                            } else null,
                            onNew = if (AppState.CurrentDiagnosticSession.diagnosticObject == diagnosticObject.diagnosticObject.id
                                && AppState.adminSession
                            ) { ->
                                addAreaRequest = diagnosticObject.diagnosticObject
                            } else null
                        )
                    }
                }
                items(selectedObjectAreas) { area ->
                    AnimatedVisibility(AppState.CurrentDiagnosticSession.diagnosticObject != null) {
                        DiagnosticAreaItem(
                            area.diagnosticArea.name,
                            modifier = Modifier.padding(start = 64.dp),
                            onLongClick = {
                                if (AppState.adminSession) {
                                    AppState.reportDb
                                        ?.diagnosticAreaDao()
                                        ?.deleteArea(area.diagnosticArea)
                                }
                            }
                        ) {
                            AppState.CurrentDiagnosticSession.diagnosticArea =
                                area.diagnosticArea.id
                            activity?.run {
                                startActivity(Intent(this, ToolMenuActivity::class.java))
                                finish()
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
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
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
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            Text(
                "${stringResource(R.string.area)} $name",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun DiagnosticObjectItem(
    name: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onNew: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(enabled = onClick != null, onClick = onClick ?: {})
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
        AnimatedVisibility(onNew != null) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = stringResource(R.string.back),
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
private fun AddObjectDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = LocalCoroutineScope.current
    val snackbarHostState = LocalSnackbarHostState.current
    var idInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest) {
        BorderedDialogContainer {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    stringResource(R.string.create_object),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    "${stringResource(R.string.registration_number)}:",
                    style = MaterialTheme.typography.titleSmall
                )
                TextField(
                    idInput,
                    onValueChange = { idInput = it },
                    placeholder = {
                        Text(
                            stringResource(R.string.input_registration_number),
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
                    "${stringResource(R.string.title)}:",
                    style = MaterialTheme.typography.titleSmall
                )
                TextField(
                    nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = {
                        Text(
                            stringResource(R.string.input_title),
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
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PtmFilledButton(
                        onClick = {
                            val id = idInput
                            val name = nameInput
                            if (id.isEmpty() || name.isEmpty()) {
                                coroutineScope?.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(context.getString(R.string.wrong_profile_input))
                                }
                                return@PtmFilledButton
                            }
                            AppState.reportDb?.diagnosticAreaDao()?.run {
                                insertDiagnosticObject(
                                    DiagnosticObject(
                                        id = id,
                                        inventoryId = id,
                                        name = name
                                    )
                                )
                            }
                            onDismissRequest()
                        }
                    ) {
                        Text(stringResource(R.string.submit))
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
private fun AddAreaDialog(
    diagnosticObject: DiagnosticObject,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = LocalCoroutineScope.current
    val snackbarHostState = LocalSnackbarHostState.current
    var idInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest) {
        BorderedDialogContainer {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    stringResource(R.string.create_area),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    "${stringResource(R.string.registration_number)}:",
                    style = MaterialTheme.typography.titleSmall
                )
                TextField(
                    idInput,
                    onValueChange = { idInput = it },
                    placeholder = {
                        Text(
                            stringResource(R.string.input_registration_number),
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
                    "${stringResource(R.string.title)}:",
                    style = MaterialTheme.typography.titleSmall
                )
                TextField(
                    nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = {
                        Text(
                            stringResource(R.string.input_title),
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
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PtmFilledButton(
                        onClick = {
                            val id = idInput
                            val name = nameInput
                            if (id.isEmpty() || name.isEmpty()) {
                                coroutineScope?.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(context.getString(R.string.wrong_profile_input))
                                }
                                return@PtmFilledButton
                            }
                            AppState.reportDb?.diagnosticAreaDao()?.run {
                                insertDiagnosticArea(
                                    DiagnosticArea(
                                        id = id,
                                        name = name,
                                        diagnosticObject = diagnosticObject.id,
                                        dangerClass = "0"
                                    )
                                )
                            }
                            onDismissRequest()
                        }
                    ) {
                        Text(stringResource(R.string.submit))
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
private fun ContentPreview() = PtmTheme { Content() }

@Preview
@Composable
private fun AddObjectDialogPreview() = PtmTheme { AddObjectDialog { } }