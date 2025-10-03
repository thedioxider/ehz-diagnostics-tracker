package space.diomentia.ehzdiagnosticstracker

import android.app.Activity.RESULT_CANCELED
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.Coordinates
import space.diomentia.ehzdiagnosticstracker.data.DiagnosticReport
import space.diomentia.ehzdiagnosticstracker.data.DiagnosticReportWithData
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.ParameterType
import space.diomentia.ehzdiagnosticstracker.data.ReportDataEntry
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.BorderedDialogContainer
import space.diomentia.ehzdiagnosticstracker.ui.FullscreenLazyColumn
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.setupEdgeToEdge
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ReportHistoryActivity : ComponentActivity() {
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
private fun Content(
    reports: MutableState<List<DiagnosticReportWithData>> = mutableStateOf(emptyList())
) {
    val activity = LocalActivity.current

    PtmTheme {
        Scaffold(
            snackbarHost = { PtmSnackbarHost(LocalSnackbarHostState.current) },
            topBar = {
                PtmTopBar(title = {
                    Text(stringResource(R.string.diagnostics_history))
                })
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
            }
        ) { innerPadding ->
            var reportShowDetails by remember { mutableStateOf<DiagnosticReportWithData?>(null) }
            reportShowDetails?.let {
                ReportDetailsDialog(it) { reportShowDetails = null }
            }

            var reportDelete by remember { mutableStateOf<DiagnosticReportWithData?>(null) }
            reportDelete?.let {
                ReportDeleteDialog(
                    onDismissRequest = { reportDelete = null },
                    onAcceptRequest = {
                        AppState.reportDb?.diagnosticReportDao()?.run {
                            deleteData(*it.data.toTypedArray())
                            deleteReport(it.report)
                            reportDelete = null
                        }
                    }
                )
            }

            val history by AppState.CurrentDiagnosticSession.diagnosticArea?.let {
                AppState.reportDb
                    ?.diagnosticAreaDao()
                    ?.getAreaPoints(it)
                    ?.collectAsStateWithLifecycle(emptyList())
            } ?: remember { mutableStateOf(emptyList()) }
            history.forEach { point ->
                val reports by AppState.reportDb
                    ?.diagnosticAreaDao()
                    ?.getPointReports(point.point.id)
                    ?.collectAsStateWithLifecycle(emptyList())
                    ?: reports
                if (reports.isEmpty()) {
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
                    items(reports) { report ->
                        ReportItem(
                            report,
                            onLongClick = { reportDelete = report }
                        ) {
                            reportShowDetails = report
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReportItem(
    report: DiagnosticReportWithData,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit
) {
    val dateTime = LocalDateTime
        .ofEpochSecond(
            report.report.timestamp, 0, ZoneOffset.systemDefault().rules.getOffset(
                LocalDateTime.now()
            )
        )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
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
                "${dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "${stringResource(R.string.point)} ${report.report.diagnosticPoint}",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .alpha(.7f)
            )
        }
    }
}


@Composable
private fun ReportDetailsDialog(
    report: DiagnosticReportWithData,
    onDismissRequest: () -> Unit
) {
    val parameters = AppState.reportConfig.value
        ?.diagnosticTypes
        ?.find { it.id == report.report.reportType }
        ?.parameters
        ?.asSequence()
        ?.map { it.id to it }
        ?.toMap()
    val reportType = AppState.reportConfig.value
        ?.diagnosticTypes
        ?.find { it.id == report.report.reportType }
        ?.name

    @Composable
    fun DataEntryItem(entry: ReportDataEntry) {
        val parameter = parameters?.get(entry.characteristic)
        val value = when (parameter?.type) {
            ParameterType.Bool -> when (entry.value) {
                "true" -> stringResource(R.string.is_true)
                "false" -> stringResource(R.string.is_false)
                else -> stringResource(R.string.unknown)
            }

            ParameterType.Integer, ParameterType.Decimal,
            ParameterType.Photo, ParameterType.Text, null -> entry.value
        } + (parameter?.unit?.let { " $it" } ?: "")
        Column(
            modifier = Modifier
                .padding(vertical = 16.dp)
        ) {
            Text(
                parameter?.name ?: entry.characteristic,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                value,
                modifier = Modifier
                    .padding(top = 8.dp, start = 24.dp)
            )
        }
    }

    Dialog(onDismissRequest) {
        BorderedDialogContainer {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
            ) {
                LazyColumn(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    reportType?.let {
                        item {
                            Text(
                                it,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                    item {
                        Text(
                            "${stringResource(R.string.report_by)} ${report.report.inspector}",
                            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
                        )
                    }
                    items(report.data) { entry ->
                        DataEntryItem(entry)
                    }
                }
                PtmFilledButton(
                    onDismissRequest,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}

@Composable
private fun ReportDeleteDialog(
    onDismissRequest: () -> Unit,
    onAcceptRequest: () -> Unit
) {
    Dialog(onDismissRequest) {
        BorderedDialogContainer {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    stringResource(R.string.question_delete_report),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                PtmFilledButton(
                    onAcceptRequest,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(stringResource(R.string.remove))
                }
                PtmFilledButton(
                    onDismissRequest,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}


private val previewReports = listOf(
    DiagnosticReportWithData(
        DiagnosticReport(
            "8539848",
            1758638351,
            "2",
            "85c906f60dff0f735be9ac84411d71e867e7104f",
            Coordinates(25.94825038, 220.4249238),
            "Me Me",
            5
        ),
        listOf()
    )
)

@Preview
@Composable
private fun ContentPreview() {
    PtmTheme { Content(remember { mutableStateOf(previewReports) }) }
}

@Preview
@Composable
private fun ReportViewDialogPreview() {
    PtmTheme {
        ReportDetailsDialog(previewReports.first()) {}
    }
}

@Preview
@Composable
private fun ReportDeleteDialogPreview() {
    PtmTheme {
        ReportDeleteDialog({}) {}
    }
}