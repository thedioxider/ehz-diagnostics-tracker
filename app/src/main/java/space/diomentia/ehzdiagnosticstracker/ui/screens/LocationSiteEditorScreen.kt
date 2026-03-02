package space.diomentia.ehzdiagnosticstracker.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
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
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.data.LocalCoroutineScope
import space.diomentia.ehzdiagnosticstracker.data.LocalLocationProvider
import space.diomentia.ehzdiagnosticstracker.data.LocalSessionManager
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.ReportDatabase
import space.diomentia.ehzdiagnosticstracker.data.SessionManager
import space.diomentia.ehzdiagnosticstracker.data.Site
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledIconButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.ScrollableColumn
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme

@Composable
fun LocationSiteEditorScreen(
    siteId: String?,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = LocalCoroutineScope.current
    val snackbarHostState = LocalSnackbarHostState.current
    val locationProvider = LocalLocationProvider.current
    val sessionManager = LocalSessionManager.current
    val reportDao = ReportDatabase.getInstance().reportDao()

    val existingSite =
        remember(siteId) { siteId?.let { siteId -> sessionManager.getSite(siteId) } }
    val editState = rememberLocationSiteEditState(existingSite)
    val reportId =
        remember(existingSite) { existingSite?.reportId ?: sessionManager.activeReportId }
    val reportSites = remember(reportId) {
        reportId?.let { reportDao.getSitesForReportOnce(it) }.orEmpty()
            .filter { it.id != siteId }
    }
    val isZeroKm = remember(reportSites) {
        reportSites.isEmpty() ||
                (existingSite?.timestamp?.let { it < reportSites.first().timestamp } ?: false)
    }

    var isSyncingLocation by remember { mutableStateOf(siteId == null) }
    val locationPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        isSyncingLocation = true
    }
    LaunchedEffect(isSyncingLocation) {
        if (!isSyncingLocation) return@LaunchedEffect
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
            isSyncingLocation = false
            return@LaunchedEffect
        }
        val currentLocationRequest = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setGranularity(Granularity.GRANULARITY_FINE)
            .setMaxUpdateAgeMillis(10000L)
            .build()
        locationProvider?.getCurrentLocation(currentLocationRequest, null)
            ?.addOnSuccessListener { location ->
                editState.latitude = location.latitude
                editState.longitude = location.longitude
                if (editState.positionKmText.isBlank()) {
                    val previousSite = reportSites.lastOrNull()
                    if (previousSite?.latitude != null && previousSite.longitude != null
                        && previousSite.positionKm != null
                    ) {
                        val results = FloatArray(1)
                        Location.distanceBetween(
                            previousSite.latitude, previousSite.longitude,
                            location.latitude, location.longitude,
                            results
                        )
                        val displacementKm = results[0] / 1000.0
                        editState.positionKmText =
                            "%.2f".format(previousSite.positionKm + displacementKm)
                    } else {
                        editState.positionKmText = "0.0"
                    }
                }
            }
            ?.addOnFailureListener {
                coroutineScope?.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        "${context.getString(R.string.location)}: ${
                            context.getString(R.string.something_wrong)
                        }..."
                    )
                }
            }
            ?.addOnCompleteListener { isSyncingLocation = false }
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

            // GPS Coordinates
            Text(
                stringResource(R.string.coordinates),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
            ) {
                // TODO: show on map button instead of text
                // TODO: should not be able to edit coordinates in saved location site
                Column(modifier = Modifier.weight(1f)) {
                    if (editState.latitude != null && editState.longitude != null) {
                        Text(
                            "${stringResource(R.string.latitude)}: ${"%.6f".format(editState.latitude)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "${stringResource(R.string.longitude)}: ${"%.6f".format(editState.longitude)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            stringResource(R.string.location),
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.alpha(.5f)
                        )
                    }
                }
                PtmFilledIconButton(
                    onClick = { isSyncingLocation = true }
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.refresh_location),
                        modifier = Modifier
                            .fillMaxSize(.8f)
                            .aspectRatio(1f)
                    )
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
                    editState.save()
                    onSaved()
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

    var latitude by mutableStateOf(existingSite?.latitude)
    var longitude by mutableStateOf(existingSite?.longitude)

    private var _positionKmText = mutableStateOf(existingSite?.positionKm?.toString() ?: "")
    var positionKmText: String
        get() = _positionKmText.value
        set(value) {
            _positionKmText.value = value.filter { it.isDigit() || it == '.' || it == ',' }
        }

    val positionKm: Double?
        get() = positionKmText.replace(',', '.').toDoubleOrNull()

    val canSave: Boolean
        get() = latitude != null && longitude != null && positionKm != null && positionKm!! >= 0.0

    init {
        if (existingSiteId != null) {
            scope.launch(Dispatchers.IO) {
                snapshotFlow { Triple(positionKm, latitude, longitude) }
                    .debounce(500)
                    .collectLatest { (km, lat, lon) ->
                        km?.let { sessionManager.updateSitePosition(existingSiteId, it) }
                        if (lat != null && lon != null) sessionManager.updateSiteCoordinates(
                            existingSiteId,
                            lat,
                            lon
                        )
                    }
            }
        }
    }

    fun save(): String {
        if (existingSiteId != null) {
            positionKm?.let { sessionManager.updateSitePosition(existingSiteId, it) }
            latitude?.let { lat ->
                longitude?.let { lon ->
                    sessionManager.updateSiteCoordinates(existingSiteId, lat, lon)
                }
            }
            sessionManager.confirmSite(existingSiteId)
            return existingSiteId
        } else {
            val site = sessionManager.addSite(
                latitude = latitude, longitude = longitude,
                positionKm = positionKm, locationOnly = true
            )
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
