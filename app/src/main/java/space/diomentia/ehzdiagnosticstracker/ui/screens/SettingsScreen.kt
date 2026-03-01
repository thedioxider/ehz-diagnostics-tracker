package space.diomentia.ehzdiagnosticstracker.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.ReportConfig
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.ScrollableColumn
import space.diomentia.ehzdiagnosticstracker.ui.SettingsOption
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val snackbarHostState = LocalSnackbarHostState.current

    val reportConfigChooser =
        AppState.localYamlConfigCopyLauncher<ReportConfig>(AppState.REPORT_CONFIG_FILENAME) {
            AppState.reportConfig.value = it
        }

    Scaffold(
        topBar = { PtmTopBar { Text(stringResource(R.string.settings)) } },
        bottomBar = {
            PtmBottomBar(
                navigation = {
                    ActionButton(
                        icon = rememberVectorPainter(Icons.AutoMirrored.Outlined.ArrowBack),
                        label = stringResource(R.string.back)
                    ) {
                        onBack()
                    }
                }
            )
        },
        snackbarHost = { PtmSnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        ScrollableColumn(innerPadding) {
            SettingsOption(
                label = stringResource(R.string.report_configuration),
                secondaryLabel = stringResource(R.string.report_configuration_description),
                icon = painterResource(R.drawable.ic_file_open)
            ) {
                reportConfigChooser.launch(arrayOf("application/*"))
            }
        }
    }
}


@Preview
@Composable
private fun SettingsScreenPreview() = PtmTheme { SettingsScreen({}) }