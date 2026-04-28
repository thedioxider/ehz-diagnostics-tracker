package space.diomentia.ehzdiagnosticstracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.ScrollableColumn
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme

@Composable
fun HomeScreen(
    onNewReport: () -> Unit,
    onHistory: () -> Unit,
    onExport: () -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = { PtmTopBar { Text(stringResource(R.string.app_name)) } },
        bottomBar = {
            PtmBottomBar(
                navigation = {
                    ActionButton(
                        icon = rememberVectorPainter(Icons.AutoMirrored.Outlined.ArrowBack),
                        label = stringResource(R.string.back)
                    ) { onBack() }
                },
                actions = {
                    ActionButton(
                        icon = rememberVectorPainter(Icons.Outlined.Settings),
                        label = stringResource(R.string.settings)
                    ) { onSettings() }
                }
            )
        },
        snackbarHost = { PtmSnackbarHost(LocalSnackbarHostState.current) }
    ) { innerPadding ->
        ScrollableColumn(innerPadding) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                PtmFilledButton(
                    onClick = onNewReport,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.start_diagnostics))
                }
                PtmFilledButton(
                    onClick = onHistory,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.diagnostics_history))
                }
                PtmFilledButton(
                    onClick = onExport,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.export_reports))
                }
            }
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() = PtmTheme { HomeScreen({}, {}, {}, {}, {}) }
