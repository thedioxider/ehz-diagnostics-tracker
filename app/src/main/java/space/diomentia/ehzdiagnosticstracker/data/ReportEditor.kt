package space.diomentia.ehzdiagnosticstracker.data

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.launch
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledIconButton
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme


@Preview(showBackground = true)
@Composable
fun ReportEditor(
    modifier: Modifier = Modifier
) {
    val title = @Composable { title: String ->
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(vertical = 4.dp)
        )
    }

    PtmTheme {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
        ) {
            title(stringResource(R.string.location))
            LocationField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .padding(start = 32.dp, end = 16.dp)
            )
        }
    }
}

@Composable
private fun LocationField(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current
    val locationProvider = LocalLocationProvider.current

    val locationPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {}
    val syncLocation = syncLocation@{
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
            return@syncLocation
        }
        locationProvider?.lastLocation
            ?.addOnSuccessListener {
                AppState.CurrentDiagnosticSession.location = it
            }
            ?.addOnFailureListener {
                coroutineScope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        "${context.getString(R.string.location)}: ${
                            context.getString(
                                R.string.something_wrong
                            )
                        }"
                    )
                }
            }
    }
    LaunchedEffect(Unit) {
        syncLocation()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(
            AppState.CurrentDiagnosticSession.location?.let {
                "${it.latitude} ${it.altitude}"
            } ?: stringResource(R.string.unknown),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f, fill = true)
        )
        PtmFilledIconButton(
            onClick = syncLocation,
        ) {
            Icon(
                Icons.Outlined.Refresh,
                contentDescription = null,
                modifier = Modifier
                    .height(32.dp)
                    .aspectRatio(1f)
            )
        }
    }
}