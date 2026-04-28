package space.diomentia.ehzdiagnosticstracker.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import space.diomentia.ehzdiagnosticstracker.data.ReportDatabase
import space.diomentia.ehzdiagnosticstracker.data.ReportExporter
import space.diomentia.ehzdiagnosticstracker.data.rememberReportExporter
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.rememberSnacker
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme

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
fun SyncScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snacker = rememberSnacker()

    val reports by ReportDatabase.getInstance().reportDao()
        .getAllCompletedReports()
        .collectAsStateWithLifecycle(emptyList())

    val syncStatuses = remember { mutableStateMapOf<String, SyncStatus>() }
    var syncBanner by remember { mutableStateOf<SyncBanner?>(null) }
    var isSyncing by remember { mutableStateOf(false) }

    var exportIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var exportToFileRequest by remember { mutableStateOf(false) }
    if (exportToFileRequest) {
        rememberReportExporter(exportIds).run {
            exportToFileAction(
                content = toJson(),
                mime = ReportExporter.Mime.Json,
                onCancel = { exportToFileRequest = false },
                onSuccess = { snacker.showMessage(context.getString(R.string.reports_exported_successfully)) },
                onFailure = { snacker.showMessage(context.getString(R.string.something_wrong) + "...") }
            )
        }
    }

    fun launchSync(checkedIds: Set<String>) {
        val selected = reports.filter { it.info.id in checkedIds }
        if (selected.isEmpty() || isSyncing) return
        isSyncing = true
        syncBanner = null
        coroutineScope.launch(Dispatchers.IO) {
            val url = context.getPreference(AppPreferences.BACKEND_URL_KEY)
            val bearer = context.getPreference(AppPreferences.BACKEND_BEARER_KEY)
            if (url.isNullOrBlank()) {
                syncBanner = SyncBanner.NotConfigured
                isSyncing = false
                return@launch
            }
            val client = BackendClient(url, bearer ?: "")
            for (report in selected) {
                syncStatuses[report.info.id] = SyncStatus.Syncing
                when (val res = client.ingest(report)) {
                    IngestResult.Ok -> syncStatuses[report.info.id] = SyncStatus.Synced
                    IngestResult.AuthFailed -> {
                        syncStatuses[report.info.id] = SyncStatus.Failed("401")
                        syncBanner = SyncBanner.AuthFailed
                        break
                    }
                    is IngestResult.VersionMismatch -> {
                        syncStatuses[report.info.id] = SyncStatus.Failed("v${res.got}")
                        syncBanner = SyncBanner.VersionMismatch(res.expected, res.got)
                        break
                    }
                    is IngestResult.HttpError ->
                        syncStatuses[report.info.id] = SyncStatus.Failed("HTTP ${res.code}")
                    is IngestResult.NetworkError ->
                        syncStatuses[report.info.id] = SyncStatus.Failed(res.cause.message ?: "network")
                }
            }
            isSyncing = false
        }
    }

    ReportListContent(
        reports = reports,
        title = stringResource(R.string.export_reports),
        onOpenReport = null,
        onDeleteReports = null,
        onBack = onBack,
        homeNav = true,
        alwaysShowCheckboxes = true,
        topBanner = syncBanner?.let { banner -> { SyncBannerCard(banner, onBack) } },
        syncStatusContent = { id ->
            SyncStatusLabel(syncStatuses[id] ?: SyncStatus.Idle)
        },
        extraActions = { checkedIds ->
            if (!isSyncing) {
                ActionButton(
                    icon = painterResource(R.drawable.ic_file_export),
                    label = stringResource(R.string.export_reports)
                ) {
                    exportIds = checkedIds.toList()
                    exportToFileRequest = true
                }
                ActionButton(
                    icon = rememberVectorPainter(Icons.Outlined.CloudUpload),
                    label = stringResource(R.string.sync_selected)
                ) { launchSync(checkedIds) }
            }
        }
    )
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
private fun SyncStatusLabel(status: SyncStatus) {
    val (text, color) = when (status) {
        SyncStatus.Idle -> return
        SyncStatus.Syncing -> stringResource(R.string.syncing) to MaterialTheme.colorScheme.primary
        SyncStatus.Synced -> stringResource(R.string.sync_done) to MaterialTheme.colorScheme.primary
        is SyncStatus.Failed -> status.reason to MaterialTheme.colorScheme.error
    }
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier.padding(start = 12.dp)
    )
}

@Preview
@Composable
private fun SyncScreenPreview() = PtmTheme {
    ReportListContent(
        reports = emptyList(),
        title = "Экспорт отчётов",
        onOpenReport = null,
        onDeleteReports = null,
        onBack = {},
        homeNav = true
    )
}
