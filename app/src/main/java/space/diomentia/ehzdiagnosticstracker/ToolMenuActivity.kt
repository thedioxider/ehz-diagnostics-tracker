package space.diomentia.ehzdiagnosticstracker

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.DiagnosticAreaWithPoints
import space.diomentia.ehzdiagnosticstracker.data.DiagnosticObjectWithAreas
import space.diomentia.ehzdiagnosticstracker.data.LocalCoroutineScope
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.ReportConfig
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmOutlinedButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.clickableWithLabel
import space.diomentia.ehzdiagnosticstracker.ui.setupEdgeToEdge
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme
import java.time.LocalDateTime

private val LocalDiagnosticObject =
    compositionLocalOf<State<DiagnosticObjectWithAreas?>> { mutableStateOf(null) }
private val LocalDiagnosticArea =
    compositionLocalOf<State<DiagnosticAreaWithPoints?>> { mutableStateOf(null) }

class ToolMenuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge(this)
        val snackbarHostState = SnackbarHostState()
        setContent {
            BackHandler {
                startActivity(Intent(this, DiagnosticObjectChooseActivity::class.java))
            }

            CompositionLocalProvider(
                LocalCoroutineScope provides rememberCoroutineScope(),
                LocalSnackbarHostState provides snackbarHostState,
                LocalDiagnosticObject provides (AppState.CurrentDiagnosticSession.diagnosticObject?.let {
                    AppState.reportDb
                        ?.diagnosticAreaDao()
                        ?.getObjectById(it)
                        ?.collectAsStateWithLifecycle(null)
                } ?: remember { mutableStateOf(null) }),
                LocalDiagnosticArea provides (AppState.CurrentDiagnosticSession.diagnosticArea?.let {
                    AppState.reportDb
                        ?.diagnosticAreaDao()
                        ?.getAreaById(it)
                        ?.collectAsStateWithLifecycle(null)
                } ?: remember { mutableStateOf(null) })
            ) {
                Content()
            }
        }
    }
}

@Composable
private fun Content() {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val snackbarHostState = LocalSnackbarHostState.current

    PtmTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
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
                            activity?.run {
                                startActivity(Intent(context, AuthorizationActivity::class.java))
                                finish()
                            }
                        }
                        ActionButton(
                            icon = rememberVectorPainter(Icons.Outlined.LocationOn)
                        ) {
                            activity?.run {
                                startActivity(
                                    Intent(
                                        context,
                                        DiagnosticObjectChooseActivity::class.java
                                    )
                                )
                                finish()
                            }
                        }
                    },
                    actions = {
                        ActionButton(
                            icon = rememberVectorPainter(Icons.Outlined.Settings),
                            label = stringResource(R.string.settings)
                        ) {
                            context.startActivity(
                                Intent(context, SettingsActivity::class.java)
                            )
                        }
                    }
                )
            },
            snackbarHost = { PtmSnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AreaInfo()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ScanQrButton(
                        modifier = Modifier.size(200.dp)
                    )
                    ContinueEditingButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
                MenuActionButtonsRow(
                    Modifier
                        .fillMaxWidth()
                )
                DiagnosticsTypeSelector()
            }
        }
    }
}

@Composable
private fun AreaInfo(
    modifier: Modifier = Modifier
) {
    val dObject by LocalDiagnosticObject.current
    val dArea by LocalDiagnosticArea.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            dObject?.diagnosticObject?.name ?: stringResource(R.string.unknown),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            "${stringResource(R.string.area)} ${dArea?.diagnosticArea?.id}",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

@Composable
private fun ScanQrButton(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = LocalCoroutineScope.current
    val activity = LocalActivity.current
    val snackbarHostState = LocalSnackbarHostState.current
    val dArea by LocalDiagnosticArea.current

    val scanQrLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@rememberLauncherForActivityResult
        val id = result.data?.extras?.getString(ScanIdActivity.QR_DATA)
        if (id == null) {
            coroutineScope?.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(context.getString(R.string.qr_no_id))
            }
            return@rememberLauncherForActivityResult
        }
        val foundPoint = dArea?.points?.find { it.id == id }
        if (foundPoint == null) {
            coroutineScope?.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar("Участок не принадлежит объекту")
            }
            return@rememberLauncherForActivityResult
        }
        AppState.CurrentDiagnosticSession.reset()
        AppState.CurrentDiagnosticSession.diagnosticPoint = foundPoint.id
        AppState.CurrentDiagnosticSession.dateTime = LocalDateTime.now()
        activity?.run {
            val intent = Intent(context, TechDiagnosticsActivity::class.java)
            startActivity(intent)
        }
    }
    val launchScanQr = {
        val intent = Intent(context, ScanIdActivity::class.java)
        scanQrLauncher.launch(intent)
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        launchScanQr()
    }

    Box(modifier = modifier) {
        PtmOutlinedButton(
            roundedCorners = true,
            modifier = Modifier
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
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ContinueEditingButton(
    modifier: Modifier = Modifier
) {
    val activity = LocalActivity.current
    val interactionSource = remember { MutableInteractionSource() }
    val isVisible = AppState.CurrentDiagnosticSession.diagnosticPoint != null
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .then(modifier)
            .alpha(if (isVisible) 1f else 0f)
            .clickableWithLabel(
                enabled = isVisible,
                label = stringResource(R.string.continue_editing),
                interactionSource = interactionSource
            ) {
                activity?.run {
                    val intent = Intent(activity, TechDiagnosticsActivity::class.java)
                    startActivity(intent)
                }
            }
            .padding(12.dp)
    ) {
        Icon(
            rememberVectorPainter(Icons.Outlined.Edit),
            contentDescription = stringResource(R.string.continue_editing),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
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
            fontWeight = FontWeight.Medium
        )
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
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .then(modifier)
            .padding(horizontal = 6.dp)
    ) {
        PtmOutlinedButton(
            enabled = enabled,
            roundedCorners = true,
            modifier = Modifier
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
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            shortDescription,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .padding(8.dp)
        )
    }
}

@Composable
private fun MenuActionButtonsRow(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dArea by LocalDiagnosticArea.current
    val areaLastLocation = dArea?.diagnosticArea?.lastLocation
    val rowHeight = 80.dp

    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .then(modifier)
    ) {
        val actionButtonModifier = Modifier
            .weight(1f)
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
            context.startActivity(
                Intent(context, ReportHistoryActivity::class.java)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiagnosticsTypeSelector(
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val options = stringArrayResource(R.array.diagnostics_types)
    val colors = TextFieldDefaults.colors(
        unfocusedContainerColor = MaterialTheme.colorScheme.background,
        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
        focusedContainerColor = MaterialTheme.colorScheme.background,
        focusedTextColor = MaterialTheme.colorScheme.onBackground
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = modifier
        ) {
            TextField(
                value = options[AppState.CurrentDiagnosticSession.reportType],
                colors = colors,
                supportingText = {
                    Text(
                        stringResource(R.string.diagnostics_type),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
            )
            ExposedDropdownMenu(
                expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEachIndexed { i, option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            AppState.CurrentDiagnosticSession.reportType = i
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
private fun ContentPreview() {
    AppState.reportConfig.value = ReportConfig(listOf())

    PtmTheme {
        Content()
    }
}