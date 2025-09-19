package space.diomentia.ehzdiagnosticstracker

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.CustomerInfo
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.ReportConfig
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
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
    val layoutDirection = LocalLayoutDirection.current

    val reportConfigChooser =
        AppState.localYamlConfigCopyLauncher<ReportConfig>(AppState.REPORT_CONFIG_FILENAME) {
            AppState.reportConfig.value = it
        }
    val customerInfoChooser =
        AppState.localYamlConfigCopyLauncher<CustomerInfo>(AppState.CUSTOMER_INFO_FILENAME) {
            AppState.customerInfo.value = it
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
            Column(
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = innerPadding.calculateStartPadding(layoutDirection),
                        end = innerPadding.calculateEndPadding(layoutDirection)
                    )
            ) {
                Spacer(Modifier.height(innerPadding.calculateTopPadding()))

                SettingsOption(
                    label = stringResource(R.string.report_configuration),
                    secondaryLabel = stringResource(R.string.report_configuration_description),
                    icon = painterResource(R.drawable.ic_file_open)
                ) {
                    reportConfigChooser.launch(arrayOf("application/*"))
                }
                OptionsDivider()
                SettingsOption(
                    label = stringResource(R.string.customer_info),
                    secondaryLabel = stringResource(R.string.customer_info_description),
                    icon = painterResource(R.drawable.ic_person)
                ) {
                    customerInfoChooser.launch(arrayOf("application/*"))
                }

                Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
            }
        }
    }
}

@Composable
private fun SettingsOption(
    label: String,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    icon: Painter? = null,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(24.dp)
            .defaultMinSize(minHeight = 36.dp)
            .height(IntrinsicSize.Min)
            .then(modifier)
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(32.dp)
            )
        }
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleMedium
            )
            if (secondaryLabel != null) {
                Text(
                    secondaryLabel,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .alpha(.7f)
                )
            }
        }
    }
}

@Composable
private fun OptionsDivider(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        thickness = 2.dp,
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .then(modifier)
    )
}

@Preview
@Composable
private fun ContentPreview() {
    Content()
}

@Preview(showBackground = true)
@Composable
private fun SettingsOptionPreview() {
    PtmTheme {
        SettingsOption(
            label = "Some settings option",
            secondaryLabel = "Description of this option",
            icon = rememberVectorPainter(Icons.Outlined.Settings)
        ) {}
    }
}