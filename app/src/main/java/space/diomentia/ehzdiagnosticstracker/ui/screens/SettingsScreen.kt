package space.diomentia.ehzdiagnosticstracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.data.AppPreferences
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.ReportConfig
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.ScrollableColumn
import space.diomentia.ehzdiagnosticstracker.ui.SettingsOption
import space.diomentia.ehzdiagnosticstracker.ui.SettingsOptionsDivider
import space.diomentia.ehzdiagnosticstracker.ui.TextFieldWithLabel
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
    var backendUrl by AppPreferences.rememberPreference(AppPreferences.BACKEND_URL_KEY, "")
    var backendBearer by AppPreferences.rememberPreference(AppPreferences.BACKEND_BEARER_KEY, "")

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
            SettingsOptionsDivider()
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    stringResource(R.string.backend_settings),
                    style = MaterialTheme.typography.titleMedium
                )
                TextFieldWithLabel(
                    label = { Text(stringResource(R.string.backend_url)) },
                    value = backendUrl,
                    onValueChange = { backendUrl = it },
                    placeholderText = stringResource(R.string.backend_url_placeholder),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    )
                )
                TextFieldWithLabel(
                    label = { Text(stringResource(R.string.backend_bearer)) },
                    value = backendBearer,
                    onValueChange = { backendBearer = it },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    )
                )
            }
        }
    }
}


@Preview
@Composable
private fun SettingsScreenPreview() = PtmTheme { SettingsScreen({}) }
