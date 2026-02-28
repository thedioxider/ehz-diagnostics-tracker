package space.diomentia.ehzdiagnosticstracker.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.Coordinates
import space.diomentia.ehzdiagnosticstracker.data.Section
import space.diomentia.ehzdiagnosticstracker.data.Route
import space.diomentia.ehzdiagnosticstracker.data.DiagnosticReport
import space.diomentia.ehzdiagnosticstracker.data.DiagnosticReportWithData
import space.diomentia.ehzdiagnosticstracker.data.DiagnosticSession
import space.diomentia.ehzdiagnosticstracker.data.LocalCoroutineScope
import space.diomentia.ehzdiagnosticstracker.data.LocalDiagnosticSession
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.FieldType
import space.diomentia.ehzdiagnosticstracker.data.ReportDataEntry
import space.diomentia.ehzdiagnosticstracker.data.ReportDatabase
import space.diomentia.ehzdiagnosticstracker.data.ReportExporter
import space.diomentia.ehzdiagnosticstracker.data.ReportFieldValue
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.BorderedDialogContainer
import space.diomentia.ehzdiagnosticstracker.ui.DeleteDialog
import space.diomentia.ehzdiagnosticstracker.ui.FullscreenLazyColumn
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.clickableWithLabel
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun ReportHistoryScreen(
    diagnosticSession: DiagnosticSession,
    onEditReport: (DiagnosticReport) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    CompositionLocalProvider(
        LocalDiagnosticSession provides remember { mutableStateOf(diagnosticSession) }
    ) {
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
                                onBack()
                            }
                        }
                    )
                }
            ) { innerPadding ->
                var reportShowDetails by remember { mutableStateOf<DiagnosticReportWithData?>(null) }
                reportShowDetails?.let {
                    ReportDetailsDialog(
                        it,
                        onEditReport = onEditReport,
                        onDismissRequest = { reportShowDetails = null }
                    )
                }

                var reportDelete by remember { mutableStateOf<DiagnosticReportWithData?>(null) }
                reportDelete?.let {
                    ReportDeleteDialog(
                        report = it,
                        onDismissRequest = { reportDelete = null },
                        onDelete = {
                            ReportDatabase.getInstance(context).diagnosticReportDao()
                                .run {
                                    deleteData(*it.data.toTypedArray())
                                    deleteReport(it.report)
                                    reportDelete = null
                                }
                        }
                    )
                }

                val history by ReportDatabase.getInstance()
                    .routeDao()
                    .getSectionPoints(diagnosticSession.section.id)
                    .collectAsStateWithLifecycle(emptyList())
                val reports = history.flatMap { point ->
                    ReportDatabase.getInstance()
                        .routeDao()
                        .getPointReports(point.point.id)
                        .collectAsStateWithLifecycle(emptyList())
                        .value
                }
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
    val context = LocalContext.current
    val coroutineScope = LocalCoroutineScope.current
    val snackbarHostState = LocalSnackbarHostState.current
    val dateTime = LocalDateTime
        .ofEpochSecond(
            report.report.timestamp, 0, ZoneOffset.systemDefault().rules.getOffset(
                LocalDateTime.now()
            )
        )
    val reportExporter = ReportExporter(
        reportId = report.report.id,
        db = ReportDatabase.getInstance(),
        reportConfig = AppState.reportConfig.value
    )
    val exportFilename = reportExporter.defaultFilename()
    val diagnosticPoint by ReportDatabase.getInstance()
        .routeDao()
        .getPointById(report.report.diagnosticPoint)
        .collectAsStateWithLifecycle(null)
    val exportFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            coroutineScope?.launch {
                val result = reportExporter.exportReportToUri(uri, context.contentResolver)
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    context.getString(
                        if (result) {
                            R.string.report_exported
                        } else {
                            R.string.something_wrong
                        }
                    )
                )
            }
        }
    }

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
                "${stringResource(R.string.point)} ${diagnosticPoint?.point?.inventoryId}",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .alpha(.7f)
            )
        }
        Icon(
            painterResource(R.drawable.ic_file_export),
            contentDescription = stringResource(R.string.export_report),
            modifier = Modifier
                .clickableWithLabel(
                    label = stringResource(R.string.export_report),
                    enabled = exportFilename != null
                ) {
                    exportFileLauncher.launch(exportFilename!!)
                }
                .fillMaxHeight()
                .aspectRatio(1f)
                .padding(end = 12.dp)
        )
    }
}


@Composable
private fun ReportDetailsDialog(
    report: DiagnosticReportWithData,
    onEditReport: (DiagnosticReport) -> Unit,
    onDismissRequest: () -> Unit
) {
    val parameters = AppState.reportConfig.value
        ?.templates
        ?.find { it.id == report.report.reportType }
        ?.specIds
        ?.asSequence()
        ?.map { it.id to it }
        ?.toMap()
    val reportType = AppState.reportConfig.value
        ?.templates
        ?.find { it.id == report.report.reportType }
        ?.name

    @Composable
    fun DataEntryItem(entry: ReportDataEntry) {
        val parameter = parameters?.get(entry.characteristic)
        val value = when (parameter?.type) {
            FieldType.Bool -> when (ReportFieldValue.Bool.fromString(entry.value)) {
                true -> stringResource(R.string.is_true)
                false -> stringResource(R.string.is_false)
                else -> stringResource(R.string.unknown)
            }

            FieldType.Integer, FieldType.Decimal,
            FieldType.Photo, FieldType.Text, FieldType.Raw, null -> entry.value
        } + (parameter?.unit?.let { " $it" } ?: "")
        Column(
            modifier = Modifier
                .padding(vertical = 16.dp)
        ) {
            Text(
                parameter?.name ?: entry.characteristic,
                style = MaterialTheme.typography.titleSmall
            )
            when (entry.datatype?.let { FieldType.parse(it) }) {
                FieldType.Photo -> Text(
                    "[ ${stringResource(R.string.image)} ]",
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier
                        .padding(top = 8.dp, start = 24.dp)
                )

                else -> Text(
                    value,
                    modifier = Modifier
                        .padding(top = 8.dp, start = 24.dp)
                )
            }
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    PtmFilledButton(
                        {
                            onEditReport(report.report)
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                    ) {
                        Text(stringResource(R.string.edit))
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
}

@Composable
private fun ReportDeleteDialog(
    report: DiagnosticReportWithData,
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit
) {
    val dateTime = LocalDateTime.ofEpochSecond(
        report.report.timestamp, 0,
        ZoneOffset.systemDefault().rules.getOffset(LocalDateTime.now())
    )

    DeleteDialog(
        onDelete = onDelete,
        label = { Text("${stringResource(R.string.question_delete_report)}?") },
        onCancel = onDismissRequest,
        name = dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
        details = report.report.inspector
    )
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
            null,
            5
        ),
        listOf()
    )
)

@Preview
@Composable
private fun ReportHistoryScreenPreview() = ReportHistoryScreen(
    DiagnosticSession(
        route = Route("", "", "Test Object"),
        section = Section("", "Test Area", "", "")
    ), {}, {})

@Preview
@Composable
private fun ReportViewDialogPreview() {
    PtmTheme {
        ReportDetailsDialog(previewReports.first(), {}) {}
    }
}

@Preview
@Composable
private fun ReportDeleteDialogPreview() {
    PtmTheme {
        ReportDeleteDialog(
            report = DiagnosticReportWithData(
                report = DiagnosticReport(
                    diagnosticPoint = "P-001",
                    timestamp = 0,
                    diagnosticsType = "",
                    reportType = "",
                    location = Coordinates(0.0, 0.0),
                    inspector = "Иванов И."
                ),
                data = emptyList()
            ),
            onDismissRequest = {},
            onDelete = {}
        )
    }
}