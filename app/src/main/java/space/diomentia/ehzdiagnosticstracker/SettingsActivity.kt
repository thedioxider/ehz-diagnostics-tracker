package space.diomentia.ehzdiagnosticstracker

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.OrganizationInfo
import space.diomentia.ehzdiagnosticstracker.data.ReportConfig
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.ScrollableColumn
import space.diomentia.ehzdiagnosticstracker.ui.SettingsOption
import space.diomentia.ehzdiagnosticstracker.ui.SettingsOptionsDivider
import space.diomentia.ehzdiagnosticstracker.ui.setupEdgeToEdge
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme

class SettingsActivity : ComponentActivity() {
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

@Composable
private fun Content() {
    val activity = LocalActivity.current
    val snackbarHostState = LocalSnackbarHostState.current
    LocalLayoutDirection.current

    val reportConfigChooser =
        AppState.localYamlConfigCopyLauncher<ReportConfig>(AppState.REPORT_CONFIG_FILENAME) {
            AppState.reportConfig.value = it
        }
    val customerInfoChooser =
        AppState.localYamlConfigCopyLauncher<OrganizationInfo>(AppState.CUSTOMER_INFO_FILENAME) {
            AppState.customerInfo.value = it
        }
    val organizationInfoChooser =
        AppState.localYamlConfigCopyLauncher<OrganizationInfo>(AppState.ORGANIZATION_INFO_FILENAME) {
            AppState.organizationInfo.value = it
        }

    PtmTheme {
        Scaffold(
            topBar = { PtmTopBar { Text(stringResource(R.string.settings)) } },
            bottomBar = {
                PtmBottomBar(
                    navigation = {
                        ActionButton(
                            icon = rememberVectorPainter(Icons.AutoMirrored.Outlined.ArrowBack),
                            label = stringResource(R.string.back)
                        ) {
                            activity?.run {
                                setResult(Activity.RESULT_CANCELED)
                                finish()
                            }
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
                SettingsOption(
                    label = stringResource(R.string.customer_info),
                    secondaryLabel = stringResource(R.string.customer_info_description),
                    icon = painterResource(R.drawable.ic_person)
                ) {
                    customerInfoChooser.launch(arrayOf("application/*"))
                }
                SettingsOption(
                    label = stringResource(R.string.diagnostics_organization_info),
                    secondaryLabel = stringResource(R.string.diagnostics_organization_info_description),
                    icon = painterResource(R.drawable.ic_domain)
                ) {
                    organizationInfoChooser.launch(arrayOf("application/*"))
                }
            }
        }
    }
}


@Preview
@Composable
private fun ContentPreview() {
    Content()
}