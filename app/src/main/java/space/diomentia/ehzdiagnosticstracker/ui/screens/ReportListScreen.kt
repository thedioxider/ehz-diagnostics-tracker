package space.diomentia.ehzdiagnosticstracker.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.data.Coordinates
import space.diomentia.ehzdiagnosticstracker.data.FullReport
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.Report
import space.diomentia.ehzdiagnosticstracker.data.ReportDatabase
import space.diomentia.ehzdiagnosticstracker.data.ReportExporter
import space.diomentia.ehzdiagnosticstracker.data.Route
import space.diomentia.ehzdiagnosticstracker.data.Section
import space.diomentia.ehzdiagnosticstracker.data.Site
import space.diomentia.ehzdiagnosticstracker.data.SiteWithFields
import space.diomentia.ehzdiagnosticstracker.data.rememberReportExporter
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.ActionCancelDialog
import space.diomentia.ehzdiagnosticstracker.ui.FullscreenLazyColumn
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.rememberSnacker
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ReportListScreen(
    routeId: String? = null,
    sectionId: String? = null,
    onOpenReport: (reportId: String) -> Unit,
    onBack: () -> Unit
) {
    val dao = ReportDatabase.getInstance().reportDao()
    val reports by when {
        sectionId != null -> dao.getCompletedReportsForSection(sectionId)
        routeId != null -> dao.getCompletedReportsForRoute(routeId)
        else -> dao.getAllCompletedReports()
    }.collectAsStateWithLifecycle(emptyList())

    ReportListContent(
        reports = reports,
        onOpenReport = onOpenReport,
        onDeleteReport = { report ->
            dao.deleteReport(report)
        },
        onBack = onBack
    )
}

@Composable
private fun ReportListContent(
    reports: List<FullReport>,
    onOpenReport: (reportId: String) -> Unit,
    onDeleteReport: (Report) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val reportDao = ReportDatabase.getInstance().reportDao()
    val snacker = rememberSnacker()

    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    var checkedReports = remember { mutableStateSetOf<String>() }

    var showDeleteReportsDialog by remember { mutableStateOf(false) }
    if (showDeleteReportsDialog) {
        ActionCancelDialog(
            onCancel = { showDeleteReportsDialog = false },
            onAction = {
                reportDao.deleteReportsByIds(checkedReports.toList())
                checkedReports.clear()
            },
            actionLabel = { Text(stringResource(R.string.delete)) },
            label = { Text(stringResource(R.string.delete_reports_confirmation)) }
        ) { }
    }

    var exportReportsRequest by remember { mutableStateOf(false) }
    if (exportReportsRequest) {
        rememberReportExporter(checkedReports.toList()).run {
            exportToFileAction(
                content = toJson(),
                mime = ReportExporter.Mime.Json,
                onCancel = { exportReportsRequest = false },
                onSuccess = {
                    checkedReports.clear()
                    snacker.showMessage(context.getString(R.string.reports_exported_successfully))
                },
                onFailure = {
                    snacker.showMessage(context.getString(R.string.something_wrong) + "...")
                }
            )
        }
    }

    Scaffold(
        snackbarHost = { PtmSnackbarHost(LocalSnackbarHostState.current) },
        topBar = {
            PtmTopBar(title = {
                Text(stringResource(R.string.report_list))
            })
        },
        bottomBar = {
            PtmBottomBar(
                navigation = {
                    if (checkedReports.isEmpty()) {
                        ActionButton(
                            icon = rememberVectorPainter(Icons.AutoMirrored.Outlined.ArrowBack),
                            label = stringResource(R.string.back)
                        ) { onBack() }
                    } else {
                        ActionButton(
                            icon = rememberVectorPainter(Icons.Outlined.Close),
                            label = stringResource(R.string.cancel)
                        ) { checkedReports.clear() }
                    }
                },
                actions = if (checkedReports.isEmpty())
                    null
                else { ->
                    ActionButton(
                        icon = rememberVectorPainter(Icons.Outlined.Delete),
                        label = stringResource(R.string.delete_reports)
                    ) { if (checkedReports.isNotEmpty()) showDeleteReportsDialog = true }
                    ActionButton(
                        icon = painterResource(R.drawable.ic_file_export),
                        label = stringResource(R.string.export_reports)
                    ) { if (checkedReports.isNotEmpty()) exportReportsRequest = true }
                }
            )
        }
    ) { innerPadding ->
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
            items(reports) { fullReport ->
                val checked = fullReport.info.id in checkedReports
                ReportListItem(
                    fullReport = fullReport,
                    dateFormatter = dateFormatter,
                    onClick = if (checkedReports.isEmpty()) { ->
                        onOpenReport(fullReport.info.id)
                    } else { ->
                        if (checked)
                            checkedReports.remove(fullReport.info.id)
                        else checkedReports.add(fullReport.info.id)
                    },
                    onLongClick = {
                        if (checked)
                            checkedReports.remove(fullReport.info.id)
                        else checkedReports.add(fullReport.info.id)
                    },
                    isChecked = if (checkedReports.isEmpty()) null else checked
                )
                HorizontalDivider(modifier = Modifier.alpha(.3f))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReportListItem(
    fullReport: FullReport,
    dateFormatter: DateTimeFormatter,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isChecked: Boolean?
) {
    val report = fullReport.info
    val siteCount = fullReport.sites.size
    val startDateTime = Instant.ofEpochSecond(report.startTime)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        isChecked?.let {
            Checkbox(
                checked = isChecked,
                onCheckedChange = null,
                modifier = Modifier.padding(end = 24.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    startDateTime.format(dateFormatter),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    fullReport.section.displayName(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.alpha(.5f)
                )
            }
            Text(
                "${stringResource(R.string.inspector)}: ${report.inspectorName}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .alpha(.7f)
            )
            Text(
                stringResource(R.string.site_count, siteCount),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .alpha(.5f)
            )
        }
    }
}

// --- Previews ---

private val previewReports = listOf(
    FullReport(
        info = Report(
            id = "r1",
            routeId = "route1",
            sectionId = "section1",
            inspectorName = "Иванов И.И.",
            startTime = 1700000000,
            isComplete = true
        ),
        route = Route("route1", "Газопровод Тест", 42),
        section = Section("section1", 0, 10, "route1"),
        sites = listOf(
            SiteWithFields(
                info = Site(
                    id = "1",
                    reportId = "r1",
                    positionKm = 0.0,
                    location = Coordinates(lat = 55.75, lng = 37.62),
                    timestamp = 1700000000,
                    locationOnly = true,
                    isDraft = false
                ),
                fields = emptyList()
            ),
            SiteWithFields(
                info = Site(
                    id = "2",
                    reportId = "r1",
                    positionKm = 1.5,
                    timestamp = 1700001000,
                    isDraft = false
                ),
                fields = emptyList()
            )
        )
    ),
    FullReport(
        info = Report(
            id = "r2",
            routeId = "route1",
            sectionId = "section1",
            inspectorName = "Петров А.В.",
            startTime = 1700100000,
            isComplete = true
        ),
        route = Route("route1", "Газопровод Тест", 42),
        section = Section("section1", 0, 10, "route1"),
        sites = emptyList()
    )
)

@Preview
@Composable
private fun ReportListPreview() {
    PtmTheme {
        ReportListContent(
            reports = previewReports,
            onOpenReport = {},
            onDeleteReport = {},
            onBack = {}
        )
    }
}

@Preview
@Composable
private fun ReportListEmptyPreview() {
    PtmTheme {
        ReportListContent(
            reports = emptyList(),
            onOpenReport = {},
            onDeleteReport = {},
            onBack = {}
        )
    }
}
