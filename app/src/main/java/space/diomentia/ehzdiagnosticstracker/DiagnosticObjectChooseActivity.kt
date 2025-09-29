package space.diomentia.ehzdiagnosticstracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.DiagnosticObjectRelative
import space.diomentia.ehzdiagnosticstracker.data.DiagnosticReportWithData
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.ui.FullscreenLazyColumn
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
private fun Content(
    reports: MutableState<List<DiagnosticReportWithData>> = mutableStateOf(emptyList())
) {
    val activity = LocalActivity.current

    PtmTheme {
        Scaffold(
            snackbarHost = { PtmSnackbarHost(LocalSnackbarHostState.current) },
            topBar = {
                PtmTopBar(title = {
                    Text(stringResource(R.string.diagnostic_object))
                })
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
                    DiagnosticObjectItem(diagnosticObject) {
                        AppState.diagnosticObject = diagnosticObject
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

@Composable
private fun DiagnosticObjectItem(
    diagnosticObject: DiagnosticObjectRelative,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(24.dp)
            .defaultMinSize(minHeight = 36.dp)
            .height(IntrinsicSize.Min)
            .then(modifier)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            Text(
                diagnosticObject.diagnosticObject.name,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}


@Preview
@Composable
private fun ContentPreview() = PtmTheme { Content() }