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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Home
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
import space.diomentia.ehzdiagnosticstracker.data.Route
import space.diomentia.ehzdiagnosticstracker.data.Section
import space.diomentia.ehzdiagnosticstracker.data.Site
import space.diomentia.ehzdiagnosticstracker.data.SiteWithFields
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.ActionCancelDialog
import space.diomentia.ehzdiagnosticstracker.ui.FullscreenLazyColumn
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ReportListScreen(
    routeId: String? = null,
    sectionId: String? = null,
    onOpenReport: (reportId: String) -> Unit,
    onBack: () -> Unit,
    homeNav: Boolean = false
) {
    val dao = ReportDatabase.getInstance().reportDao()
    val reports by when {
        sectionId != null -> dao.getCompletedReportsForSection(sectionId)
        routeId != null -> dao.getCompletedReportsForRoute(routeId)
        else -> dao.getAllCompletedReports()
    }.collectAsStateWithLifecycle(emptyList())

    ReportListContent(
        reports = reports,
        title = stringResource(R.string.report_list),
        onOpenReport = onOpenReport,
        onDeleteReports = { dao.deleteReportsByIds(it) },
        onBack = onBack,
        homeNav = homeNav
    )
}

@Composable
internal fun ReportListContent(
    reports: List<FullReport>,
    title: String,
    onOpenReport: ((reportId: String) -> Unit)?,
    onDeleteReports: ((List<String>) -> Unit)?,
    onBack: () -> Unit,
    homeNav: Boolean,
    topBanner: (@Composable () -> Unit)? = null,
    syncStatusContent: (@Composable (reportId: String) -> Unit)? = null,
    extraActions: (@Composable (checkedIds: Set<String>) -> Unit)? = null
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    val checkedReports = remember { mutableStateSetOf<String>() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        ActionCancelDialog(
            onCancel = { showDeleteDialog = false },
            onAction = {
                onDeleteReports?.invoke(checkedReports.toList())
                checkedReports.clear()
            },
            actionLabel = { Text(stringResource(R.string.delete)) },
            label = { Text(stringResource(R.string.delete_reports_confirmation)) }
        ) { }
    }

    Scaffold(
        snackbarHost = { PtmSnackbarHost(LocalSnackbarHostState.current) },
        topBar = {
            Column {
                PtmTopBar(title = { Text(title) })
                topBanner?.invoke()
            }
        },
        bottomBar = {
            PtmBottomBar(
                navigation = {
                    ActionButton(
                        icon = if (homeNav) rememberVectorPainter(Icons.Outlined.Home)
                               else painterResource(R.drawable.ic_list_alt),
                        label = stringResource(if (homeNav) R.string.home else R.string.back)
                    ) { onBack() }
                },
                actions = if (checkedReports.isNotEmpty()) { ->
                    extraActions?.invoke(checkedReports)
                    if (onDeleteReports != null) {
                        ActionButton(
                            icon = rememberVectorPainter(Icons.Outlined.Delete),
                            label = stringResource(R.string.delete_reports)
                        ) { showDeleteDialog = true }
                    }
                } else null
            )
        }
    ) { innerPadding ->
        if (reports.isEmpty()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    stringResource(R.string.entries_not_found),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.alpha(.3f)
                )
            }
            return@Scaffold
        }
        FullscreenLazyColumn(innerPadding) {
            items(reports, key = { it.info.id }) { fullReport ->
                val id = fullReport.info.id
                val checked = id in checkedReports
                ReportListItem(
                    fullReport = fullReport,
                    dateFormatter = dateFormatter,
                    onClick = if (checkedReports.isEmpty() && onOpenReport != null) { ->
                        onOpenReport(id)
                    } else { ->
                        if (checked) checkedReports.remove(id) else checkedReports.add(id)
                    },
                    onLongClick = {
                        if (checked) checkedReports.remove(id) else checkedReports.add(id)
                    },
                    isChecked = if (checkedReports.isEmpty()) null else checked,
                    trailingContent = syncStatusContent?.let { { it(id) } }
                )
                HorizontalDivider(modifier = Modifier.alpha(.3f))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ReportListItem(
    fullReport: FullReport,
    dateFormatter: DateTimeFormatter,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isChecked: Boolean?,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val report = fullReport.info
    val startDateTime = report.startTime.atZone(ZoneId.systemDefault()).toLocalDateTime()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        isChecked?.let {
            Checkbox(checked = isChecked, onCheckedChange = null, modifier = Modifier.padding(end = 24.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(startDateTime.format(dateFormatter), style = MaterialTheme.typography.titleMedium)
                Text(
                    fullReport.section.displayName(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.alpha(.5f)
                )
            }
            Text(
                "${stringResource(R.string.inspector)}: ${report.inspectorName}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp).alpha(.7f)
            )
            Text(
                stringResource(R.string.site_count, fullReport.sites.size),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp).alpha(.5f)
            )
        }
        trailingContent?.invoke()
    }
}

// --- Previews ---

private val previewReports = listOf(
    FullReport(
        info = Report(
            id = "r1", routeId = "route1", sectionId = "section1",
            inspectorName = "Иванов И.И.",
            startTime = Instant.ofEpochSecond(1700000000), isComplete = true
        ),
        route = Route("route1", "Газопровод Тест", 42),
        section = Section("section1", 0, 10, "route1"),
        sites = listOf(
            SiteWithFields(
                info = Site(
                    id = "1", reportId = "r1", positionKm = 0.0,
                    location = Coordinates(lat = 55.75, lng = 37.62),
                    timestamp = Instant.ofEpochSecond(1700000000),
                    locationOnly = true, isDraft = false
                ),
                fields = emptyList()
            )
        )
    ),
    FullReport(
        info = Report(
            id = "r2", routeId = "route1", sectionId = "section1",
            inspectorName = "Петров А.В.",
            startTime = Instant.ofEpochSecond(1700100000), isComplete = true
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
            title = "История отчётов",
            onOpenReport = {},
            onDeleteReports = {},
            onBack = {},
            homeNav = true
        )
    }
}

@Preview
@Composable
private fun ReportListEmptyPreview() {
    PtmTheme {
        ReportListContent(
            reports = emptyList(),
            title = "История отчётов",
            onOpenReport = {},
            onDeleteReports = {},
            onBack = {},
            homeNav = false
        )
    }
}
