package space.diomentia.ehzdiagnosticstracker.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.DiagnosticArea
import space.diomentia.ehzdiagnosticstracker.data.DiagnosticObject
import space.diomentia.ehzdiagnosticstracker.data.DiagnosticSession
import space.diomentia.ehzdiagnosticstracker.data.LocalDiagnosticSession
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.ReportConfig
import space.diomentia.ehzdiagnosticstracker.data.ReportDatabase
import space.diomentia.ehzdiagnosticstracker.data.ReportSession
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
    onHistory: () -> Unit,
    onEditReport: (ReportSession) -> Unit,
    reportSession: ReportSession? = null
) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    val history by ReportDatabase.getInstance()
        .diagnosticAreaDao()
        .getAreaPoints(diagnosticSession.diagnosticArea.id)
        .collectAsStateWithLifecycle(emptyList())
    val latestReport = history.flatMap { it.history }.maxByOrNull { it.timestamp }

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
                        reportSession = reportSession ?: latestReport?.let {
                            ReportSession
                                .fromExisting(context, diagnosticSession, latestReport.id)
                        },
                        onEditReport = onEditReport,
                        saved = reportSession == null,
                        modifier = Modifier.fillMaxWidth()
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
                description = stringResource(R.string.diagnostics_history),
                icon = painterResource(R.drawable.ic_history)
            ) {
                onHistory()
            }
            MenuActionButton(
                buttonHeight = 100.dp,
                description = stringResource(R.string.diagnostic_points),
                icon = painterResource(R.drawable.ic_edit_location)
            ) {
                onPointList()
            }
        }
    }
}

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
    reportSession: ReportSession?,
    saved: Boolean,
    onEditReport: (ReportSession) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val diagnosticSession by LocalDiagnosticSession.current
    val areaLastLocation = diagnosticSession.diagnosticArea.lastLocation
    val rowHeight = 80.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.then(modifier)
    ) {
        reportSession?.let { session ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "${stringResource(R.string.last_point_report)}: ${session.diagnosticPoint.inventoryId}",
                    style = MaterialTheme.typography.titleLarge
                )
                if (!saved) {
                    Text(
                        stringResource(R.string.last_report_not_saved),
                        style = MaterialTheme.typography.labelMedium,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(
            verticalAlignment = Alignment.Companion.Top,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val actionButtonModifier = Modifier
                .weight(1f)
            MenuActionButton(
                actionButtonModifier,
                enabled = reportSession != null,
                buttonHeight = rowHeight,
                description = stringResource(R.string.continue_editing),
                shortDescription = stringResource(R.string.continue_editing_short),
                icon = rememberVectorPainter(Icons.Outlined.Edit)
            ) {
                onEditReport(reportSession!!)
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
                enabled = reportSession != null,
                buttonHeight = rowHeight,
                description = stringResource(R.string.new_report_there),
                icon = rememberVectorPainter(Icons.Outlined.Add)
            ) {
                onEditReport(
                    ReportSession(
                        diagnosticSession = diagnosticSession,
                        diagnosticPoint = reportSession!!.diagnosticPoint
                    )
                )
            }
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
        DiagnosticMenuScreen(diagnosticSession, {}, {}, {}, {}, {}, {})
    }
}