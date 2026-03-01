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
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.data.FullReport
import space.diomentia.ehzdiagnosticstracker.data.LocalSessionManager
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.Report
import space.diomentia.ehzdiagnosticstracker.data.ReportDatabase
import space.diomentia.ehzdiagnosticstracker.data.ReportField
import space.diomentia.ehzdiagnosticstracker.data.Site
import space.diomentia.ehzdiagnosticstracker.data.SiteWithFields
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.DeleteDialog
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
fun ReportScreen(
    reportId: String,
    onSiteClicked: (siteId: String, locationOnly: Boolean) -> Unit,
    onBack: () -> Unit
) {
    val dao = ReportDatabase.getInstance().reportDao()
    val fullReport by dao.getFullReport(reportId).collectAsStateWithLifecycle(null)
    val sessionManager = LocalSessionManager.current

    ReportScreenContent(
        fullReport = fullReport,
        onSiteClicked = onSiteClicked,
        onDeleteSite = { siteId -> sessionManager.removeSite(siteId) },
        onBack = onBack
    )
}

@Composable
private fun ReportScreenContent(
    fullReport: FullReport?,
    onSiteClicked: (siteId: String, locationOnly: Boolean) -> Unit,
    onDeleteSite: (siteId: String) -> Unit = {},
    onBack: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    val context = LocalContext.current
    val snacker = rememberSnacker()
    var siteToDelete by remember { mutableStateOf<SiteWithFields?>(null) }

    siteToDelete?.let { swf ->
        val position = swf.site.positionKm?.let { "%.2f км".format(it) } ?: "—"
        DeleteDialog(
            onDelete = {
                onDeleteSite(swf.site.id)
                siteToDelete = null
            },
            label = { Text(stringResource(R.string.question_delete_site)) },
            onCancel = { siteToDelete = null },
            name = position,
            details = if (swf.site.locationOnly) stringResource(R.string.location_only)
                else stringResource(R.string.field_count, swf.fields.size)
        )
    }

    Scaffold(
        snackbarHost = { PtmSnackbarHost(LocalSnackbarHostState.current) },
        topBar = {
            PtmTopBar(title = {
                Text(stringResource(R.string.view_report))
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
                },
                actions = {
                    // TODO: export report button
                }
            )
        }
    ) { innerPadding ->
        val confirmedSites = fullReport?.sites
            ?.filter { !it.site.isDraft }
            ?.sortedBy { it.site.timestamp }
            ?: emptyList()

        if (confirmedSites.isEmpty()) {
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

        val firstSite = confirmedSites.firstOrNull()
        val isZeroKm = firstSite?.site?.locationOnly == true

        FullscreenLazyColumn(innerPadding) {
            items(confirmedSites) { siteWithFields ->
                val isDeletable = !(isZeroKm && siteWithFields.site.id == firstSite.site.id)
                SiteItem(
                    siteWithFields = siteWithFields,
                    dateFormatter = dateFormatter,
                    onClick = {
                        onSiteClicked(
                            siteWithFields.site.id,
                            siteWithFields.site.locationOnly
                        )
                    },
                    onLongClick = if (isDeletable) {{ siteToDelete = siteWithFields }}
                        else {{ snacker.showMessage(context.getString(R.string.cannot_delete_zero_km)) }}
                )
                HorizontalDivider(modifier = Modifier.alpha(.3f))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SiteItem(
    siteWithFields: SiteWithFields,
    dateFormatter: DateTimeFormatter,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val site = siteWithFields.site
    val dateTime = Instant.ofEpochSecond(site.timestamp)
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
        Icon(
            if (site.locationOnly) Icons.Outlined.LocationOn
            else Icons.Outlined.Search,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 16.dp)
                .alpha(.7f)
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    site.positionKm?.let { "%.2f км".format(it) }
                        ?: stringResource(R.string.not_set),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    dateTime.format(dateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.alpha(.5f)
                )
            }
            Text(
                if (site.locationOnly) stringResource(R.string.location_only)
                else stringResource(R.string.field_count, siteWithFields.fields.size),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .alpha(.7f)
            )
        }
    }
}

// --- Previews ---

private val previewSites = listOf(
    SiteWithFields(
        site = Site(
            id = "1",
            reportId = "r1",
            positionKm = 0.0,
            latitude = 55.75,
            longitude = 37.62,
            timestamp = 1700000000,
            locationOnly = true,
            isDraft = false
        ),
        fields = emptyList()
    ),
    SiteWithFields(
        site = Site(
            id = "2",
            reportId = "r1",
            positionKm = 1.234,
            latitude = 55.76,
            longitude = 37.63,
            timestamp = 1700001000,
            validationId = "QR-123",
            isDraft = false
        ),
        fields = listOf(
            ReportField("2", "voltage", "Напряжение", "decimal", "В"),
            ReportField("2", "current", "Ток", "decimal", "А")
        )
    ),
    SiteWithFields(
        site = Site(
            id = "3",
            reportId = "r1",
            positionKm = 2.567,
            latitude = 55.77,
            longitude = 37.64,
            timestamp = 1700002000,
            isDraft = false
        ),
        fields = listOf(
            ReportField("3", "potential", "Потенциал", "decimal", "мВ")
        )
    )
)

private val previewReport = FullReport(
    report = Report(
        id = "r1",
        routeId = "route1",
        sectionId = "section1",
        inspectorName = "Иванов И.И.",
        startTime = 1700000000
    ),
    sites = previewSites
)

@Preview
@Composable
private fun ReportScreenPreview() {
    PtmTheme {
        ReportScreenContent(
            fullReport = previewReport,
            onSiteClicked = { _, _ -> },
            onDeleteSite = {},
            onBack = {}
        )
    }
}

@Preview
@Composable
private fun ReportScreenEmptyPreview() {
    PtmTheme {
        ReportScreenContent(
            fullReport = FullReport(
                report = previewReport.report,
                sites = emptyList()
            ),
            onSiteClicked = { _, _ -> },
            onDeleteSite = {},
            onBack = {}
        )
    }
}