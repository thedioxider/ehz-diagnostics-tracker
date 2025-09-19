package space.diomentia.ehzdiagnosticstracker

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.launch
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.ReportConfig
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmOutlinedButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.clickableWithLabel
import space.diomentia.ehzdiagnosticstracker.ui.setupEdgeToEdge
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppState.dataDir = dataDir
        setupEdgeToEdge(this)
        val snackbarHostState = SnackbarHostState()
        setContent {
            CompositionLocalProvider(
                LocalSnackbarHostState provides snackbarHostState
            ) {
                Content()
            }
        }
    }
}

@Composable
private fun Content() {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current

    PtmTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                PtmTopBar(
                    title = { Text(stringResource(R.string.app_name)) }
                )
            },
            bottomBar = {
                PtmBottomBar(
                    actions = {
                        ActionButton(
                            icon = painterResource(R.drawable.ic_history),
                            label = stringResource(R.string.diagnostics_history)
                        ) {}
                        ActionButton(
                            icon = rememberVectorPainter(Icons.Outlined.Settings),
                            label = stringResource(R.string.settings)
                        ) {
                            val intent = Intent(context, SettingsActivity::class.java)
                            context.startActivity(intent)
                        }
                    }
                )
            },
            snackbarHost = { PtmSnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            if (AppState.reportConfig.value != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ScanQrButton(
                            modifier = Modifier.size(200.dp)
                        )
                        ContinueEditingButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        )
                    }
                    MenuActionButtonsRow(
                        Modifier
                            .fillMaxWidth()
                    )
                }
            } else {
                RequestConfigScreen(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun ScanQrButton(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = LocalActivity.current
    val snackbarHostState = LocalSnackbarHostState.current

    val scanQrLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data?.extras?.getString(ScanIdActivity.QR_DATA)
        val id = data?.toIntOrNull()
        if (id != null) {
            AppState.CurrentDiagnosticSession.reset()
            AppState.CurrentDiagnosticSession.id = id
            activity?.run {
                val intent = Intent(context, TechDiagnosticsActivity::class.java)
                startActivity(intent)
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(context.getString(R.string.qr_no_id))
            }
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
    val isVisible = AppState.CurrentDiagnosticSession.id != null
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
                .padding(horizontal = 16.dp)
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
            roundedCorners = true,
            modifier = Modifier
                .let { if (buttonHeight != null) it.height(buttonHeight) else it }
                .weight(1f, fill = false)
                .aspectRatio(1f)
                .clickableWithLabel(
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
            buttonHeight = rowHeight,
            description = stringResource(R.string.list_areas),
            shortDescription = stringResource(R.string.list_areas_short),
            icon = painterResource(R.drawable.ic_explore)
        ) {}
        MenuActionButton(
            actionButtonModifier,
            buttonHeight = rowHeight,
            description = stringResource(R.string.customer_info),
            icon = painterResource(R.drawable.ic_person)
        ) {}
        MenuActionButton(
            actionButtonModifier,
            buttonHeight = rowHeight,
            description = stringResource(R.string.diagnostics_organization_info),
            shortDescription = stringResource(R.string.diagnostics_organization_info_short),
            icon = painterResource(R.drawable.ic_domain)
        ) {}
    }
}

@Composable
private fun RequestConfigScreen(
    modifier: Modifier = Modifier
) {
    val configChooserLauncher =
        AppState.localYamlConfigCopyLauncher<ReportConfig>(AppState.REPORT_CONFIG_FILENAME) {
            AppState.reportConfig.value = it
        }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            stringResource(R.string.import_report_config),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(6.dp)
        )
        PtmFilledButton(
            onClick = {
                configChooserLauncher.launch(arrayOf("application/*"))
            },
            modifier = Modifier.padding(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painterResource(R.drawable.ic_file_open),
                    contentDescription = stringResource(R.string.import_report_config),
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(stringResource(R.string.choose_file))
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