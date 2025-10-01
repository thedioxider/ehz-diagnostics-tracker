package space.diomentia.ehzdiagnosticstracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.FullscreenLazyColumn
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
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
    LocalLayoutDirection.current

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
                    }
                )
            }
        ) { innerPadding ->
            val diagnosticObjects by AppState.reportDb
                ?.diagnosticAreaDao()
                ?.getAllObjects()
                ?.collectAsStateWithLifecycle(emptyList())
                ?: remember { mutableStateOf(emptyList()) }
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
            FullscreenLazyColumn(innerPadding) {
                items(diagnosticObjects) { diagnosticObject ->
                    AnimatedVisibility(
                        AppState.CurrentDiagnosticSession.diagnosticObject == null
                                || AppState.CurrentDiagnosticSession.diagnosticObject == diagnosticObject
                    ) {
                        DiagnosticObjectItem(
                            diagnosticObject.diagnosticObject.name,
                            onBack = if (AppState.CurrentDiagnosticSession.diagnosticObject == diagnosticObject) { ->
                                AppState.CurrentDiagnosticSession.diagnosticObject = null
                            } else null,
                            onClick = if (AppState.CurrentDiagnosticSession.diagnosticObject == null) { ->
                                AppState.CurrentDiagnosticSession.diagnosticObject =
                                    diagnosticObject
                            } else null
                        )
                    }
                }
                AppState.CurrentDiagnosticSession.diagnosticObject?.let { dObject ->
                    items(dObject.areas) { area ->
                        AnimatedVisibility(AppState.CurrentDiagnosticSession.diagnosticObject != null) {
                            DiagnosticAreaItem(
                                area.id,
                                modifier = Modifier.padding(start = 64.dp)
                            ) {
                                AppState.CurrentDiagnosticSession.diagnosticArea = AppState.reportDb
                                    ?.diagnosticAreaDao()
                                    ?.getAreaById(area.id)
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
}

@Composable
private fun DiagnosticAreaItem(
    name: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(enabled = onClick != null, onClick = onClick ?: {})
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
                contentDescription = stringResource(R.string.back),
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
    }
}


@Preview
@Composable
private fun ContentPreview() = PtmTheme { Content() }
