package space.diomentia.ehzdiagnosticstracker.ui.screens

import android.icu.text.DecimalFormat
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.data.Coordinates
import space.diomentia.ehzdiagnosticstracker.data.LocalCoroutineScope
import space.diomentia.ehzdiagnosticstracker.data.LocalLocationProvider
import space.diomentia.ehzdiagnosticstracker.data.LocalSessionManager
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.NAME_ALLOWED_SPECIAL_CHARS
import space.diomentia.ehzdiagnosticstracker.data.ReportDatabase
import space.diomentia.ehzdiagnosticstracker.data.SessionManager
import space.diomentia.ehzdiagnosticstracker.data.Site
import space.diomentia.ehzdiagnosticstracker.data.distanceKmTo
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.LocationField
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.ScrollableColumn
import space.diomentia.ehzdiagnosticstracker.ui.TextFieldWithLabel
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme

@Composable
fun LocationSiteEditorScreen(
    siteId: String?,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    LocalContext.current
    LocalCoroutineScope.current
    val snackbarHostState = LocalSnackbarHostState.current
    LocalLocationProvider.current
    val sessionManager = LocalSessionManager.current
    val reportDao = ReportDatabase.getInstance().reportDao()

    val existingSite =
        remember(siteId) { siteId?.let { siteId -> sessionManager.getSite(siteId) } }
    val editState = rememberLocationSiteEditState(existingSite)
    val reportId =
        remember(existingSite) { existingSite?.reportId ?: sessionManager.activeReportId }
    val reportSites = remember(reportId) {
        reportId?.let { reportDao.getSitesForReportOnce(it) }.orEmpty()
            .filter { it.id != siteId && !it.isDraft }
    }
    val isZeroKm = remember(reportSites) {
        reportSites.isEmpty() ||
                (existingSite?.timestamp?.let { it < reportSites.first().timestamp } ?: false)
    }

    Scaffold(
        topBar = {
            PtmTopBar(title = {
                Text(
                    stringResource(
                        if (isZeroKm) R.string.zero_km else R.string.location_site
                    )
                )
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
        },
        snackbarHost = { PtmSnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        ScrollableColumn(
            innerPadding,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(24.dp))

            // Name (optional, hidden for zero-km point)
            if (!isZeroKm) {
                TextFieldWithLabel(
                    value = editState.name,
                    onValueChange = {
                        editState.name =
                            it.filter { c -> c.isLetterOrDigit() || c in NAME_ALLOWED_SPECIAL_CHARS }
                    },
                    label = {
                        Text("${stringResource(R.string.site_name)} (${stringResource(R.string.optional)})")
                    }
                )
                Spacer(Modifier.height(24.dp))
            }

            // GPS Coordinates
            Text(
                stringResource(R.string.coordinates),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LocationField(
                location = editState.location,
                modifiable = remember { existingSite == null }
            ) {
                val loc = Coordinates(lat = it.latitude, lng = it.longitude)
                editState.location = loc
                if (editState.positionKmText.isBlank()) {
                    val previousSite = reportSites.lastOrNull()
                    editState.positionKmText = (previousSite?.run {
                        if (location != null && positionKm != null) {
                            positionKm + location.distanceKmTo(loc)
                        } else null
                    } ?: if (isZeroKm) 0.0 else null)
                        ?.let { DecimalFormat("#.##").format(it) } ?: ""
                }
            }

            Spacer(Modifier.height(24.dp))

            // Position on route in km
            Text(
                stringResource(R.string.position_km),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = editState.positionKmText,
                onValueChange = { editState.positionKmText = it },
                placeholder = {
                    Text(
                        stringResource(R.string.input_value),
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.alpha(.5f)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(32.dp))

            // Save button
            PtmFilledButton(
                enabled = editState.canSave,
                onClick = {
                    if (editState.save() != null) {
                        onSaved()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Icon(
                    painterResource(R.drawable.ic_save),
                    contentDescription = stringResource(R.string.save),
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(stringResource(R.string.save))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// --- State Holder ---

@OptIn(FlowPreview::class)
private class LocationSiteEditState(
    existingSite: Site?,
    private val sessionManager: SessionManager,
    scope: CoroutineScope
) {
    private val existingSiteId = existingSite?.id

    var name by mutableStateOf(existingSite?.name ?: "")
    var location by mutableStateOf(existingSite?.location)

    private var _positionKmText = mutableStateOf(existingSite?.positionKm?.toString() ?: "")
    var positionKmText: String
        get() = _positionKmText.value
        set(value) {
            _positionKmText.value = value.filter { it.isDigit() || it == '.' || it == ',' }
        }

    val positionKm: Double?
        get() = positionKmText.replace(',', '.').toDoubleOrNull()

    val canSave: Boolean
        get() = location != null && positionKm != null && positionKm!! >= 0.0

    init {
        if (existingSiteId != null) {
            scope.launch(Dispatchers.IO) {
                snapshotFlow { positionKm to location }
                    .debounce(500)
                    .collectLatest { (km, loc) ->
                        km?.let { sessionManager.updateSitePosition(existingSiteId, it) }
                        if (loc != null) sessionManager.updateSiteLocation(existingSiteId, loc)
                    }
            }
            scope.launch(Dispatchers.IO) {
                snapshotFlow { name }
                    .debounce(500)
                    .collectLatest { sessionManager.updateSiteName(existingSiteId, it) }
            }
        }
    }

    fun save(): String? {
        val km = positionKm ?: return null
        if (existingSiteId != null) {
            sessionManager.updateSitePosition(existingSiteId, km)
            location?.let { loc ->
                sessionManager.updateSiteLocation(existingSiteId, loc)
            }
            sessionManager.updateSiteName(existingSiteId, name)
            sessionManager.confirmSite(existingSiteId)
            return existingSiteId
        } else {
            val site = sessionManager.addSite(
                positionKm = km,
                location = location,
                locationOnly = true
            )
            sessionManager.updateSiteName(site.id, name)
            sessionManager.confirmSite(site.id)
            return site.id
        }
    }
}

@Composable
private fun rememberLocationSiteEditState(site: Site?): LocationSiteEditState {
    val sessionManager = LocalSessionManager.current
    val scope = rememberCoroutineScope()
    return remember(site) {
        LocationSiteEditState(site, sessionManager, scope)
    }
}

// --- Previews ---

@Preview(showBackground = true)
@Composable
private fun LocationSiteEditorScreenPreview() {
    val context = LocalContext.current
    val sessionManager = SessionManager(
        ReportDatabase.getInstance(context).reportDao(),
        null
    )
    CompositionLocalProvider(LocalSessionManager provides sessionManager) {
        PtmTheme {
            LocationSiteEditorScreen(siteId = null, onSaved = {}, onBack = {})
        }
    }
}
