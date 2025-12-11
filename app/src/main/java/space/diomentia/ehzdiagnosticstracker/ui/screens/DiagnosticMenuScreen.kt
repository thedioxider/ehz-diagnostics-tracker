package space.diomentia.ehzdiagnosticstracker.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.DiagnosticArea
import space.diomentia.ehzdiagnosticstracker.data.DiagnosticObject
import space.diomentia.ehzdiagnosticstracker.data.DiagnosticSession
import space.diomentia.ehzdiagnosticstracker.data.LocalDiagnosticSession
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.ReportConfig
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmOutlinedButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.clickableWithLabel
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme

@Composable
fun DiagnosticMenuScreen(
    diagnosticSession: DiagnosticSession,
    onAuthorize: () -> Unit,
    onChooseArea: () -> Unit,
    onSettings: () -> Unit,
    onPointList: () -> Unit,
    onHistory: () -> Unit
) {
    val snackbarHostState = LocalSnackbarHostState.current
    CompositionLocalProvider(
        LocalDiagnosticSession provides remember { mutableStateOf(diagnosticSession) }
    ) {
        PtmTheme {
            Scaffold(
                modifier = Modifier.Companion.fillMaxSize(),
                topBar = {
                    PtmTopBar(
                        title = {
                            Text(stringResource(R.string.app_name))
                        }
                    )
                },
                bottomBar = {
                    PtmBottomBar(
                        navigation = {
                            ActionButton(
                                icon = painterResource(R.drawable.ic_person),
                                label = stringResource(R.string.change_account)
                            ) {
                                onAuthorize()
                            }
                            ActionButton(
                                icon = rememberVectorPainter(Icons.Outlined.LocationOn)
                            ) {
                                onChooseArea()
                            }
                        },
                        actions = {
                            ActionButton(
                                icon = rememberVectorPainter(Icons.Outlined.Settings),
                                label = stringResource(R.string.settings)
                            ) {
                                onSettings()
                            }
                        }
                    )
                },
                snackbarHost = { PtmSnackbarHost(snackbarHostState) }
            ) { innerPadding ->
                Column(
                    modifier = Modifier.Companion
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.Companion.CenterHorizontally
                ) {
                    AreaInfo()
                    MainActions(
                        onPointList = onPointList,
                        onHistory = onHistory
                    )
                    DiagnosticsTypeSelector()
                    LastReport(
                        modifier = Modifier.fillMaxWidth(),
                        onHistory = onHistory,
                        onPointList = onPointList
                    )
                }
            }
        }
    }
}

@Composable
private fun AreaInfo(
    modifier: Modifier = Modifier
) {
    val diagnosticSession by LocalDiagnosticSession.current

    Column(
        horizontalAlignment = Alignment.Companion.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            diagnosticSession.diagnosticObject.name,
            textAlign = TextAlign.Companion.Center,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            "${stringResource(R.string.area)} ${diagnosticSession.diagnosticArea.name}",
            textAlign = TextAlign.Companion.Center,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            "${stringResource(R.string.profile)}: " + AppState.profileSession?.let { "${it.surname} ${it.name[0]}." },
            textAlign = TextAlign.Companion.Center,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
private fun MainActions(
    modifier: Modifier = Modifier,
    onPointList: () -> Unit,
    onHistory: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.Companion.CenterHorizontally,
        modifier = Modifier.then(modifier)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MenuActionButton(
                buttonHeight = 100.dp,
                description = stringResource(R.string.diagnostic_points),
                icon = painterResource(R.drawable.ic_edit_location)
            ) {
                onPointList()
            }
            MenuActionButton(
                buttonHeight = 100.dp,
                description = stringResource(R.string.diagnostics_history),
                icon = painterResource(R.drawable.ic_history)
            ) {
                onHistory()
            }
        }
    }
}

/*
@Composable
private fun ScanQrButton(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = LocalCoroutineScope.current
    val snackbarHostState = LocalSnackbarHostState.current
    val diagnosticSession by LocalDiagnosticSession.current

    rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        // TODO
        //val id = result.data?.extras?.getString(ScanIdActivity.Companion.QR_DATA)
        val id: String? = null
        if (id == null) {
            coroutineScope?.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(context.getString(R.string.qr_no_id))
            }
            return@rememberLauncherForActivityResult
        }
        val foundPoint = diagnosticSession.diagnosticArea.points.find { it.id == id }
        if (foundPoint == null) {
            coroutineScope?.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar("Участок не принадлежит объекту")
            }
            return@rememberLauncherForActivityResult
        }
        /*
        AppState.CurrentDiagnosticSession.reset()
        AppState.CurrentDiagnosticSession.diagnosticPoint = foundPoint.id
        AppState.CurrentDiagnosticSession.validationId = foundPoint.id
        AppState.CurrentDiagnosticSession.dateTime = LocalDateTime.now()
         */
        /* TODO
        activity?.run {
            val intent = Intent(context, TechDiagnosticsActivity::class.java)
            startActivity(intent)
        }
         */
    }
    val launchScanQr = {
        /* TODO
        val intent = Intent(context, ScanIdActivity::class.java)
        scanQrLauncher.launch(intent)
         */
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        launchScanQr()
    }

    Box(modifier = modifier) {
        PtmOutlinedButton(
            roundedCorners = true,
            modifier = Modifier.Companion
                .aspectRatio(1f)
                .clickableWithLabel(
                    label = stringResource(R.string.scan_id)
                ) {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    } else {
                        launchScanQr()
                    }
                }
        ) {
            Icon(
                painterResource(R.drawable.ic_qr_code),
                contentDescription = stringResource(R.string.scan_id),
                modifier = Modifier.Companion.fillMaxSize()
            )
        }
    }
}
 */

/*
@Composable
private fun ContinueEditingButton(
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isVisible = AppState.CurrentDiagnosticSession.diagnosticPoint != null
    Row(
        verticalAlignment = Alignment.Companion.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.Companion
            .then(modifier)
            .alpha(if (isVisible) 1f else 0f)
            .clickableWithLabel(
                enabled = isVisible,
                label = stringResource(R.string.continue_editing),
                interactionSource = interactionSource
            ) {
                /* TODO
                activity?.run {
                    val intent = Intent(activity, TechDiagnosticsActivity::class.java)
                    startActivity(intent)
                }
                 */
            }
            .padding(12.dp)
    ) {
        Icon(
            rememberVectorPainter(Icons.Outlined.Edit),
            contentDescription = stringResource(R.string.continue_editing),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.Companion
                .padding(horizontal = 8.dp)
                .indication(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = false)
                )
                .size(24.dp)
        )
        Text(
            stringResource(R.string.continue_editing_short).lowercase(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Companion.Medium
        )
    }
}
 */

@Composable
private fun MenuActionButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    buttonHeight: Dp? = null,
    description: String,
    shortDescription: String = description,
    icon: Painter,
    action: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.Companion.CenterHorizontally,
        modifier = Modifier.Companion
            .then(modifier)
            .padding(horizontal = 6.dp)
    ) {
        PtmOutlinedButton(
            enabled = enabled,
            roundedCorners = true,
            modifier = Modifier.Companion
                .let { if (buttonHeight != null) it.height(buttonHeight) else it }
                .weight(1f, fill = false)
                .aspectRatio(1f)
                .clickableWithLabel(
                    enabled = enabled,
                    label = description,
                    action = action
                )
        ) {
            Icon(
                icon,
                contentDescription = description,
                modifier = Modifier.Companion.fillMaxSize()
            )
        }
        Text(
            shortDescription,
            textAlign = TextAlign.Companion.Center,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.Companion
                .padding(8.dp)
        )
    }
}

@Composable
private fun LastReport(
    onPointList: () -> Unit,
    onHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val diagnosticSession by LocalDiagnosticSession.current
    val areaLastLocation = diagnosticSession.diagnosticArea.lastLocation
    val rowHeight = 80.dp

    Row(
        verticalAlignment = Alignment.Companion.Top,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.Companion
            .then(modifier)
    ) {
        val actionButtonModifier = Modifier
            .weight(1f)
        MenuActionButton(
            actionButtonModifier,
            buttonHeight = rowHeight,
            description = stringResource(R.string.diagnostic_points),
            icon = painterResource(R.drawable.ic_edit_location)
        ) {
            onPointList()
        }
        MenuActionButton(
            actionButtonModifier,
            enabled = areaLastLocation != null,
            buttonHeight = rowHeight,
            description = stringResource(R.string.show_area_on_map),
            shortDescription = stringResource(R.string.show_area_on_map_short),
            icon = painterResource(R.drawable.ic_explore)
        ) {
            areaLastLocation?.let {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "geo:0,0?q=${it.latitude},${it.longitude}".toUri()
                    )
                )
            }
        }
        MenuActionButton(
            actionButtonModifier,
            buttonHeight = rowHeight,
            description = stringResource(R.string.diagnostics_history),
            icon = painterResource(R.drawable.ic_history)
        ) {
            onHistory()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiagnosticsTypeSelector(
    modifier: Modifier = Modifier
) {
    var diagnosticSession by LocalDiagnosticSession.current
    var expanded by remember { mutableStateOf(false) }
    val options = stringArrayResource(R.array.diagnostics_types)
    val colors = TextFieldDefaults.colors(
        unfocusedContainerColor = MaterialTheme.colorScheme.background,
        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
        focusedContainerColor = MaterialTheme.colorScheme.background,
        focusedTextColor = MaterialTheme.colorScheme.onBackground
    )

    Column(
        horizontalAlignment = Alignment.Companion.CenterHorizontally
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = modifier
        ) {
            TextField(
                value = options[diagnosticSession.diagnosticType],
                colors = colors,
                supportingText = {
                    Text(
                        stringResource(R.string.diagnostics_type),
                        textAlign = TextAlign.Companion.Center,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        modifier = Modifier.Companion.fillMaxWidth()
                    )
                },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.Companion.menuAnchor(
                    MenuAnchorType.Companion.PrimaryNotEditable,
                    true
                )
            )
            ExposedDropdownMenu(
                expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEachIndexed { i, option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            diagnosticSession = diagnosticSession.copy(diagnosticType = i)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun DiagnosticMenuScreenPreview() {
    AppState.reportConfig.value = ReportConfig(listOf())
    val diagnosticSession = DiagnosticSession(
        diagnosticObject = DiagnosticObject("", "", "Test Object"),
        diagnosticArea = DiagnosticArea("", "Test Area", "", "")
    )

    PtmTheme {
        DiagnosticMenuScreen(diagnosticSession, {}, {}, {}, {}, {})
    }
}