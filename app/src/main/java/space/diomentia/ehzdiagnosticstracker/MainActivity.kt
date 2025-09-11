package space.diomentia.ehzdiagnosticstracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmOutlinedButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.clickableWithLabel
import space.diomentia.ehzdiagnosticstracker.ui.setupEdgeToEdge
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

@Preview
@Composable
private fun Content() {
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
                            icon = Icons.Outlined.History,
                            label = stringResource(R.string.diagnostics_history)
                        ) {}
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
                Text(
                    "Id: ${AppState.currentDiagnosticArea.id ?: "Not found"}",
                    modifier = Modifier
                        .alpha(if (AppState.currentDiagnosticArea.id == null) 0f else 1f)
                )
                ScanQrButton(
                    modifier = Modifier.size(200.dp)
                )
                MenuActionButtonsRow(
                    Modifier
                        .fillMaxWidth()
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
    val snackbarHostState = LocalSnackbarHostState.current

    val scanQrLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data?.extras?.getString(ScanIdActivity.QR_DATA)
        val id = data?.toIntOrNull()
        if (id != null) {
            AppState.currentDiagnosticArea.id = id
            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar("Id found: ${id}")
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar("Id not found...")
            }
        }
    }
    val launchScanQr = {
        val intent = Intent(
            context,
            ScanIdActivity::class.java
        )
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
                Icons.Outlined.QrCode,
                contentDescription = stringResource(R.string.scan_id),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun MenuActionButton(
    modifier: Modifier = Modifier,
    buttonHeight: Dp? = null,
    description: String,
    shortDescription: String = description,
    icon: ImageVector,
    action: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        PtmOutlinedButton(
            roundedCorners = true,
            modifier = Modifier
                .let { if (buttonHeight != null) it.height(buttonHeight) else it }
                .aspectRatio(1f)
                .weight(1f, fill = false)
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
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .padding(8.dp)
            // .width(IntrinsicSize.Min)
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
            icon = Icons.Outlined.LocationOn
        ) {}
        MenuActionButton(
            actionButtonModifier,
            buttonHeight = rowHeight,
            description = stringResource(R.string.customer_info),
            icon = Icons.Outlined.Person
        ) {}
        MenuActionButton(
            actionButtonModifier,
            buttonHeight = rowHeight,
            description = stringResource(R.string.diagnostics_organization_info),
            shortDescription = stringResource(R.string.diagnostics_organization_info_short),
            icon = Icons.Outlined.Business
        ) {}
    }
}