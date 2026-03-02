package space.diomentia.ehzdiagnosticstracker.ui.screens

import android.content.Intent
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.data.FullReport
import space.diomentia.ehzdiagnosticstracker.data.LocalSessionManager
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.Report
import space.diomentia.ehzdiagnosticstracker.data.Route
import space.diomentia.ehzdiagnosticstracker.data.Section
import space.diomentia.ehzdiagnosticstracker.data.Site
import space.diomentia.ehzdiagnosticstracker.data.SiteWithFields
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.ActionCancelDialog
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmOutlinedButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.clickableWithLabel
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SessionHubScreen(
    onNewMeasurementSite: () -> Unit,
    onNewLocationSite: () -> Unit,
    onEditDraftSite: (siteId: String) -> Unit,
    onViewReport: (reportId: String) -> Unit,
    onSessionCompleted: () -> Unit,
    onAuthorize: () -> Unit,
    onChooseArea: () -> Unit
) {
    val sessionManager = LocalSessionManager.current
    val fullReport by sessionManager.fullReport.collectAsStateWithLifecycle(null)
    val reportId = sessionManager.activeReportId

    SessionHubContent(
        fullReport = fullReport,
        onNewMeasurementSite = onNewMeasurementSite,
        onNewLocationSite = onNewLocationSite,
        onEditDraftSite = onEditDraftSite,
        onDeleteDraft = { siteId -> sessionManager.removeSite(siteId) },
        onViewReport = { reportId?.let { onViewReport(it) } },
        onCompleteSession = {
            sessionManager.completeReport()
            onSessionCompleted()
        },
        onAuthorize = onAuthorize,
        onChooseArea = onChooseArea
    )
}

@Composable
private fun SessionHubContent(
    fullReport: FullReport?,
    onNewMeasurementSite: () -> Unit,
    onNewLocationSite: () -> Unit,
    onEditDraftSite: (siteId: String) -> Unit,
    onDeleteDraft: (siteId: String) -> Unit,
    onViewReport: () -> Unit,
    onCompleteSession: () -> Unit,
    onAuthorize: () -> Unit,
    onChooseArea: () -> Unit
) {
    val context = LocalContext.current
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    var showCompleteDialog by remember { mutableStateOf(false) }
    var showDraftDialog by remember { mutableStateOf(false) }
    var showDeleteDraftConfirm by remember { mutableStateOf(false) }

    val report = fullReport?.report
    val route = fullReport?.route
    val section = fullReport?.section
    val confirmedSites = fullReport?.sites
        ?.filter { !it.site.isDraft }
        ?: emptyList()
    val draftSite = fullReport?.sites?.find { it.site.isDraft }
    val lastSite = confirmedSites.maxByOrNull { it.site.timestamp }

    if (showCompleteDialog) {
        ActionCancelDialog(
            onAction = {
                showCompleteDialog = false
                onCompleteSession()
            },
            actionLabel = { Text(stringResource(R.string.complete_session)) },
            onCancel = { showCompleteDialog = false },
            label = { Text(stringResource(R.string.confirm_complete_session)) }
        ) { }
    }

    if (showDraftDialog && draftSite != null) {
        // TODO: instead of long text buttons, add outlined buttons with icons and descriptions under them
        ActionCancelDialog(
            onAction = {
                showDraftDialog = false
                onEditDraftSite(draftSite.site.id)
            },
            actionLabel = { Text(stringResource(R.string.continue_draft)) },
            onCancel = { showDraftDialog = false },
            onSecondaryAction = {
                showDraftDialog = false
                showDeleteDraftConfirm = true
            },
            secondaryActionLabel = { Text(stringResource(R.string.delete_draft_and_create)) },
            label = { Text(stringResource(R.string.unsaved_draft_exists)) }
        ) { }
    }

    if (showDeleteDraftConfirm && draftSite != null) {
        ActionCancelDialog(
            onAction = {
                showDeleteDraftConfirm = false
                onDeleteDraft(draftSite.site.id)
                onNewMeasurementSite()
            },
            actionLabel = { Text(stringResource(R.string.delete)) },
            onCancel = { showDeleteDraftConfirm = false },
            label = { Text(stringResource(R.string.confirm_delete_draft)) }
        ) { }
    }

    Scaffold(
        snackbarHost = { PtmSnackbarHost(LocalSnackbarHostState.current) },
        topBar = {
            PtmTopBar(title = {
                Text(stringResource(R.string.session_info))
            })
        },
        bottomBar = {
            PtmBottomBar(
                navigation = {
                    ActionButton(
                        icon = painterResource(R.drawable.ic_person),
                        label = stringResource(R.string.change_account)
                    ) {
                        onAuthorize()
                    }
                    ActionButton(
                        icon = rememberVectorPainter(Icons.Outlined.LocationOn)
                    ) {
                        onChooseArea()
                    }
                },
                actions = {
                    ActionButton(
                        icon = rememberVectorPainter(Icons.Outlined.Check),
                        label = stringResource(R.string.complete_session)
                    ) {
                        showCompleteDialog = true
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Session info header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                route?.let {
                    Text(
                        it.name,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                section?.let {
                    Text(
                        "${stringResource(R.string.area)} ${it.displayName()}",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                report?.let {
                    Text(
                        "${stringResource(R.string.inspector)}: ${it.inspectorName}",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    val startDateTime = Instant.ofEpochSecond(it.startTime)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
                    Text(
                        "${stringResource(R.string.start_time)}: ${
                            startDateTime.format(
                                dateFormatter
                            )
                        }",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .alpha(.7f)
                    )
                }
                Text(
                    stringResource(R.string.site_count, confirmedSites.size),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .alpha(.7f)
                )
            }

            // Primary action — new measurement site
            Column {
                HubActionButton(
                    description = stringResource(R.string.new_measurement_site),
                    icon = painterResource(R.drawable.ic_edit_location),
                    buttonHeight = 140.dp,
                    textStyle = MaterialTheme.typography.titleMedium,
                    onClick = {
                        if (draftSite != null) showDraftDialog = true
                        else onNewMeasurementSite()
                    }
                )
                if (draftSite != null) {
                    Text(
                        stringResource(R.string.unsaved_draft_exists),
                        style = MaterialTheme.typography.titleSmall,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .alpha(.7f)
                    )
                }
            }

            // Secondary actions
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                HubActionButton(
                    description = stringResource(R.string.new_location_marker),
                    shortDescription = stringResource(R.string.new_location_marker_short),
                    icon = rememberVectorPainter(Icons.Outlined.LocationOn),
                    modifier = Modifier.weight(1f),
                    onClick = onNewLocationSite
                )
                HubActionButton(
                    description = stringResource(R.string.view_report),
                    shortDescription = stringResource(R.string.view_report_short),
                    icon = painterResource(R.drawable.ic_history),
                    enabled = confirmedSites.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    onClick = onViewReport
                )
                HubActionButton(
                    description = stringResource(R.string.open_last_site_on_map),
                    shortDescription = stringResource(R.string.open_last_site_on_map_short),
                    icon = painterResource(R.drawable.ic_explore),
                    enabled = lastSite?.site?.latitude != null,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        lastSite?.site?.let { site ->
                            if (site.latitude != null && site.longitude != null) {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        "geo:0,0?q=${site.latitude},${site.longitude}".toUri()
                                    )
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun HubActionButton(
    description: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    shortDescription: String = description,
    enabled: Boolean = true,
    buttonHeight: Dp = 80.dp,
    textStyle: TextStyle = MaterialTheme.typography.labelMedium,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(horizontal = 6.dp)
            .clickableWithLabel(
                enabled = enabled,
                label = description,
                interactionSource = interactionSource,
                indication = null,
                action = onClick
            )
    ) {
        PtmOutlinedButton(
            enabled = enabled,
            roundedCorners = true,
            modifier = Modifier
                .height(buttonHeight)
                .aspectRatio(1f)
                .indication(interactionSource, ripple())
        ) {
            Icon(
                icon,
                contentDescription = description,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            shortDescription,
            textAlign = TextAlign.Center,
            style = textStyle,
            modifier = Modifier.padding(8.dp)
        )
    }
}

// --- Previews ---

private val previewReport = FullReport(
    report = Report(
        id = "r1",
        routeId = "route1",
        sectionId = "section1",
        inspectorName = "Иванов И.И.",
        startTime = 1700000000
    ),
    route = Route("route1", "Газопровод Тест", 42),
    section = Section("section1", 0, 10, "route1"),
    sites = listOf(
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
                positionKm = 1.5,
                latitude = 55.76,
                longitude = 37.63,
                timestamp = 1700001000,
                isDraft = false
            ),
            fields = emptyList()
        )
    )
)

@Preview
@Composable
private fun SessionHubPreview() {
    PtmTheme {
        SessionHubContent(
            fullReport = previewReport,
            onNewMeasurementSite = {},
            onNewLocationSite = {},
            onEditDraftSite = {},
            onDeleteDraft = {},
            onViewReport = {},
            onCompleteSession = {},
            onAuthorize = {},
            onChooseArea = {}
        )
    }
}