package space.diomentia.ehzdiagnosticstracker

import android.app.Activity.RESULT_CANCELED
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.LocalLocationProvider
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.ParameterType
import space.diomentia.ehzdiagnosticstracker.data.ReportConfig
import space.diomentia.ehzdiagnosticstracker.data.ReportParameter
import space.diomentia.ehzdiagnosticstracker.data.ReportType
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.RequestConfigScreen
import space.diomentia.ehzdiagnosticstracker.ui.ScrollableColumn
import space.diomentia.ehzdiagnosticstracker.ui.setupEdgeToEdge
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme
import java.time.format.DateTimeFormatter

private enum class UiState {
    Configuration,
    Editor;

    companion object {
        fun canOpenEditor(): Boolean {
            return with(AppState.CurrentDiagnosticSession) {
                diagnosticPoint != null && reportType != null
            }
        }
    }
}

private val LocalUiState =
    compositionLocalOf<MutableState<UiState>> { mutableStateOf(UiState.Configuration) }

class TechDiagnosticsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge(this)
        val snackbarHostState = SnackbarHostState()
        val pointId = AppState.CurrentDiagnosticSession.diagnosticPoint
        if (pointId == null) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        setContent {
            CompositionLocalProvider(
                LocalSnackbarHostState provides snackbarHostState,
                LocalUiState provides remember {
                    mutableStateOf(
                        if (UiState.canOpenEditor()) UiState.Editor else UiState.Configuration
                    )
                },
                LocalLocationProvider provides LocationServices.getFusedLocationProviderClient(this)
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
    var state by LocalUiState.current
    val diagnosticObject by AppState.CurrentDiagnosticSession.diagnosticObject?.let { id ->
        AppState.reportDb
            ?.diagnosticAreaDao()
            ?.getObjectById(id)
            ?.collectAsStateWithLifecycle(null)
    } ?: remember { mutableStateOf(null) }
    val diagnosticArea by AppState.CurrentDiagnosticSession.diagnosticArea?.let { id ->
        AppState.reportDb
            ?.diagnosticAreaDao()
            ?.getAreaById(id)
            ?.collectAsStateWithLifecycle(null)
    } ?: remember { mutableStateOf(null) }
    val diagnosticPoint by AppState.CurrentDiagnosticSession.diagnosticPoint?.let { diagnosticPoint ->
        AppState.reportDb
            ?.diagnosticAreaDao()
            ?.getPointById(diagnosticPoint)
            ?.collectAsStateWithLifecycle(null)
    } ?: remember { mutableStateOf(null) }

    if (AppState.CurrentDiagnosticSession.diagnosticPoint == null) activity?.finish()
    if (AppState.CurrentDiagnosticSession.reportType == null) state = UiState.Configuration
    PtmTheme {
        Scaffold(
            topBar = {
                PtmTopBar(
                    title = {
                        Text(stringResource(R.string.technical_diagnostics))
                    }
                )
            },
            bottomBar = {
                PtmBottomBar(
                    navigation = {
                        ActionButton(
                            icon = rememberVectorPainter(Icons.AutoMirrored.Outlined.ArrowBack),
                            label = stringResource(R.string.back)
                        ) {
                            activity?.run {
                                setResult(RESULT_CANCELED)
                                finish()
                            }
                        }
                    }
                )
            },
            snackbarHost = { PtmSnackbarHost(snackbarHostState) },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            if (AppState.reportConfig.value == null) {
                val configChooserLauncher =
                    AppState.localYamlConfigCopyLauncher<ReportConfig>(AppState.REPORT_CONFIG_FILENAME) {
                        AppState.reportConfig.value = it
                    }
                RequestConfigScreen(
                    label = stringResource(R.string.import_report_config),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    configChooserLauncher.launch(arrayOf("application/*"))
                }
                return@Scaffold
            }
            ScrollableColumn(
                innerPadding,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    "${stringResource(R.string.point)} ${
                        diagnosticPoint?.point?.inventoryId ?: stringResource(R.string.unknown)
                    }",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 36.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                )
                Text(
                    diagnosticObject?.diagnosticObject?.name ?: stringResource(R.string.unknown),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                )
                Text(
                    "${stringResource(R.string.area)} ${
                        diagnosticArea?.diagnosticArea?.name ?: stringResource(
                            R.string.unknown
                        )
                    }",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                )
                Text(
                    "${stringResource(R.string.report_by)} " + AppState.profileSession?.let { "${it.surname} ${it.name[0]}." },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    AppState.CurrentDiagnosticSession.dateTime
                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .alpha(.5f)
                        .padding(bottom = 16.dp)
                )

                AnimatedVisibility(state == UiState.Editor) {
                    Text(
                        AppState.CurrentDiagnosticSession.reportType?.let { reportType ->
                            AppState.reportConfig.value?.reportTypes[reportType]?.name
                        } ?: stringResource(R.string.unknown),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 8.dp)
                    )
                }

                AnimatedVisibility(state == UiState.Configuration) {
                    ReportConfiguration(Modifier.fillMaxWidth())
                }

                ApplyButton()

                AnimatedVisibility(state == UiState.Editor)
                {
                    ReportEditor(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ReportTypesList(
    modifier: Modifier = Modifier
) {
    val reportConfig = AppState.reportConfig.value
    if (reportConfig == null) {
        Text(
            stringResource(R.string.no_report_configuration),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
        return
    }

    var state by LocalUiState.current

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
        modifier = modifier
    ) {
        Text(
            stringResource(R.string.works_contents),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        reportConfig.reportTypes.forEachIndexed { i, rClass ->
            val interactionSource = remember { MutableInteractionSource() }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        AppState.CurrentDiagnosticSession.reportType = i
                        state = UiState.Editor
                    }
                    .padding(start = 16.dp)
                    .padding(vertical = 12.dp)
            ) {
                Icon(
                    if (AppState.CurrentDiagnosticSession.reportType == i)
                        painterResource(R.drawable.ic_radio_button_checked)
                    else
                        painterResource(R.drawable.ic_radio_button_unchecked),
                    contentDescription = stringResource(R.string.works_contents),
                    modifier = Modifier
                        .height(18.dp)
                        .indication(interactionSource, ripple(bounded = false))
                )
                Text(
                    rClass.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .padding(horizontal = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun ReportConfiguration(
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        ReportTypesList(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
    }
}

@Composable
private fun ApplyButton(
    modifier: Modifier = Modifier
) {
    var state by LocalUiState.current
    val enabled = when (state) {
        UiState.Configuration -> UiState.canOpenEditor()
        UiState.Editor -> true
    }
    PtmTheme {
        PtmFilledButton(
            enabled = enabled,
            onClick = {
                state = when (state) {
                    UiState.Configuration -> UiState.Editor
                    UiState.Editor -> UiState.Configuration
                }
            },
            modifier = modifier
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    when (state) {
                        UiState.Configuration -> painterResource(R.drawable.ic_done_outline)
                        UiState.Editor -> rememberVectorPainter(Icons.Outlined.Settings)
                    },
                    contentDescription = stringResource(R.string.apply),
                    modifier = Modifier
                        .padding(4.dp)
                        .height(18.dp)
                )
                Text(
                    when (state) {
                        UiState.Configuration -> stringResource(R.string.apply)
                        UiState.Editor -> stringResource(R.string.edit_report_type)
                    },
                    modifier = Modifier
                        .padding(4.dp)
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun ContentPreview() {
    AppState.reportConfig.value = ReportConfig(
        listOf(
            ReportType(
                "foo0",
                "Test", listOf(
                    ReportParameter(
                        "bar0",
                        "Very long and wordy parameter name with like lots of words in it " +
                                "and that's why it doesn't even fit the screen",
                        ParameterType.Bool
                    ),
                    ReportParameter(
                        "bar1",
                        "Some other parameter",
                        ParameterType.Decimal,
                        "kV"
                    )
                )
            )
        )
    )
    AppState.CurrentDiagnosticSession.reportType = 0
    LocalUiState.current.value = UiState.Editor

    PtmTheme {
        Content()
    }
}