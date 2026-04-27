package space.diomentia.ehzdiagnosticstracker.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.Priority
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.data.Coordinates
import space.diomentia.ehzdiagnosticstracker.data.EnumValue
import space.diomentia.ehzdiagnosticstracker.data.LocalLocationProvider
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme

@Composable
fun TextFieldWithLabel(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    placeholderText: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Next
    ),
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null
) {
    val textStyle = LocalTextStyle.current
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier
    ) {
        label?.let {
            Box(
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                ProvideTextStyle(MaterialTheme.typography.titleSmall) {
                    label()
                }
            }
        }
        OutlinedTextField(
            value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    placeholderText,
                    style = textStyle,
                    fontStyle = FontStyle.Italic,
                    maxLines = 1,
                    modifier = Modifier.alpha(.5f)
                )
            },
            keyboardOptions = keyboardOptions,
            isError = isError,
            supportingText = supportingText,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (label != null) Modifier.padding(start = 12.dp) else Modifier)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnumDropdownField(
    selectedCode: String,
    values: List<EnumValue>,
    onSelect: (code: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = values.find { it.code == selectedCode }
    val displayText = selected?.label ?: ""
    val placeholder = stringResource(R.string.not_selected)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            placeholder = {
                Text(
                    placeholder,
                    fontStyle = FontStyle.Italic,
                    maxLines = 1,
                    modifier = Modifier.alpha(.5f)
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        placeholder,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.alpha(.5f)
                    )
                },
                onClick = {
                    onSelect("")
                    expanded = false
                }
            )
            values.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(entry.label) },
                    onClick = {
                        onSelect(entry.code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun LocationField(
    location: Coordinates?,
    modifier: Modifier = Modifier,
    modifiable: Boolean = true,
    updateAgeMillis: Long = 10000L,
    onLocation: (Location) -> Unit
) {
    val context = LocalContext.current
    val snacker = rememberSnacker()
    val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    val locationProvider = LocalLocationProvider.current

    locationProvider ?: {
        snacker.showMessage("no location provider")
    }

    var isSyncingLocation by remember { mutableStateOf(modifiable && location == null) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        isSyncingLocation = true
    }
    LaunchedEffect(isSyncingLocation) syncLocation@{
        if (!isSyncingLocation) return@syncLocation
        if (requiredPermissions.all { permission ->
                ActivityCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            }
        ) {
            locationPermissionLauncher.launch(requiredPermissions)
            isSyncingLocation = false
            return@syncLocation
        }
        val currentLocationRequest = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setGranularity(Granularity.GRANULARITY_FINE)
            .setMaxUpdateAgeMillis(updateAgeMillis)
            .build()
        locationProvider?.getCurrentLocation(currentLocationRequest, null)
            ?.addOnSuccessListener(onLocation)
            ?.addOnFailureListener {
                snacker.showMessage(
                    "${context.getString(R.string.location)}: ${
                        context.getString(R.string.something_wrong)
                    }..."
                )
            }
            ?.addOnCompleteListener { isSyncingLocation = false }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(IntrinsicSize.Max)
            .then(modifier)
    ) {
        PtmFilledButton(
            enabled = location != null,
            onClick = {
                location?.let {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            "geo:0,0?q=${it.lat},${it.lng}".toUri()
                        )
                    )
                }
            },
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxHeight()
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = stringResource(R.string.location),
                    modifier = Modifier
                        .padding(4.dp)
                        .aspectRatio(1f)
                )
                Text(stringResource(R.string.show_area_on_map_short))
            }
        }
        if (modifiable) {
            PtmFilledIconButton(
                onClick = { isSyncingLocation = true },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .aspectRatio(1f)
                    .width(48.dp)
            ) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun LocationIconButton(
    location: Coordinates?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    PtmFilledIconButton(
        enabled = location != null,
        onClick = {
            location?.let {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "geo:0,0?q=${it.lat},${it.lng}".toUri()
                    )
                )
            }
        },
        modifier = Modifier.then(modifier)
    ) {
        Icon(
            Icons.Outlined.LocationOn,
            contentDescription = stringResource(R.string.location),
            modifier = Modifier
                .aspectRatio(1f)
                .padding(4.dp)
        )
    }
}


@Preview
@Composable
private fun TextFieldWithLabelPreview() = PtmTheme {
    TextFieldWithLabel(
        label = { Text("Label") },
        value = "",
        onValueChange = {},
        placeholderText = "Введите значение"
    )
}

@Preview
@Composable
private fun LocationFieldPreview() = PtmTheme {
    LocationField(Coordinates(0.0, 0.0)) {}
}

@Preview
@Composable
private fun LocationIconButtonEnabledPreview() = PtmTheme {
    LocationIconButton(Coordinates(0.0, 0.0))
}

@Preview
@Composable
private fun LocationIconButtonDisablePreview() = PtmTheme {
    LocationIconButton(null)
}


/*
@Composable
private fun LocationField(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = LocalCoroutineScope.current
    val snackbarHostState = LocalSnackbarHostState.current
    val locationProvider = LocalLocationProvider.current
    var reportSession by LocalReportSession.current

    var isSyncingLocation by remember { mutableStateOf(true) }
    val locationPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        isSyncingLocation = true
    }
    LaunchedEffect(isSyncingLocation) syncLocation@{
        if (!isSyncingLocation) return@syncLocation
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
            isSyncingLocation = false
            return@syncLocation
        }
        val currentLocationRequest = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setGranularity(Granularity.GRANULARITY_FINE)
            .setMaxUpdateAgeMillis(15000L)
            .build()
        locationProvider?.getCurrentLocation(currentLocationRequest, null)
            ?.addOnSuccessListener {
                reportSession =
                    reportSession.copy(location = Coordinates(it.latitude, it.longitude))
            }
            ?.addOnFailureListener {
                coroutineScope?.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        "${context.getString(R.string.location)}: ${
                            context.getString(
                                R.string.something_wrong
                            )
                        }..."
                    )
                }
            }
            ?.addOnCompleteListener { isSyncingLocation = false }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        val location = reportSession.location
        PtmFilledButton(
            enabled = location != null,
            onClick = {
                location?.let {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            "geo:0,0?q=${it.latitude},${it.longitude}".toUri()
                        )
                    )
                }
            },
            modifier = Modifier.weight(1f, fill = true)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = stringResource(R.string.location),
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(stringResource(R.string.show_area_on_map_short))
            }
        }
        PtmFilledIconButton(
            onClick = { isSyncingLocation = true },
        ) {
            Icon(
                Icons.Outlined.Refresh,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(.8f)
                    .aspectRatio(1f)
            )
        }
    }
}
 */
