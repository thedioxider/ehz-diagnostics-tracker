package space.diomentia.ehzdiagnosticstracker.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.location.Location
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.data.LocalCoroutineScope
import space.diomentia.ehzdiagnosticstracker.data.LocalLocationProvider
import space.diomentia.ehzdiagnosticstracker.data.LocalSessionManager
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledIconButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.ScrollableColumn
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import space.diomentia.ehzdiagnosticstracker.data.ReportDatabase
import space.diomentia.ehzdiagnosticstracker.data.SessionManager

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

    val fullReport by sessionManager.fullReport.collectAsStateWithLifecycle(null)
    val existingSite = remember(fullReport, siteId) {
        siteId?.let { id -> fullReport?.sites?.find { it.site.id == id }?.site }
    }

    var latitude by remember { mutableStateOf(existingSite?.latitude) }
    var longitude by remember { mutableStateOf(existingSite?.longitude) }
    var positionKmText by remember {
        mutableStateOf(existingSite?.positionKm?.toString() ?: "")
    }

    val isZeroKm = remember(fullReport, siteId) {
        val sites = fullReport?.sites.orEmpty()
        sites.isEmpty() || sites.firstOrNull()?.site?.id == siteId
    }

    var isSyncingLocation by remember { mutableStateOf(existingSite == null) }
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
            .setMaxUpdateAgeMillis(15000L)
            .build()
        locationProvider?.getCurrentLocation(currentLocationRequest, null)
            ?.addOnSuccessListener { location ->
                latitude = location.latitude
                longitude = location.longitude
                if (positionKmText.isBlank()) {
                    val sites = fullReport?.sites.orEmpty()
                        .filter { it.site.id != siteId }
                    val previousSite = sites.lastOrNull()?.site
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
                        positionKmText = "%.3f".format(previousSite.positionKm + displacementKm)
                    } else {
                        positionKmText = "0.0"
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
                Column(modifier = Modifier.weight(1f)) {
                    if (latitude != null && longitude != null) {
                        Text(
                            "${stringResource(R.string.latitude)}: ${"%.6f".format(latitude)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "${stringResource(R.string.longitude)}: ${"%.6f".format(longitude)}",
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
            TextField(
                value = positionKmText,
                onValueChange = { input ->
                    positionKmText = input.filter {
                        it.isDigit() || it == '.' || it == ','
                    }
                },
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
            val positionKm = positionKmText.replace(',', '.').toDoubleOrNull()
            val canSave = latitude != null && longitude != null
                    && positionKm != null && positionKm >= 0.0
            PtmFilledButton(
                enabled = canSave,
                onClick = {
                    if (siteId != null && existingSite != null) {
                        // Editing existing site
                        positionKm?.let {
                            sessionManager.updateSitePosition(siteId, it)
                        }
                        latitude?.let { lat ->
                            longitude?.let { lon ->
                                sessionManager.updateSiteCoordinates(siteId, lat, lon)
                            }
                        }
                    } else {
                        // Creating new site
                        sessionManager.addSite(
                            latitude = latitude,
                            longitude = longitude,
                            positionKm = positionKm,
                            locationOnly = true
                        )
                    }
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