package space.diomentia.ehzdiagnosticstracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.api.BackendClient
import space.diomentia.ehzdiagnosticstracker.api.IngestResult
import space.diomentia.ehzdiagnosticstracker.data.AppPreferences
import space.diomentia.ehzdiagnosticstracker.data.AppPreferences.getPreference
import space.diomentia.ehzdiagnosticstracker.data.FullReport
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.ReportDatabase
import space.diomentia.ehzdiagnosticstracker.data.ReportExporter
import space.diomentia.ehzdiagnosticstracker.data.rememberReportExporter
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.FullscreenLazyColumn
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.rememberSnacker
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data object Synced : SyncStatus
    data class Failed(val reason: String) : SyncStatus
}

private sealed interface SyncBanner {
    data object AuthFailed : SyncBanner
    data class VersionMismatch(val expected: Int, val got: Int?) : SyncBanner
    data object NotConfigured : SyncBanner
}

@Composable
fun SyncScreen(
    onBack: () -> Unit,
    onGoToSettings: () -> Unit
) {
    val reports by ReportDatabase.getInstance().reportDao()
        .getAllCompletedReports()
        .collectAsStateWithLifecycle(emptyList())

    SyncContent(reports = reports, onBack = onBack, onGoToSettings = onGoToSettings)
}

@Composable
private fun SyncContent(
    reports: List<FullReport>,
    onBack: () -> Unit,
    onGoToSettings: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snacker = rememberSnacker()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm") }

    val checkedReports = remember { mutableStateSetOf<String>() }
    val statuses = remember { mutableStateMapOf<String, SyncStatus>() }
    var banner by remember { mutableStateOf<SyncBanner?>(null) }
    var isSyncing by remember { mutableStateOf(false) }

    var exportToFileRequest by remember { mutableStateOf(false) }
    if (exportToFileRequest) {
        rememberReportExporter(checkedReports.toList()).run {
            exportToFileAction(
                content = toJson(),
                mime = ReportExporter.Mime.Json,
                onCancel = { exportToFileRequest = false },
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

    fun launchSync() {
        val selected = reports.filter { it.info.id in checkedReports }
        if (selected.isEmpty() || isSyncing) return
        isSyncing = true
        banner = null
        coroutineScope.launch(Dispatchers.IO) {
            val url = context.getPreference(AppPreferences.BACKEND_URL_KEY)
            val bearer = context.getPreference(AppPreferences.BACKEND_BEARER_KEY)
            if (url.isNullOrBlank()) {
                banner = SyncBanner.NotConfigured
                isSyncing = false
                return@launch
            }
            val client = BackendClient(url, bearer ?: "")
            for (report in selected) {
                statuses[report.info.id] = SyncStatus.Syncing
                when (val res = client.ingest(report)) {
                    IngestResult.Ok -> statuses[report.info.id] = SyncStatus.Synced
                    IngestResult.AuthFailed -> {
                        statuses[report.info.id] = SyncStatus.Failed("401")
                        banner = SyncBanner.AuthFailed
                        break
                    }
                    is IngestResult.VersionMismatch -> {
                        statuses[report.info.id] = SyncStatus.Failed("v${res.got}")
                        banner = SyncBanner.VersionMismatch(res.expected, res.got)
                        break
                    }
                    is IngestResult.HttpError ->
                        statuses[report.info.id] = SyncStatus.Failed("HTTP ${res.code}")
                    is IngestResult.NetworkError ->
                        statuses[report.info.id] = SyncStatus.Failed(res.cause.message ?: "network")
                }
            }
            isSyncing = false
        }
    }

    Scaffold(
        snackbarHost = { PtmSnackbarHost(LocalSnackbarHostState.current) },
        topBar = {
            Column {
                PtmTopBar(title = { Text(stringResource(R.string.export_reports)) })
                banner?.let { SyncBannerCard(it, onGoToSettings) }
            }
        },
        bottomBar = {
            PtmBottomBar(
                navigation = {
                    ActionButton(
                        icon = rememberVectorPainter(Icons.AutoMirrored.Outlined.ArrowBack),
                        label = stringResource(R.string.back)
                    ) { onBack() }
                },
                actions = if (checkedReports.isNotEmpty() && !isSyncing) { ->
                    ActionButton(
                        icon = painterResource(R.drawable.ic_file_export),
                        label = stringResource(R.string.export_reports)
                    ) { exportToFileRequest = true }
                    ActionButton(
                        icon = rememberVectorPainter(Icons.Outlined.CloudUpload),
                        label = stringResource(R.string.sync_selected)
                    ) { launchSync() }
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
                SyncReportRow(
                    fullReport = fullReport,
                    dateFormatter = dateFormatter,
                    checked = id in checkedReports,
                    status = statuses[id] ?: SyncStatus.Idle,
                    onToggle = {
                        if (id in checkedReports) checkedReports.remove(id) else checkedReports.add(id)
                    }
                )
                HorizontalDivider(modifier = Modifier.alpha(.3f))
            }
        }
    }
}

@Composable
private fun SyncBannerCard(banner: SyncBanner, onGoToSettings: () -> Unit) {
    val text = when (banner) {
        SyncBanner.AuthFailed -> stringResource(R.string.sync_auth_error)
        is SyncBanner.VersionMismatch ->
            stringResource(R.string.sync_version_mismatch, banner.expected, banner.got?.toString() ?: "?")
        SyncBanner.NotConfigured -> stringResource(R.string.sync_not_configured)
    }
    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            if (banner == SyncBanner.AuthFailed || banner == SyncBanner.NotConfigured) {
                TextButton(onClick = onGoToSettings) {
                    Text(stringResource(R.string.settings), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }
}

@Composable
private fun SyncReportRow(
    fullReport: FullReport,
    dateFormatter: DateTimeFormatter,
    checked: Boolean,
    status: SyncStatus,
    onToggle: () -> Unit
) {
    val report = fullReport.info
    val startDateTime = report.startTime.atZone(ZoneId.systemDefault()).toLocalDateTime()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(end = 16.dp))
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
        }
        Spacer(Modifier.width(12.dp))
        SyncStatusLabel(status)
    }
}

@Composable
private fun SyncStatusLabel(status: SyncStatus) {
    val (text, color) = when (status) {
        SyncStatus.Idle -> return
        SyncStatus.Syncing -> stringResource(R.string.syncing) to MaterialTheme.colorScheme.primary
        SyncStatus.Synced -> stringResource(R.string.sync_done) to MaterialTheme.colorScheme.primary
        is SyncStatus.Failed -> status.reason to MaterialTheme.colorScheme.error
    }
    Text(text, style = MaterialTheme.typography.labelSmall, color = color)
}

@Preview
@Composable
private fun SyncScreenPreview() = PtmTheme { SyncContent(reports = emptyList(), onBack = {}, onGoToSettings = {}) }
